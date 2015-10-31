package cs425.mp3;
import cs425.mp3.sdfsserverMain;
public class FaliureDetectorThread extends Thread{
	@Override
	public void run(){
		boolean restart=sdfsserverMain.FD.startFD();
	}
}
