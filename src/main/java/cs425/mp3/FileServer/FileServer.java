package cs425.mp3.FileServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class FileServer extends Thread {
    private Set<String> sdfsfilenames;
    private final int port;
    public FileServer(int port) {
        this.port=port;
        sdfsfilenames= Collections.synchronizedSet(new HashSet<String>());
    }

    public ArrayList<String> getFilesInServer() {
        ArrayList<String> ret=new ArrayList<String>();
        for (String s : sdfsfilenames)
            ret.add(s);

        return ret;
    }
    @Override
    public void run() {
        try {
            ServerSocket listener=new ServerSocket(this.port);
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
