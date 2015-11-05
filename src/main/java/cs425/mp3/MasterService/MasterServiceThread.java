package cs425.mp3.MasterService;
public class MasterServiceThread extends Thread{
	private MasterService MS;
	public MasterServiceThread(MasterService MS){
		this.MS=MS;
	}
	@Override
	public void run(){
		MS.startMS();
	}
}
