package cs425.mp3.FileServer;

import java.net.Socket;

/**
 * Created by parijatmazumdar on 03/11/15.
 */
class FileServerThread extends Thread {
    Socket socket;
    FileServerThread(Socket sock) {
        socket=sock;
    }


}
