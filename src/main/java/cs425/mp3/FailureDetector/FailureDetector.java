package cs425.mp3.FailureDetector;

import cs425.mp3.Pid;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    public static final long PROTOCOL_TIME=2000;
    private final int MAX_NODES=10;
    private final int CONCURRENCY_LEVEL=2;
    private final float LOAD_FACTOR= (float) 0.75;

    protected AtomicInteger time=new AtomicInteger(0);
    private AtomicBoolean ackReceived=new AtomicBoolean(false);

	protected Pid introducer_id;
    private final AtomicBoolean introducer_failed;
    protected ConcurrentHashMap<Info,Integer> infoBuffer;
    protected ConcurrentHashMap<String,Integer> recentlyLeft;
    protected Set<String> membershipSet;
    protected Pid self_id;
    private PingSender sender;
    private Transponder transponder;
    
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
    public void startFD() {
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
        this.runFD();
    }

	/** Runs FD module
	 * @return true if need to rejoin
	 */
	protected void runFD() {
        DatagramSocket socket=null;
		try {
			socket = new DatagramSocket(self_id.port);
		} catch (SocketException e) {
			e.printStackTrace();
		}

        System.out.println("[MAIN] [INFO] [" + System.currentTimeMillis() + "] : udp socket initiated");
		Transponder receiverThread = new Transponder(socket,self_id.pidStr,introducer_id.pidStr,introducer_failed,
                membershipSet,ackReceived,infoBuffer,recentlyLeft,time);
		receiverThread.setDaemon(true);
		receiverThread.start();
		transponder = receiverThread;
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
		sender=senderThread;
        System.out.println("[MAIN] [INFO] [" + System.currentTimeMillis() + "] : sender thread added");
        try {
            senderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            receiverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        socket.close();
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
    
    public Pid getSelfID(){
    	return this.self_id;
    }
    public Pid getIntroID(){
    	return this.introducer_id;
    }
    public Boolean isAlive(String id){
    	if(id.equals(self_id.pidStr)){
    		return true;
    	}
    	else if(id.equals(introducer_id.toString())){
    		return (!introducer_failed.get());
    	}
    	else {
    		return membershipSet.contains(id);
    	}
    }
    public List<String> getMemlistSkipIntroducer(){
    	List<String> memlist = new ArrayList<String>(membershipSet);
    	memlist.remove(introducer_id.pidStr);
    	return memlist;
    }
    public List<String> getMemlistSkipIntroducerWithSelf(){
    	List<String> memlist = new ArrayList<String>(membershipSet);
    	memlist.remove(introducer_id.pidStr);
    	memlist.add(self_id.toString());
    	return memlist;
    }
    public void leaveInitiate(){
    	sender.terminate();
    	transponder.terminate();
    }
}

