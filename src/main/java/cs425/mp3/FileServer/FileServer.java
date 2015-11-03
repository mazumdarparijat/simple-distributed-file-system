package cs425.mp3.FileServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class FileServer extends Thread {
    private Set<String> sdfsfilenames;
    private final int port;
    public FileServer(int port) {
        this.port=port;
        sdfsfilenames=new HashSet<String>();
    }

    @Override
    public void run() {
        ServerSocket listener=null;
        try {
            listener=new ServerSocket(this.port);
            System.out.println(listener.getLocalSocketAddress() + ":" + listener.getLocalPort());
            ArrayList<Thread> threads=new ArrayList<Thread>();
            while(true) {
                Socket connection=listener.accept();
                connection.setSoTimeout(2000);
                FileServerThread ft=new FileServerThread(connection);
                threads.add(ft);
                ft.start();
                Scanner in=null;
                PrintWriter out=null;
                try {
                    in=new Scanner(new InputStreamReader(connection.getInputStream()));
                    in.useDelimiter("\n");
                    out=new PrintWriter(new OutputStreamWriter(connection.getOutputStream()),true);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    handleRequest(in,out);
                } catch (InterruptedIOException e) {
                    e.printStackTrace();
                }

                try {
                    connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void handleRequest(Scanner in, PrintWriter out) throws InterruptedIOException {
        Message messageRequest= null;
        try {
            messageRequest = Message.retrieveMessage(in.next());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (messageRequest.type.equals(MessageType.GET)) {
            if (sdfsfilenames.contains(messageRequest.fileName)) {
                out.println(Message.createOkayMessage());
                out.flush();
                sendSDFSFile(out,messageRequest.fileName);
            } else {
                out.println(Message.createNayMessage());
                out.flush();
            }
        } else if (messageRequest.type.equals(MessageType.PUT)) {
            if (sdfsfilenames.contains(messageRequest.fileName)) {
                out.println(Message.createNayMessage());
                out.flush();
            } else {
                out.println(Message.createOkayMessage());
                out.flush();
                createSDFSFile(in,messageRequest.fileName);
            }
        } else if (messageRequest.type.equals(MessageType.REP)) {
            boolean successful = replicateSDFSFile(messageRequest.fileName,
                    messageRequest.ipAddress,messageRequest.port);
            if (successful) {
                out.println(Message.createOkayMessage());
                out.flush();
            } else {
                out.println(Message.createNayMessage());
                out.flush();
            }

        } else if (messageRequest.type.equals(MessageType.DEL)) {
            new File(messageRequest.fileName).delete();
            sdfsfilenames.remove(messageRequest.fileName);
            out.println(Message.createOkayMessage());
            out.flush();
        } else {
            System.out.println("Not Implemented");
            System.exit(1);
        }
    }

    private boolean replicateSDFSFile(String fileName, String ipAddress, int port) {
        Socket socket=null;
        Scanner soIn=null;
        PrintWriter soOut=null;
        try {
            socket=new Socket(ipAddress,port);
            socket.setSoTimeout(2000);
            soIn=new Scanner(new InputStreamReader(socket.getInputStream()));
            soIn.useDelimiter("\n");
            soOut=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        soOut.println(Message.createGetMessage(fileName));
        soOut.flush();
        try {
            if (Message.retrieveMessage(soIn.next()).type.equals(MessageType.YES)) {
                createSDFSFile(soIn,fileName);
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void sendSDFSFile(PrintWriter out,String filename) {
        Scanner fileIn=null;
        try {
            fileIn=new Scanner(new InputStreamReader(new FileInputStream(filename)));
            fileIn.useDelimiter("\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (fileIn.hasNext()) {
            out.println(fileIn.next());
        }

        out.println(Message.EOF);
        out.flush();
        fileIn.close();
    }

    private void createSDFSFile(Scanner in, String fileName) throws InterruptedIOException {
        BufferedWriter bw=null;
        try {
            bw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        boolean endReceived=false;
        while (in.hasNext()) {
            String text=in.next();
            if (text.equals(Message.EOF)) {
                endReceived=true;
                break;
            }

            try {
                bw.write(text);
                bw.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (endReceived) {
            System.out.println("end received. new file in sdfs");
            sdfsfilenames.add(fileName);
        } else {
            System.out.println("end not received. deleting file");
            new File(fileName).delete();
        }
    }
}
