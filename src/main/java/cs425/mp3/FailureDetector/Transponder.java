package cs425.mp3.FailureDetector;

import cs425.mp3.FailureDetector.FailureDetector;
import cs425.mp3.FailureDetector.Info;
import cs425.mp3.FailureDetector.Message;
import cs425.mp3.FailureDetector.MessageType;
import cs425.mp3.Pid;

import java.io.IOException;
import java.net.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Receiver Class
 */
public class Transponder extends Thread{
    private static final int MAX_BYTE_LENGTH=1024;
	private final DatagramSocket socket;
    private final String idString;
    private final String introID;
    private final Set<String> membershipSet;
    private final ConcurrentHashMap<Info,Integer> infoMap;
    private final ConcurrentHashMap<String,Integer> recentlyLeft;
    private final AtomicBoolean introFailed;
    private AtomicInteger time;
    private AtomicBoolean ackReceived;
    private AtomicBoolean rejoinSignal;
    private volatile boolean leave=false;

    /**Constructor
     * @param socket
     * @param idStr
     * @param introID
     * @param introducer_failed
     * @param membershipSet
     * @param ackReceived
     * @param rejoinSignal
     * @param infoMap
     * @param recentlyLeft
     * @param time
     */
    public Transponder(DatagramSocket socket, String idStr, String introID, AtomicBoolean introducer_failed, Set<String> membershipSet,
                       AtomicBoolean ackReceived, AtomicBoolean rejoinSignal, ConcurrentHashMap<Info, Integer> infoMap,
                       ConcurrentHashMap<String, Integer> recentlyLeft, AtomicInteger time) {
        this.socket=socket;
        this.idString=idStr;
        this.introID=introID;
        this.membershipSet=membershipSet;
        this.infoMap=infoMap;
        this.recentlyLeft=recentlyLeft;
        this.time=time;
        this.ackReceived=ackReceived;
        this.rejoinSignal=rejoinSignal;
        this.introFailed=introducer_failed;
    }

    /**
     * Kill reciever
     */
    public void terminate() {
        leave=true;
    }

    /** Send paket
     * @param sendBytes
     * @param address
     * @param port
     */
    private void sendDatagramPacket(byte [] sendBytes, InetAddress address, int port) {
        DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length,
                address,port);

