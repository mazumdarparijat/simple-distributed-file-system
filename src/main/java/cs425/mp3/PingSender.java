package cs425.mp3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/** Thread class for sender
 * 
 */
public class PingSender extends Thread{
    private static final int SUBGROUP_K=2;
	private final DatagramSocket socket;
    private final long pingTimeOut;
    private final long protocolTime;
    private final Set<String> memberSet;
    private final ConcurrentHashMap<Info,Integer> infoMap;
    private final ConcurrentHashMap<String,Integer> recentlyLeft;
    private final String idString;
    private final String introID;
    private final AtomicBoolean introFailed;
    private AtomicInteger time;
    private AtomicBoolean ackReceived;
    private volatile boolean leave=false;

    /** Constructor
     * @param socket
     * @param memberSet
     * @param ackReceived
     * @param infoMap
     * @param recentlyLeft
     * @param idStr
     * @param introID
     * @param introducer_failed
     * @param time
     * @param pingTimeOut
     * @param protocolTime
     */
    public PingSender(DatagramSocket socket, Set<String> memberSet, AtomicBoolean ackReceived,
                      ConcurrentHashMap<Info, Integer> infoMap, ConcurrentHashMap<String, Integer> recentlyLeft, String idStr, String introID,
                      AtomicBoolean introducer_failed, AtomicInteger time, long pingTimeOut, long protocolTime) {
        this.socket=socket;
        this.pingTimeOut=pingTimeOut;
        this.protocolTime=protocolTime;
        this.memberSet=memberSet;
        this.infoMap=infoMap;
        this.recentlyLeft=recentlyLeft;
        this.idString=idStr;
        this.introID=introID;
        this.time=time;
        this.ackReceived=ackReceived;
        this.introFailed=introducer_failed;
    }

    /**Terminate sender
     * 
     */
    public void terminate() {
        leave=true;
    }

 
    private void sendPing(String destID,AtomicInteger counterKey) {
        byte [] sendData = Message.MessageBuilder
                .buildPingMessage(String.valueOf(counterKey.get()),idString)
                .addInfoFromList(infoMap.keySet())
                .getMessage()
                .toByteArray();
        Pid destination = Pid.getPid(destID);
        sendMessage(sendData,destination);
    }

    private void sendPingReq(String relayerID, String destID,AtomicInteger counter) {
        byte [] sendData = Message.MessageBuilder
                .buildPingReqMessage(String.valueOf(counter.get()), idString, destID)
                .getMessage()
                .toByteArray();
        Pid destination = Pid.getPid(relayerID);
        sendMessage(sendData, destination);
    }

	private void sendMessage(byte [] sendData, Pid destination){
        System.out.println("[SENDER] [INFO] [" + System.currentTimeMillis() + "] message sent  : " +
                new String(sendData, 0, sendData.length) + " : destination : " + destination.pidStr);
		 try{
			 DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                     InetAddress.getByName(destination.hostname),destination.port);
			 socket.send(sendPacket);
		 }catch(IOException e){
			 e.printStackTrace();
		 }
	}

    /** Shuffle memlist for completeness guarantee
     * @return
     */
    ListIterator<String> getShuffledMembers() {
        // TODO handle empty memberSet
        List<String> shuffledMembers=new ArrayList<String>(memberSet);
        Collections.shuffle(shuffledMembers);
        ListIterator<String> iterator=shuffledMembers.listIterator();
        return iterator;
    }

    /**
     * Add info to buffer
     */
    private void updateInfoBuffer() {
        System.out.println("[DEBUG] INFO BUFFER : " + infoMap);
        for (Info i : this.infoMap.keySet()) {
            if (infoMap.get(i)<this.time.get())
                infoMap.remove(i);
        }
    }

    /**
     * Add failed to recently failed
     */
    private void updateRecentlyLeftList() {
        System.out.println("[DEBUG] RECENTLY LEFT : "+recentlyLeft);
        for (String i : this.recentlyLeft.keySet()) {
            if (recentlyLeft.get(i)<this.time.get())
                recentlyLeft.remove(i);
        }
    }

    private void printMembershipList() {
        System.out.println("[DEBUG] MEMBERSHIP LIST : "+memberSet);
        System.out.println("[DEBUG] INTRO FAILED : "+introFailed.get());
    }

	/** Start sender
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run(){
        ListIterator<String> shuffledIterator=getShuffledMembers();

		while(!leave) {
            long startTime = System.currentTimeMillis();

            if (!shuffledIterator.hasNext()) {
                shuffledIterator = getShuffledMembers();
                continue;
            }

            String pingMemberID = shuffledIterator.next();

            // skip if shuffledList contains a member which is deleted from memberSet in between
            if (!memberSet.contains(pingMemberID))
                continue;

            time.getAndIncrement();
            ackReceived.set(false);
            updateInfoBuffer();
            updateRecentlyLeftList();
            printMembershipList();
            sendPing(pingMemberID, time);

            try {
                synchronized (ackReceived) {
                    ackReceived.wait(pingTimeOut);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (ackReceived.get()) {
                if (pingMemberID.equals(introID))
                    introFailed.set(false);
                sleepThread(startTime);
            } else {
                //if message not in awklist
                //send ping_requests
                ackReceived.set(false);
                ListIterator<String> shuffledk = getShuffledMembers();
                for (int i = 0; i < SUBGROUP_K; i++) {
                    if (!shuffledk.hasNext())
                        break;

                    String nextMember = shuffledk.next();
                    if (!memberSet.contains(nextMember)) {
                        i--;
                        continue;
                    }

                    sendPingReq(nextMember, pingMemberID, time);
                }

                try {
                    synchronized (ackReceived) {
                        ackReceived.wait(startTime + protocolTime - System.currentTimeMillis());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (!ackReceived.get()) {
                    this.infoMap.putIfAbsent(new Info(Info.InfoType.FAILED, pingMemberID), (int) FailureDetector
                            .getSpreadTime(memberSet.size()) + this.time.intValue());
                    this.recentlyLeft.putIfAbsent(pingMemberID,this.time.intValue()+3*memberSet.size());
                    if (pingMemberID.equals(introID)) {
                        System.out.println("[SENDER] [INFO] [" + System.currentTimeMillis() + "] : introducer " +
                                "failure detected " + ": " + pingMemberID);
                        introFailed.set(true);
                    } else {
                        this.memberSet.remove(pingMemberID);
                        System.out.println("[SENDER] [MEM_REMOVE] [" + System.currentTimeMillis() + "] : " +
                                "failure detected : " + pingMemberID);
                    }
                } else {
                    if (pingMemberID.equals(introID))
                        introFailed.set(false);
                    sleepThread(startTime);
                }
            }
        }

        leaveSequence();
	}

    /**
     * Start leaving protocol
     */
    private void leaveSequence() {
        int timePeriods=Math.min(memberSet.size(), (int) FailureDetector.getSpreadTime(memberSet.size()));
        Iterator<String> shuffledIterator=getShuffledMembers();
        for (int i=0;i<timePeriods;i++) {
            long startTime=System.currentTimeMillis();
            time.getAndIncrement();
            byte [] sendData = Message.MessageBuilder
                    .buildPingMessage(String.valueOf(time.get()),idString)
                    .addLeaveInfo(idString)
                    .getMessage()
                    .toByteArray();
            sendMessage(sendData,Pid.getPid(shuffledIterator.next()));
            sleepThread(startTime);
        }
    }

    /** Wait for sometime after joining
     * @param startTime
     */
    private void sleepThread(long startTime) {
        try {
            Thread.sleep(startTime+protocolTime-System.currentTimeMillis());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
