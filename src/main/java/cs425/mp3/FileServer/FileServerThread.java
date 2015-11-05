package cs425.mp3.FileServer;

import cs425.mp3.Pid;
import cs425.mp3.sdfsserverMain;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by parijatmazumdar on 03/11/15.
 */
class FileServerThread extends Thread {
    Socket socket;
    Set<String> sdfsFiles;
    FileServerThread(Socket sock, Set<String> sdfsfiles) {
        socket=sock;
        sdfsFiles=sdfsfiles;
    }

    @Override
    public void run() {
        handleRequest(socket);
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private boolean replicateSDFSFile(String fileName, String ipAddress, int port) {
        try {
            Socket socket=new Socket(ipAddress,port);
            socket.setSoTimeout(2000);
            Scanner soIn=new Scanner(new InputStreamReader(socket.getInputStream()));
            soIn.useDelimiter("\n");
            PrintWriter soOut=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            soOut.println(Message.createGetMessage(fileName));
            soOut.flush();
            if (Message.retrieveMessage(soIn.next()).type.equals(MessageType.YES)) {
                createSDFSFile(socket,fileName);
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void sendSDFSFile(Socket connection,String filename) {
        try {
            DataOutputStream out=new DataOutputStream(connection.getOutputStream());
            byte [] buffer=new byte[1024];
            FileInputStream fileIn=new FileInputStream(filename);
            int readlen=0;
            while((readlen=fileIn.read(buffer))!=-1) {
                out.write(buffer,0,readlen);
            }

            out.flush();
            fileIn.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRequest(Socket connection) {
        try {
            Scanner in=new Scanner(new InputStreamReader(connection.getInputStream()));
            in.useDelimiter("\n");
            PrintWriter out=new PrintWriter(new OutputStreamWriter(connection.getOutputStream()));
            Message messageRequest = Message.retrieveMessage(in.next());
            String fname=FileServer.baseDir+messageRequest.fileName;
            if (messageRequest.type.equals(MessageType.GET)) {
                if (sdfsFiles.contains(fname)) {
                    out.println(Message.createOkayMessage());
                    out.flush();
                    sendSDFSFile(connection,fname);
                } else {
                    out.println(Message.createNayMessage());
                    out.flush();
                }
            } else if (messageRequest.type.equals(MessageType.PUT)) {
                if (sdfsFiles.contains(fname)) {
                    out.println(Message.createNayMessage());
                    out.flush();
                } else {
                    out.println(Message.createOkayMessage());
                    out.flush();
                    createSDFSFile(connection,fname);
                }
            } else if (messageRequest.type.equals(MessageType.REP)) {
                replicateSDFSFile(fname,
                        messageRequest.ipAddress,messageRequest.port);
            } else if (messageRequest.type.equals(MessageType.DEL)) {
                new File(fname).delete();
                sdfsFiles.remove(fname);
            } else {
                System.out.println("Not Implemented");
                System.exit(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createSDFSFile(Socket connection, String fileName) throws InterruptedIOException {
        try {
            FileOutputStream fs=new FileOutputStream(fileName);
            byte[] buffer=new byte[1024];
            DataInputStream in=new DataInputStream(connection.getInputStream());
            int readlen;
            while ((readlen=in.read(buffer))>0) {
                fs.write(buffer,0,readlen);
            }

            fs.close();
            sdfsFiles.add(fileName);
            notifyFileAdd(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notifyFileAdd(String fileName) {
        Pid master=sdfsserverMain.ES.getMasterPid();
        try {
            Socket sock=new Socket(master.hostname,master.port);
            Scanner in=new Scanner(new InputStreamReader(sock.getInputStream()));
            PrintWriter out=new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
            String [] filenames={fileName};
            out.println(cs425.mp3.ElectionService.Message
                    .MessageBuilder
                    .buildNewfilesMessage(sdfsserverMain.FD.getSelfID().toString(),filenames)
                    .toString());
            if (in.hasNext()) {
                in.next();
            }

            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
