package cs425.mp3.ElectionService;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import cs425.mp3.sdfsproxyMain;
import cs425.mp3.ElectionService.Message.MessageBuilder;
import cs425.mp3.FailureDetector.FailureDetector;

/**
 * Class for MasterTracker Module
 *
 */
public class MasterTracker {
	private String master=null;
	private ServerSocket welcomeSocket;
	private FailureDetector FD;
	public MasterTracker(int port){
		FD=sdfsproxyMain.FD;
		try {
			welcomeSocket=new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("[ERROR]: Can't Create Server Socket");
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
		if(m.type==MessageType.MASTER){
			String masterID="NOT_SET";
			if(master!=null && FD.isAlive(master)){
				masterID=master;
			}
			String msgreply = MessageBuilder.buildReplyMessage(masterID).toString();
			sendMessage(clientSocket,msgreply);
		}
		if(m.type==MessageType.COORDINATOR){
			String new_master=m.messageParams[0];
			master=new_master;
			System.out.println("[Election]:"+FD.getSelfID().pidStr);
			String msgreply = MessageBuilder.buildOKMessage(FD.getSelfID().pidStr).toString();
			System.out.println(msgreply);
			sendMessage(clientSocket,msgreply);
		}
	}
	public void startMT() {
		while(true){
			try{
				Socket connectionSocket = welcomeSocket.accept();
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				String msg=inFromClient.readLine();
				System.out.println("Recieved Message "+msg);
				if(msg!=null){
					handleMessage(msg,connectionSocket);
				}
				connectionSocket.close();
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