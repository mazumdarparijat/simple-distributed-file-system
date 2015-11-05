package cs425.mp3;
import cs425.mp3.sdfsserverMain;
import cs425.mp3.FailureDetector.FailureDetector;
public class FailureDetectorThread extends Thread{
	private FailureDetector FD;
	public FailureDetectorThread(FailureDetector FD){
		this.FD=FD;
	}
	@Override
	public void run(){
		FD.startFD();
	}
}
