package cs425.mp3.MasterService;

import org.apache.commons.lang3.tuple.MutablePair;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import cs425.mp3.Pid;
import cs425.mp3.sdfsserverMain;
import cs425.mp3.ElectionService.Message;
import cs425.mp3.ElectionService.MessageType;
import cs425.mp3.ElectionService.Message.MessageBuilder;
import cs425.mp3.FailureDetector.FailureDetector;

public class MasterService {
	private int REPLICATION_TIMEOUT = 20000;
	private int REPLICATION_UNIT=3;
	private int SERVER_TIMEOUT=5000;
	private FailureDetector FD;
	private ServerSocket welcomeSocket;
	private HashMap<String,MutablePair<Set <String>, Long>> filemap;
	private void updateFileMap(String serverid, String filename){
		if(filemap.containsKey(filename)){
			filemap.get(filename).getLeft().add(serverid);
			filemap.get(filename).setRight(new Long(System.currentTimeMillis()));
		}
		else{
			Set<String> s=new HashSet<String>();
			s.add(serverid);
			Long timestamp=new Long(System.currentTimeMillis());
			filemap.put(filename, new MutablePair<Set<String>,Long>(s,timestamp));
		}
	}
	public MasterService(int port){
		FD=sdfsserverMain.FD;
		filemap=new HashMap<String,MutablePair<Set <String>, Long>>();
		try{
			welcomeSocket=new ServerSocket(port);
			welcomeSocket.setSoTimeout(SERVER_TIMEOUT);
		}catch(IOException e){
			e.printStackTrace();
			System.err.println("[MASTER]: Unable to start master server socket");
			System.exit(-1);
		}
	}
	private String sendMessage(Socket clientSocket, String msg) {
		String response=null;
		try {
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			outToServer.writeBytes(msg+'\n');
			response =inFromServer.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("[ERROR]: Sending Message");
		}
		return response;
	}
	private void handleMessage(String msg, Socket clientSocket) throws IOException{
		Message m=Message.extractMessage(msg);
		if(m.type==MessageType.OK){

		}
		else if(m.type==MessageType.PUT){
			String filename=m.messageParams[0];
			if(filemap.containsKey(filename)){
				String replymsg=MessageBuilder.buildPutReplyMessage("NOT_OK", new String[0]).toString();
				sendMessage(clientSocket,replymsg);
			}
			else {
				List<String> memlist=FD.getMemlistSkipIntroducerWithSelf();
				Collections.shuffle(memlist);
				int num=Math.min(memlist.size(), REPLICATION_UNIT);
				String servers[] = new String[num];
				for(int i=0;i<num;i++){servers[i]=memlist.get(i);}
				String replymsg=MessageBuilder.buildPutReplyMessage("OK", servers).toString();
				sendMessage(clientSocket,replymsg);
			}
		}
		else if(m.type==MessageType.GET){
			String filename=m.messageParams[0];
			if(!filemap.containsKey(filename)){
				String replymsg=MessageBuilder.buildGetReplyMessage("NOT_OK", new String[0]).toString();
				sendMessage(clientSocket,replymsg);
			}
			else{
				int num=filemap.get(filename).getLeft().size();
				String servers[] = new String[num];
				int idx=0;
				for(String server: filemap.get(filename).getLeft()){
					servers[idx]=server;
					idx++;
				}
				String replymsg=MessageBuilder.buildGetReplyMessage("OK", servers).toString();
				sendMessage(clientSocket,replymsg);
			}
		}
		else if(m.type==MessageType.LIST){
			String[] files = (new ArrayList<String>(filemap.keySet())).toArray(new String[0]);
			String replymsg=MessageBuilder.buildListReplyMessage(FD.getSelfID().toString(), files).toString();
			sendMessage(clientSocket,replymsg);
		}
		else if(m.type==MessageType.DELETE){
			String filename=m.messageParams[0];
			if(!filemap.containsKey(filename)){
				String replymsg=MessageBuilder.buildDeleteReplyMessage("NOT_OK").toString();
				sendMessage(clientSocket,replymsg);
			}
			else{
				for(String server: filemap.get(filename).getLeft()){
					String delmsg=cs425.mp3.FileServer.Message.createDelMessage(filename).toString();
					try{
						Socket connectionSocket=new Socket(Pid.getPid(server).hostname,Pid.getPid(server).port+sdfsserverMain.FSPortDelta);
						sendMessage(connectionSocket,delmsg);
						connectionSocket.close();
					}catch(IOException e){
						e.printStackTrace();
						System.out.println("[ERROR][MASTER]: Error sending delete to "+server);
					}
				}
				filemap.remove(filename);
				String replymsg=MessageBuilder.buildDeleteReplyMessage("OK").toString();
				sendMessage(clientSocket,replymsg);
			}
		}
		else if(m.type==MessageType.NEWFILES){
			String serverid=m.messageParams[0];
			int numfiles=m.messageParams.length-1;
			for(int i=1;i<=numfiles;i++){
				updateFileMap(serverid,m.messageParams[i]);
			}
			String msgreply=MessageBuilder.buildOKMessage(FD.getSelfID().toString()).toString();
			sendMessage(clientSocket,msgreply);
		}
		else{
			throw new IOException("Message not recognized");
		}
	} 
	void communicate(String add, int port, String msg){
		try{
			Socket clientSocket=new Socket(add,port);
			String response=sendMessage(clientSocket,msg);
			if(response!=null){
				handleMessage(response,clientSocket);
			}
			clientSocket.close();
		}catch(IOException e){
			e.printStackTrace();
			System.out.println("[ERROR][Election]: Unable to communicate with "+add+" on port "+port);
		}
	}
	private void informIntro(){
			String msg=MessageBuilder.buildCoordMessage(FD.getSelfID().pidStr).toString(); 
			communicate(sdfsserverMain.intro_address,
					sdfsserverMain.intro_port+sdfsserverMain.ESPortDelta,
					msg);
	}
	private void checkReplication(){
		Random rn = new Random();
		for(Iterator<Map.Entry<String, MutablePair<Set <String>, Long>>> it = filemap.entrySet().iterator(); it.hasNext();){
			Map.Entry<String, MutablePair<Set <String>, Long>> entry = it.next();
			String filename=entry.getKey();
			if((System.currentTimeMillis()-filemap.get(filename).getRight().longValue() )> REPLICATION_TIMEOUT){
				Set<String> replicaServers=filemap.get(filename).getLeft();
				for(String serverid: new HashSet<String>(replicaServers)){
					if(!FD.isAlive(serverid)){
						replicaServers.remove(serverid);
					}
				}
				if(replicaServers.size()==0){
					it.remove();
				}
				else{
					List<String> memlist=FD.getMemlistSkipIntroducerWithSelf();
					List<String> replicaServersList=new ArrayList<String>(replicaServers);
					if(replicaServers.size()<REPLICATION_UNIT &&(memlist.size()>=REPLICATION_UNIT)){
						int count=0;
						for(String serverid: memlist){
							if(count<REPLICATION_UNIT){
								if(replicaServers.contains(serverid)){
									count++;
								}
								else{
									try{
										Pid source=Pid.getPid(replicaServersList.get(rn.nextInt(replicaServers.size())));
										String msg=cs425.mp3.FileServer.Message.createReplicateMessage
												(filename, source.hostname, source.port+sdfsserverMain.FSPortDelta).toString();
										Socket clientSocket=new Socket(Pid.getPid(serverid).hostname,Pid.getPid(serverid).port+sdfsserverMain.FSPortDelta);
										sendMessage(clientSocket,msg);
										clientSocket.close();
										count++;
									}catch(IOException e){
										e.printStackTrace();
										System.out.println("[ERROR] [MASTER]: Error sending replicate message to "+ serverid);
									}
								}
							}
						}
						filemap.get(filename).setRight(new Long(System.currentTimeMillis()));
					}
				}
			}
		}
	}
	public void startMS(){
		boolean introducer_state=true;
		while(true){
			if(!introducer_state && FD.isAlive(FD.getIntroID().toString())){
				informIntro();
			}
			introducer_state=FD.isAlive(FD.getIntroID().toString());
			checkReplication();
			try{
				Socket connectionSocket = welcomeSocket.accept();
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				String msg=inFromClient.readLine();
				if(msg!=null){
					handleMessage(msg,connectionSocket);
				}
				connectionSocket.close();
			}
			catch(SocketTimeoutException e){
				System.out.println("[DEBUG][MASTER]: Socket Timeout");
			}
			catch(IOException e){
				e.printStackTrace();
				System.out.println("[ERROR][MASTER]: Connection Error");
			}
		}
	}
}
