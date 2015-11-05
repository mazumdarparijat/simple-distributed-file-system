package cs425.mp3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class InputProcessorThread extends Thread{
	private void processUserCommand(String line) {
        if (line.equals("m")) {
        	System.err.println("MEMBERSHIP LIST");
        	List<String> memlist = sdfsserverMain.FD.getMemlistSkipIntroducer();
        	for(String member: memlist){
        		System.err.println(member);
        	}
        	String intro=sdfsserverMain.FD.getIntroID().toString();
        	if(sdfsserverMain.FD.isAlive(intro)){
        		System.err.println(intro);
        	}
        } else if (line.equals("i")) {
        	System.err.println("SELF ID");
        	System.err.println(sdfsserverMain.FD.getSelfID().toString());
        } else if (line.equals("l")) {
        	System.err.println("Leave Initiated");
        	sdfsserverMain.FD.leaveInitiate();
        }else if (line.equals("b")){
        	System.err.println("Master");
        	System.err.println(sdfsserverMain.ES.getMaster());
        }else if (line.equals("f")){
        	System.err.println("Files Stored");
        	for (String filename : sdfsserverMain.FS.getFilesInServer()){
        		System.err.println(filename);
        	}
        } 
        else {
            System.err.println("argument not recognised. Press m to get membership list, i to get id, l to leave, "
            		+ "b to get master and f to list files stores");
        }
    }
	@Override
	public void run(){
		System.err.println("Ready to take arguments. Press m to get membership list, i to get id, l to leave, "
            		+ "b to get master and f to list files stores");
		BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
		while(true){
	        try {
				String line=br.readLine();
				processUserCommand(line);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}
