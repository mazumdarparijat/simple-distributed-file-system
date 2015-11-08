package cs425.mp3.FileServer;

import cs425.mp3.sdfsserverMain;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**Class for local File Server
 *
 */
public class FileServer extends Thread {
    static final String baseDir=System.getProperty("user.home")+"/Desktop/CS425Project/"
            + sdfsserverMain.FD.getSelfID().toString()+"/";
    private Set<String> sdfsfilenames;
    private final int port;
    public FileServer(int port) {
        this.port=port;
        sdfsfilenames= Collections.synchronizedSet(new HashSet<String>());
        try {
            FileUtils.deleteDirectory(new File(baseDir));
            FileUtils.forceMkdir(new File(baseDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**Getter method to get meta data of local files
     * @return
     */
    public ArrayList<String> getFilesInServer() {
        ArrayList<String> ret=new ArrayList<String>();
        for (String s : sdfsfilenames)
            ret.add(s);

        return ret;
    }
    @Override
    public void run() {
    	ServerSocket listener=null;
        try {
            listener=new ServerSocket(this.port);
            System.out.println(listener.getLocalSocketAddress() + ":" + listener.getLocalPort());
            while(true) {
                Socket connection=listener.accept();
                connection.setSoTimeout(2000);
                new FileServerThread(connection,sdfsfilenames).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
