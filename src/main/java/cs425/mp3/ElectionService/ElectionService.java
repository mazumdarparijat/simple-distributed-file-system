package cs425.mp3.ElectionService;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;

import cs425.mp3.FailureDetector.FailureDetector;
import cs425.mp3.Pid;
import cs425.mp3.sdfsserverMain;
import cs425.mp3.ElectionService.Message.MessageBuilder;
/**
 * Class for SDFSServer Module
 *
 */
public class ElectionService {
	private int TIMEOUT=500;
	private final FailureDetector FD;
	private String master=null;
	private ServerSocket welcomeSocket;
	public ElectionService(int port) throws IOException{
		FD=sdfsserverMain.FD;
		welcomeSocket=new ServerSocket(port);
		welcomeSocket.setSoTimeout(TIMEOUT);
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
	private void setInitialMaster() {
		System.out.println("[Election]: Setting initial master");
		String msg = MessageBuilder.buildMasterMessage(FD.getSelfID().pidStr).toString();
		System.out.println("[Election]: Sent Msg: "+msg);
		try {
			Socket clientSocket = new Socket(sdfsserverMain.intro_address,sdfsserverMain.intro_port+1);
			String response=sendMessage(clientSocket,msg);
			if(response!=null){
				handleMessage(response,clientSocket);
			}
			clientSocket.close();
		}  catch (IOException e) {
			e.printStackTrace();
			System.out.println("[ERROR]: Server not running");
			System.exit(-1);
		}
	}
	private void handleMessage(String msg, Socket clientSocket) throws IOException{
		Message m=Message.extractMessage(msg);
		if(m.type==MessageType.REPLY){
			String new_master=m.messageParams[0];
			if(!new_master.equals("NOT_SET")){
				master=new_master;
			}
		}
		else if(m.type==MessageType.COORDINATOR){
			String new_master=m.messageParams[0];
			master=new_master;
			String msgreply = MessageBuilder.buildOKMessage(FD.getSelfID().pidStr).toString();
			sendMessage(clientSocket,msgreply);
		}
		else if(m.type==MessageType.OK){
			
		}
		else{
			throw new IOException("Message not recognized");
		}
	} 
	private Boolean needElection(){
		boolean res=false;
		if(master==null){
			res=true;
		}
		else if(!FD.isAlive(master)){
			res=true;
		}
		System.out.println("[Election] needElection:"+res);
		return res;
	}
	private Boolean isPotentialMaster(){
		List<String> memlist=FD.getMemlistSkipIntroducer();
		System.out.println("[Election]: Loop Start");
		for (String m:memlist){
			System.out.println("[Election]: Element "+m);
			System.out.println("[Election]: Element Self "+FD.getSelfID().toString());
			System.out.println(FD.getSelfID().toString().compareTo(m));
			if(FD.getSelfID().toString().compareTo(m)>=0){
				System.out.println("[Election] ispotentialMaster False");
				return false;
			}
		}
		System.out.println("[Election] ispotentialMaster True");
		return true;
	} 
	private void multicastMaster() throws IOException{
		List<String> memlist=FD.getMemlistSkipIntroducer();
		String msg=MessageBuilder.buildCoordMessage(FD.getSelfID().pidStr).toString(); 
		for (String m:memlist){
			Pid p = Pid.getPid(m);
			Socket clientSocket=new Socket(p.hostname,p.port+1);
			String response=sendMessage(clientSocket,msg);
			if(response!=null){
				handleMessage(response,clientSocket);
			}
			clientSocket.close();
		}
		Socket clientSocket=new Socket(sdfsserverMain.intro_address,sdfsserverMain.intro_port+1);
		String response=sendMessage(clientSocket,msg);
		if(response!=null){
			handleMessage(response,clientSocket);
		}
		clientSocket.close();
	}
	public void startES() throws IOException{
		setInitialMaster();
		System.out.println("[Election]: Initial Master Set");
		while(true){
			try{
				Socket connectionSocket = welcomeSocket.accept();
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				String msg=inFromClient.readLine();
				System.out.println("[Election]: Recieved Message "+msg);
				if(msg!=null){
					handleMessage(msg,connectionSocket);
				}
				connectionSocket.close();
			}
			catch(SocketTimeoutException e){
				System.out.println("[Election]: Timedout");
				if(needElection() && isPotentialMaster()){
					System.out.println("[Election]: set self as Master");
					master=FD.getSelfID().pidStr;
					multicastMaster();
				}
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	public String getMaster(){
		return master;
	}
}