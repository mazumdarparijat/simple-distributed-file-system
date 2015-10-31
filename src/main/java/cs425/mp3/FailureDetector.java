package cs425.mp3;

import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class for Failure Detector Module
 *
 */
public class FailureDetector {
    private final long PING_TIME_OUT=500;
    public static final long PROTOCOL_TIME=1000;
    private final int MAX_NODES=10;
    private final int CONCURRENCY_LEVEL=2;
    private final float LOAD_FACTOR= (float) 0.75;

    protected AtomicInteger time=new AtomicInteger(0);
    private AtomicBoolean ackReceived=new AtomicBoolean(false);
    private AtomicBoolean rejoinSignal =new AtomicBoolean(false);

	protected Pid introducer_id;
    private final AtomicBoolean introducer_failed;
    protected ConcurrentHashMap<Info,Integer> infoBuffer;
    protected ConcurrentHashMap<String,Integer> recentlyLeft;
    protected Set<String> membershipSet;
    protected Pid self_id;

    /**
     * Initializes membership list and other data structures
     */
    protected FailureDetector() {
        membershipSet= Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>
                (MAX_NODES,LOAD_FACTOR,CONCURRENCY_LEVEL));
        infoBuffer=new ConcurrentHashMap<Info, Integer>(MAX_NODES,LOAD_FACTOR,CONCURRENCY_LEVEL);
        recentlyLeft=new ConcurrentHashMap<String, Integer>(MAX_NODES,LOAD_FACTOR,CONCURRENCY_LEVEL);
        introducer_id=new Pid("",0,0);
        introducer_failed=new AtomicBoolean(false);
    }

	/**Constructor called when process is not introducer
	 * @param port Port to run FD module
	 * @param intro_address Address of introducer
	 * @param intro_port Port of Introducer
	 */
	public FailureDetector(int port, String intro_address, int intro_port){
        this();
        try {
            self_id=new Pid(InetAddress.getLocalHost().getHostName(),port,System.currentTimeMillis());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        System.out.println("[MAIN] [INFO] [" + System.currentTimeMillis() + "] : node created with id : " + self_id.pidStr);
        introducer_id=new Pid(intro_address,intro_port,0);
        membershipSet.add(introducer_id.pidStr);
        System.out.println("[MAIN] [MEM_ADD] ["+System.currentTimeMillis()+"] : "+introducer_id.pidStr);
	}

    /** Starts FD Module
     * @return true if needs to rejoin
     */
    public boolean startFD() {
        // get membership list from introducer over TCP
        Socket tcpConnection=null;

        while (true) {
            try {
                tcpConnection = new Socket(this.introducer_id.hostname, this.introducer_id.port);
            } catch (IOException e) {
                System.out.println("[MAIN] [INFO] Cannot connect to introducer. Will try again in a while.");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

                continue;
            }

            System.out.println("[MAIN] [INFO] TCP connection established.");
            break;
        }

        Scanner inputReader = null;
        try {
            inputReader = new Scanner(new InputStreamReader(tcpConnection.getInputStream()));
            inputReader.useDelimiter("\n");
        } catch (IOException e) {
            System.err.println("[MAIN] [ERROR] Error creating input stream to introducer");
        }
        PrintWriter outputWriter = null;
        try {
            outputWriter = new PrintWriter(new OutputStreamWriter(tcpConnection.getOutputStream()));
        } catch (IOException e) {
            System.err.println("[MAIN] [ERROR] Error creating input stream from socket");
        }

        System.out.println("[MAIN] [INFO] ["+System.currentTimeMillis()+"] : tcp connection initiated");
        outputWriter.println(this.self_id.pidStr);
        outputWriter.flush();

        while (inputReader.hasNext()) {
            String newMember=inputReader.next();
            System.out.println("[MAIN] [MEM_ADD] ["+System.currentTimeMillis()+"] : "+newMember);
            membershipSet.add(newMember);
        }
        try {
            tcpConnection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this.runFD();
    }

	/** Runs FD module
	 * @return true if need to rejoin
	 */
	protected boolean runFD() {
        DatagramSocket socket=null;
		try {
			socket = new DatagramSocket(self_id.port);
		} catch (SocketException e) {
			e.printStackTrace();
		}

        System.out.println("[MAIN] [INFO] [" + System.currentTimeMillis() + "] : udp socket initiated");
		Transponder receiverThread = new Transponder(socket,self_id.pidStr,introducer_id.pidStr,introducer_failed,
                membershipSet,ackReceived,rejoinSignal,infoBuffer,recentlyLeft,time);
		receiverThread.setDaemon(true);
		receiverThread.start();
        System.out.println("[MAIN] [INFO] [" + System.currentTimeMillis() + "] : receiver thread started");

        // wait for log N cycles before ping sending
        try {
            Thread.sleep((long) (PROTOCOL_TIME*FailureDetector.getSpreadTime(membershipSet.size())));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        PingSender senderThread = new PingSender(socket,membershipSet,ackReceived,infoBuffer,recentlyLeft,
                self_id.pidStr,introducer_id.pidStr,introducer_failed,time,PING_TIME_OUT,PROTOCOL_TIME);
		senderThread.setDaemon(true);
		senderThread.start();
        System.out.println("[MAIN] [INFO] [" + System.currentTimeMillis() + "] : sender thread added");
		System.out.println("Press any key followed by enter to leave");
		System.err.println("Ready to take arguments. Press m to get membership list, i to get id and l to leave");
        boolean rejoin;
        while (true) {
            try {
                while ((rejoin = System.in.available() <= 0) && !rejoinSignal.get()) {
                    Thread.sleep(100);
                }
                if (!rejoin) {
                    BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
                    String line=br.readLine();
                    if (processUserCommand(line))
                        break;
                } else {
                	System.err.println("The node is leaving to rejoin. Wait for it to come back with a new ID!");
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        senderThread.terminate();
        try {
            senderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        receiverThread.terminate();
        try {
            receiverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        socket.close();
        return rejoin;
    }

    /** Processes user options
     * @param line user input
     * @return true when leave requested
     */
    private boolean processUserCommand(String line) {
        if (line.equals("m")) {
            System.err.println("MEMBERSHIP LIST :");
            for (String pid : membershipSet) {
                if (pid.equals(introducer_id.pidStr)) {
                    if (!introducer_failed.get())
                        System.err.println(pid);
                } else {
                    System.err.println(pid);
                }
            }
            return false;
        } else if (line.equals("i")) {
            System.err.println("ID : "+ self_id.pidStr);
            return false;
        } else if (line.equals("l")) {
            System.err.println("leave requested");
            return true;
        } else {
            System.err.println("argument not recognised. Press m to get membership list, i to get id and l to leave");
            return false;
        }
    }

    /** Calculates dissemination time
     * @param numMembers
     * @return dissemination time
     */
    public static double getSpreadTime(int numMembers) {
        if (numMembers==0)
            return 0;

        double ret=3.0*Math.log(numMembers)/Math.log(2.0)+1.0;
        return ret;
    }
}

