package cs425.mp3;

import cs425.mp3.FileServer.Message;
import cs425.mp3.FileServer.MessageType;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by parijatmazumdar on 02/11/15.
 */
public class SDFSClient {
    private static final String introIP= "Parijats-MacBook-Pro.local";
    private static final int introPort=9101;

    public static void main(String [] args) {
        if (args[0].equals("put")) {
            fileOps(args[1], args[2], 'p');
        } else if (args[0].equals("get")) {
            fileOps(args[1], args[2], 'g');
        } else if (args[0].equals("del")) {
            deleteFile(args[1]);
        } else if (args[0].equals("rep")) {
            replicateFile(args[1],args[2],Integer.parseInt(args[3]));
        } else {
            System.out.println("Not implemented");
        }
    }

    private static void replicateFile(String filename, String ip, int port) {
        Scanner soIn=null;
        PrintWriter soOut=null;
        Socket sock=null;
        try {
            sock=new Socket(introIP,introPort);
            sock.setSoTimeout(2000);
            soIn=new Scanner(new InputStreamReader(sock.getInputStream()));
            soIn.useDelimiter("\n");
            soOut=new PrintWriter(new OutputStreamWriter(sock.getOutputStream()),true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        soOut.println(Message.createReplicateMessage(filename,ip,port));
        soOut.flush();

        try {
            Message reply=Message.retrieveMessage(soIn.next());
            if (reply.type.equals(MessageType.YES)) {
                System.out.println("replication done");
            } else if (reply.type.equals(MessageType.NO)) {
                System.out.println("Nay received");
            } else {
                System.out.println("idk");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void deleteFile(String fname) {
        Scanner soIn=null;
        PrintWriter soOut=null;
        Socket sock=null;
        try {
            sock=new Socket(introIP,introPort);
            sock.setSoTimeout(2000);
            soIn=new Scanner(new InputStreamReader(sock.getInputStream()));
            soIn.useDelimiter("\n");
            soOut=new PrintWriter(new OutputStreamWriter(sock.getOutputStream()),true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        soOut.println(Message.createDelMessage(fname));
        soOut.flush();

        try {
            Message reply=Message.retrieveMessage(soIn.next());
            if (reply.type.equals(MessageType.YES)) {
                System.out.println("deleted");
            } else if (reply.type.equals(MessageType.NO)) {
                System.out.println("Nay received");
            } else {
                System.out.println("idk");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fileOps(String srcfname, String destfname,char op) {
        assert op=='p' || op=='g' : "op can only be either p or g.";
        Scanner soIn=null;
        PrintWriter soOut=null;
        Socket sock=null;
        try {
            sock=new Socket(introIP,introPort);
            sock.setSoTimeout(2000);
            soIn=new Scanner(new InputStreamReader(sock.getInputStream()));
            soIn.useDelimiter("\n");
            soOut=new PrintWriter(new OutputStreamWriter(sock.getOutputStream()),true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (op=='p')
            soOut.println(Message.createPutMessage(destfname));
        else if (op=='g')
            soOut.println(Message.createGetMessage(srcfname));

        soOut.flush();

        try {
            Message reply=Message.retrieveMessage(soIn.next());
            if (reply.type.equals(MessageType.YES)) {
                if (op=='p') sendFile(soOut, srcfname);
                else if (op=='g') receiveFile(soIn,destfname);
            } else if (reply.type.equals(MessageType.NO)) {
                System.out.println("Nay received");
            } else {
                System.out.println("idk");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void receiveFile(Scanner soIn, String destfname) {
        BufferedWriter bw=null;
        try {
            bw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destfname)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        boolean endReceived=false;
        while (soIn.hasNext()) {
            String text=soIn.next();
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
            System.out.println("end received. new file in local");
        } else {
            System.out.println("end not received. deleting file");
            new File(destfname).delete();
        }
    }

    private static void sendFile(PrintWriter soOut, String srcfname) {
        try {
            Scanner in=new Scanner(new InputStreamReader(new FileInputStream(srcfname)));
            in.useDelimiter("\n");
            while (in.hasNext()) {
                soOut.println(in.next());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        soOut.println(Message.EOF);
    }
}
