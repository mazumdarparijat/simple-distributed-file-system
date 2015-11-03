package cs425.mp3;
import java.io.IOException;
import cs425.mp3.ElectionService.ElectionService;
public class ElectionServiceThread extends Thread{
	private ElectionService ES;
	public ElectionServiceThread(ElectionService ES){
		this.ES=ES;
	}
	@Override
	public void run(){
		try {
			ES.startES();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
