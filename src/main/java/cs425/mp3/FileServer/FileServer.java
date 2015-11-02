package cs425.mp3.FileServer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by parijatmazumdar on 02/11/15.
 */
public class FileServer extends Thread {
    private Set<String> fileNames;
    private final int port;
    public FileServer(int port) {
        this.port=port;
    }

    @Override
    public void run() {
        ServerSocket listener=null;
        try {
            listener=new ServerSocket(this.port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(true) {
            Socket connection=null;
            try {
                connection=listener.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Scanner in=null;
            PrintWriter out=null;
            try {
                in=new Scanner(new InputStreamReader(connection.getInputStream()));
                in.useDelimiter("\n");
                out=new PrintWriter(new OutputStreamWriter(connection.getOutputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            handleRequest(in,out);
        }
    }

    private void handleRequest(Scanner in, PrintWriter out) {
        String messageRequest=in.next();
    }
}