        try {
            socket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**Process recieved packets
     * @param m
     */
    private void processInfoPackets(Message m) {
        for (Info i : m.getInfoList()){
            if (!i.param.equals(idString)) {
                if ((i.type == Info.InfoType.JOIN) && (!this.recentlyLeft.containsKey(i.param))) {
                    if (!this.membershipSet.contains(i.param)) {
                        this.infoMap.putIfAbsent(i, this.time.intValue() + (int) FailureDetector
                                .getSpreadTime(this.membershipSet.size()));
                        this.membershipSet.add(i.param);
                        System.out.println("[RECEIVER] [MEM_ADD] ["+System.currentTimeMillis()+"] : "+i.param);

                    }
                }
                else if (i.type == Info.InfoType.FAILED || i.type == Info.InfoType.LEAVE) {
                    if (this.membershipSet.contains(i.param)) {
                        this.infoMap.putIfAbsent(i, this.time.intValue() + (int) FailureDetector
                                .getSpreadTime(this.membershipSet.size()));

                        if (i.param.equals(introID)) {
                            System.out.println("[RECEIVER] [INFO] [" + System.currentTimeMillis() + "] " +
                                    ": introducer failure received : " + i.param);
                            introFailed.set(true);

                        } else {
                            this.membershipSet.remove(i.param);
                            System.out.println("[RECEIVER] [MEM_REMOVE] [" + System.currentTimeMillis() + "] " +
                                    ": failure received : " + i.param);
                        }
                    }

                    this.recentlyLeft.putIfAbsent(i.param,this.time.intValue()+3*membershipSet.size());
                }

            }
        }
    }

	/** Handle different messages recieved
	 * @param receivePacket
	 */
	public void handleMsg(DatagramPacket receivePacket){
        // if ping - send ack if in membership list and write dissemination to dissemination buffer
        // if ack
        System.out.println("[RECEIVER] [INFO] ["+System.currentTimeMillis()+"] Message Received : "
                +new String(receivePacket.getData(),0,receivePacket.getLength()));

        Message m = Message.extractMessage(receivePacket.getData(),receivePacket.getLength());
        if (m.type== MessageType.PING) {
            this.processInfoPackets(m);
            byte[] sendBytes;
            if (membershipSet.contains(m.getMessageSenderID())) {
                sendBytes = Message.MessageBuilder
                        .buildAckMessage(m.getMessageKey())
                        .addInfoFromList(this.infoMap.keySet())
                        .getMessage()
                        .toByteArray();
            } else {
                // hack for introducer rejoin - accept all unknown pings
                // add to memlist
                if (idString.equals(introID)) {
                    membershipSet.add(m.getMessageSenderID());
                    sendBytes = Message.MessageBuilder
                            .buildAckMessage(m.getMessageKey())
                            .addInfoFromList(this.infoMap.keySet())
                            .getMessage()
                            .toByteArray();
                } else {
                    sendBytes = Message.MessageBuilder
                            .buildMissingNoticeMessage()
                            .getMessage()
                            .toByteArray();
                }
            }

            System.out.println("[RECEIVER] [INFO] ["+System.currentTimeMillis()+"] message sent : "
                    +new String(sendBytes,0,sendBytes.length));
            sendDatagramPacket(sendBytes, receivePacket.getAddress(), receivePacket.getPort());

        } else if (m.type==MessageType.ACK) {
            this.processInfoPackets(m);
            if (Integer.parseInt(m.getMessageKey())==time.intValue()) {
                ackReceived.set(true);
                synchronized (ackReceived) {
                    ackReceived.notify();
                }
            }

        } else if (m.type == MessageType.PING_REQUEST) {
            if (m.getReqDestination().equals(idString)) {
                byte [] sendBytes = Message.MessageBuilder
                        .buildAckReqMessage(m.getMessageKey(),m.getMessageSenderID())
                        .getMessage()
                        .toByteArray();
                System.out.println("[RECEIVER] [INFO] ["+System.currentTimeMillis()+"] message sent : "
                        +new String(sendBytes,0,sendBytes.length));
                sendDatagramPacket(sendBytes,receivePacket.getAddress(),receivePacket.getPort());
            } else {
                byte [] sendBytes = m.toByteArray();
                Pid destPid=Pid.getPid(m.getReqDestination());
                try {
                    System.out.println("[RECEIVER] [INFO] ["+System.currentTimeMillis()+"] message sent : "
                            +new String(sendBytes,0,sendBytes.length));
                    sendDatagramPacket(sendBytes, InetAddress.getByName(destPid.hostname), destPid.port);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        } else if (m.type == MessageType.ACK_REQUEST) {
            if (m.getMessageSenderID().equals(idString)) {
                if (Integer.parseInt(m.getMessageKey())==time.intValue()) {
                    ackReceived.set(true);
                    synchronized (ackReceived) {
                        ackReceived.notify();
                    }
                }
            } else {
                Pid destPid=Pid.getPid(m.getMessageSenderID());
                try {
                    System.out.println("[RECEIVER] [INFO] ["+System.currentTimeMillis()+"] message sent : "
                            +new String(m.toByteArray(),0,m.toByteArray().length));
                    sendDatagramPacket(m.toByteArray(), InetAddress.getByName(destPid.hostname), destPid.port);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        } else if (m.type==MessageType.MISSING_NOTICE) {
            this.rejoinSignal.set(true);
        } else {
                throw new IllegalArgumentException("Message type not recognized");
        }
	}
	/** Run receiver
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run(){
        try {
            socket.setSoTimeout((int) (FailureDetector.PROTOCOL_TIME*2));
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while(!leave){
            byte [] receiveData = new byte[MAX_BYTE_LENGTH];
			DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
            System.out.println("[RECEIVER] [INFO] ["+System.currentTimeMillis()+"] Waiting to receive next packet");
            try {
                socket.receive(receivedPacket);
            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
                e.printStackTrace();
            }

            handleMsg(receivedPacket);
		}
	}
}
