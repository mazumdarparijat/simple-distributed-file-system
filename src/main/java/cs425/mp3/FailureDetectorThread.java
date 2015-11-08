package cs425.mp3;
import cs425.mp3.FailureDetector.FailureDetector;
/**
 * Thread Class for launching Failure Detector service
 *
 */
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
