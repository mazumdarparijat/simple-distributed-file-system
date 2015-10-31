package cs425.mp3;
import cs425.mp3.sdfsserverMain;
public class SDFSServerThread extends Thread{
	@Override
	public void run(){
		sdfsserverMain.FileServer.startFS();
	}
}
