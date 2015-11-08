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

/** Election Service Class
 *
 */
public class ElectionService {
	private int SERVER_TIMEOUT=100;
	private final FailureDetector FD;
	private String master=null;
	private ServerSocket welcomeSocket;
	private final int MASTER_PREP_TIME=50;
	public ElectionService(int port) throws IOException{
		FD=sdfsserverMain.FD;
		welcomeSocket=new ServerSocket(port);
		welcomeSocket.setSoTimeout(SERVER_TIMEOUT);
	}
	/**Send Message over TCP to clientSocket
	 * @param clientSocket
	 * @param msg
	 * @return
	 */
	private String sendMessage(Socket clientSocket, String msg) {
		String response=null;
		try {
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			outToServer.writeBytes(msg+'\n');
			System.out.println("[DEBUG][Election]: Sent Message "+msg);
			response =inFromServer.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("[ERROR][Election]: Sending Message "+msg);
		}
		return response;
	}
	/**Set Initial Master when launched
	 * 
	 */
	private void setInitialMaster() {
		System.out.println("[DEBUG][Election]: Setting initial master");
		String msg = MessageBuilder.buildMasterMessage(FD.getSelfID().toString()).toString();
		communicate(sdfsserverMain.intro_address,sdfsserverMain.intro_port+sdfsserverMain.ESPortDelta,msg);
	}
	/**Handle reply from clientSocket
	 * @param msg
	 * @param clientSocket
	 * @throws IOException
	 */
	private void handleMessage(String msg, Socket clientSocket) throws IOException{
		System.out.println("[DEBUG][Election]: Recived Message "+msg);
		Message m=Message.extractMessage(msg);
		if(m.type==MessageType.MASTER_REPLY){
			String new_master=m.messageParams[0];
			if(!new_master.equals("NOT_SET")){
				master=new_master;
			}
		}
		else if(m.type==MessageType.COORDINATOR){
			String new_master=m.messageParams[0];
			master=new_master;
			String[] files=sdfsserverMain.FS.getFilesInServer().toArray(new String[0]);
			String msgmaster = MessageBuilder.buildNewfilesMessage(FD.getSelfID().toString(), files).toString();
			communicate(Pid.getPid(master).hostname,
					Pid.getPid(master).port+sdfsserverMain.MSPortDelta,msgmaster);
			String msgreply = MessageBuilder.buildOKMessage(FD.getSelfID().toString()).toString();
			sendMessage(clientSocket,msgreply);
		}
		else if(m.type==MessageType.OK){
			
		}
		else{
			throw new IOException("Message not recognized");
		}
	} 
	/**Communicate message to other processes
	 * @param add
	 * @param port
	 * @param msg
	 */
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
	/**Indicates whether election is needed
	 * @return
	 */
	private Boolean needElection(){
		boolean res=false;
		if(master==null){
			res=true;
		}
		else if(!FD.isAlive(master)){
			res=true;
		}
		return res;
	}
	/**Indicates whether self is potential master
	 * @return
	 */
	private Boolean isPotentialMaster(){
		List<String> memlist=FD.getMemlistSkipIntroducer();
		for (String m:memlist){
			if(FD.getSelfID().toString().compareTo(m)>=0){
				return false;
			}
		}
		return true;
	} 
	/**MultiCast New master to other in group
	 * 
	 */
	private void multicastMaster(){
		List<String> memlist=FD.getMemlistSkipIntroducer();
		String msg=MessageBuilder.buildCoordMessage(FD.getSelfID().pidStr).toString(); 
		for (String m:memlist){
			System.out.println("[DEBUG][ELECTION]: communicating with "+m);
			Pid p = Pid.getPid(m);
			communicate(p.hostname, p.port + sdfsserverMain.ESPortDelta, msg);
		}

		communicate(sdfsserverMain.intro_address, sdfsserverMain.intro_port + sdfsserverMain.ESPortDelta, msg);
		System.out.println("[DEBUG][ELECTION]: sent master notification to intro");
	}
	/**Start Election Service
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void startES() throws IOException, InterruptedException{
		setInitialMaster();

		System.out.println("[DEBUG][Election]: Initial Master Set : "+master);
		while(true){
			try{
				System.out.println("[DEBUG][Election]: Waiting to accept connection");
				Socket connectionSocket = welcomeSocket.accept();
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				String msg=inFromClient.readLine();
				if(msg!=null){
					handleMessage(msg,connectionSocket);
				}
				connectionSocket.close();
			}
			catch(SocketTimeoutException e){
				System.out.println("[DEBUG][Election]: Timedout");
				if(needElection() && isPotentialMaster()){
					System.out.println("[DEBUG][Election]: set self as Master");
					master=FD.getSelfID().pidStr;
					sdfsserverMain.launchMaster(sdfsserverMain.FS.getFilesInServer());
					Thread.sleep(MASTER_PREP_TIME);
					multicastMaster();
				}
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	/**Getter method for Master
	 * @return
	 */
	public String getMaster(){
		return master;
	}
	/**Getter method for master
	 * @return
	 */
	public Pid getMasterPid(){
		return Pid.getPid(master);
	}
}