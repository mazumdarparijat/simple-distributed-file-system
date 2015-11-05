package cs425.mp3.FailureDetector;

import cs425.mp3.Pid;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Class for introducer
 *
 */
public class FDIntroducer extends FailureDetector {
	/**Constructor
	 * @param port for running introducer
	 */
	public FDIntroducer(int port){
        super();
        try {
            this.self_id=new Pid(InetAddress.getLocalHost().getHostName(),port,0);
            this.introducer_id=this.self_id;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        System.out.println("self id created : " + self_id.pidStr);
	}

    /**
     * Start FD for introducer
     * @see FailureDetector#startFD()
     */
    @Override
    public void startFD() {
        System.out.println("node started");
        NewJoinThread joiner=new NewJoinThread(this.self_id.port);
        joiner.setDaemon(true);
        joiner.start();
        this.runFD();
        joiner.setTerminate();
        try {
            joiner.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Thread class for joining
     *
     */
    private class NewJoinThread extends Thread {
        private final int port;
        private final AtomicBoolean terminate;
        /** Constructor
         * @param port for TCP 
         */
        public NewJoinThread(int port) {
            terminate=new AtomicBoolean(false);
            this.port=port;
        }

        /**
         * Kill this thread
         */
        public void setTerminate() {
            terminate.set(true);
        }

        /** 
         * Run TCP thread for joining
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            System.out.println("joiner thread started");
            ServerSocket tcp=null;
            try {
                tcp=new ServerSocket(this.port);
                tcp.setSoTimeout(100);
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (!terminate.get()) {
                Socket joinRequest = null;
                try {
                    joinRequest=tcp.accept();
                } catch (SocketTimeoutException e) {
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Scanner inputReader = null;
                try {
                    inputReader = new Scanner(new InputStreamReader(joinRequest.getInputStream()));
                    inputReader.useDelimiter("\n");
                } catch (IOException e) {
                    System.err.println("[ERROR] Error creating input stream to introducer");
                    return;
                }
                PrintWriter outputWriter = null;
                try {
                    outputWriter = new PrintWriter(new OutputStreamWriter(joinRequest.getOutputStream()));
                } catch (IOException e) {
                    System.err.println("[ERROR] Error creating input stream from socket");
                    return;
                }

                String joinerID=inputReader.next();
                System.out.println("[JOINER THREAD] join requested by : " + joinerID);
                for (String member : membershipSet) {
                    outputWriter.println(member);
                }

                outputWriter.flush();

                try {
                    joinRequest.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                membershipSet.add(joinerID);
                System.out.println(joinerID + " added to membershipSet");
                infoBuffer.put(new Info(Info.InfoType.JOIN, joinerID), time.intValue()
                        + (int) getSpreadTime(membershipSet.size()));
                System.out.println(joinerID + " join added to infoBuffer");
            }

            try {
                tcp.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
