package cs425.mp3.FileServer;

/**
 * Created by parijatmazumdar on 02/11/15.
 */
public class FileServerTest {
    public static void main(String [] args) {
        FileServer fs1=new FileServer(9100);
        fs1.start();

        FileServer fs2=new FileServer(9101);
        fs2.start();

        try {
            fs1.join();
            fs2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }
}
