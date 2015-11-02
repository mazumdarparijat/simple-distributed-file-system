package cs425.mp3;
import cs425.mp3.sdfsserverMain;
public class FailureDetectorThread extends Thread{
	@Override
	public void run(){
		boolean restart=sdfsserverMain.FD.startFD();
	}
}
