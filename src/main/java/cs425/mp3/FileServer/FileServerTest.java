package cs425.mp3.FileServer;


import cs425.mp3.sdfsserverMain;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by parijatmazumdar on 02/11/15.
 */
public class FileServerTest {
    static final String baseDir=System.getProperty("user.home")+"/Desktop/CS425Project/"+"test";
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
