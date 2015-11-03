package cs425.mp3;
import cs425.mp3.sdfsserverMain;
import cs425.mp3.ElectionService.MasterTracker;
public class MasterTrackerThread extends Thread{
	private MasterTracker MT;
	public MasterTrackerThread(MasterTracker MT){
		this.MT=MT;
	}
	@Override
	public void run(){
		MT.startMT();
	}
}
