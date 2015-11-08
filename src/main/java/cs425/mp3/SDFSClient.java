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
    private static String introIP;
    private static int introPort;
    private static final int ElectionPortDelta=1;
    private static final int FSPortDelta=2;
    private static final int MasterPortDelta=3;

    public static void main(String [] args) {
        assert args.length==2 : "usage : String argument introducerIP and int argument introducer port reqd!";
        introIP=args[0];
        introPort=Integer.parseInt(args[1]);
        System.out.println("Introducer IP : "+introIP+". Introducer port : "+introPort);
        BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
        boolean exit=false;
        while(!exit) {
            System.out.println("press exit to end, put srcfilename sdfsfilename," +
                    " get sdfsfilename destfilename, del sdfsfilename, list sdfsfilename, ls");
            try {
                String [] input=br.readLine().split(" ");
                if (input[0].equals("exit"))
                    exit=true;
                else if (input[0].equals("put")) {
                    System.out.println("put requested");
                    fileOps(input[1],flattenFilename(input[2]),'p');
                } else if (input[0].equals("get")) {
                    fileOps(flattenFilename(input[1]),input[2],'g');
                } else if (input[0].equals("del")) {
                    fileOps(flattenFilename(input[1]),"",'d');
                } else if (input[0].equals("list")) {
                    fileOps(flattenFilename(input[1]),"",'l');
                } else if (input[0].equals("ls")) {
                    listfiles();
                } else {
                    System.out.println("argument not recognized. Try again");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("arguments not provided correctly!");
                e.printStackTrace();
            }

            System.out.println("Request handled");
        }
    }

    private static void listfiles() {
        Pid master=getMaster();
        try {
            Socket sock=new Socket(master.hostname,master.port+MasterPortDelta);
            sock.setSoTimeout(2000);
            Scanner soIn=new Scanner(new InputStreamReader(sock.getInputStream()));
            soIn.useDelimiter("\n");
            PrintWriter soOut=new PrintWriter(new OutputStreamWriter(sock.getOutputStream()),true);
            soOut.println(cs425.mp3.ElectionService.Message
                    .MessageBuilder
                    .buildListMessage(InetAddress.getLocalHost().getHostName()));
            soOut.flush();
            if (soIn.hasNext()) {
                cs425.mp3.ElectionService.Message reply= cs425.mp3.ElectionService
                        .Message
                        .extractMessage(soIn.next());
                System.out.println("Files in the sdfs are the following : ");
                for (int i=1;i<reply.messageParams.length;i++)
                    System.out.println(unflattenFilename(reply.messageParams[i]));
            } else {
                System.out.println("ls failed. master is down!");
            }
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String unflattenFilename(String fname) {
        String ret=fname.replace("$", "/");
        return ret;
    }

    private static String flattenFilename(String fname) {
        String ret=fname.replace("/", "$");
        System.out.println("flattened file = "+ret);
        return ret;
    }

    private static Pid getMaster() {
        while (true) {
            System.out.println("Getting Master");
            Socket sock=null;
            try {
                sock = new Socket(introIP, introPort + ElectionPortDelta);
                Scanner in = new Scanner(new InputStreamReader(sock.getInputStream()));
                in.useDelimiter("\n");
                PrintWriter out = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
                out.println(cs425.mp3.ElectionService.Message
                        .MessageBuilder
                        .buildMasterMessage(InetAddress.getLocalHost().getHostName())
                        .toString());
                out.flush();
                cs425.mp3.ElectionService.Message reply = cs425.mp3.ElectionService
                        .Message
                        .extractMessage(in.next());
                if (!reply.messageParams[0].equals("NOT_SET")) {
                    return Pid.getPid(reply.messageParams[0]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    sock.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("Master not set! I will try again.");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void fileOps(String srcfname, String destfname,char op) {
        assert op=='p' || op=='g' || op=='d' || op=='l' : "op can only be either p or g.";
        Pid master=getMaster();
        System.out.println("master = "+master.toString());
        try {
            Socket sock=new Socket(master.hostname,master.port+MasterPortDelta);
            sock.setSoTimeout(2000);
            Scanner soIn=new Scanner(new InputStreamReader(sock.getInputStream()));
            soIn.useDelimiter("\n");
            PrintWriter soOut=new PrintWriter(new OutputStreamWriter(sock.getOutputStream()),true);

            if (op=='p') {
                putOperation(soIn, soOut,srcfname,destfname);
            } else if (op=='g') {
                getOperation(soIn, soOut, srcfname, destfname);
            } else if (op=='d') {
                deleteOperation(soIn,soOut,srcfname);
            } else if (op=='l') {
                listOperation(soIn,soOut,srcfname);
            } else {
                System.out.println("op not recognized!");
            }

            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteOperation(Scanner in, PrintWriter out, String fname) {
        out.println(cs425.mp3.ElectionService.Message
                .MessageBuilder
                .buildDeleteMessage(fname));
        out.flush();
        if (in.hasNext()) {
            cs425.mp3.ElectionService.Message reply= cs425.mp3.ElectionService
                    .Message
                    .extractMessage(in.next());
            if (reply.messageParams[0].equals("NOT_OK"))
                System.out.println("file not found in sdfs");
            else
                System.out.println("file deleted");
        } else {
            System.out.println("delete failed. master is down!");
        }
    }

    private static void listOperation(Scanner in, PrintWriter out, String srcfname) {
        out.println(cs425.mp3.ElectionService.Message
                .MessageBuilder
                .buildGetMessage(srcfname)
                .toString());
        out.flush();
        if (in.hasNext()) {
            cs425.mp3.ElectionService.Message reply= cs425.mp3.ElectionService
                    .Message
                    .extractMessage(in.next());
            if (reply.messageParams[0].equals("NOT_OK"))
                System.out.println("file not found in sdfs");
            else {
                System.out.println("The file is replicated in the following VMs : ");
                for (int i=1;i<reply.messageParams.length;i++) {
                    System.out.println(reply.messageParams[i]);
                }
            }
        } else {
            System.out.println("list failed. master is down!");
        }
    }

    private static void getOperation(Scanner soIn, PrintWriter soOut, String sdfsfname, String destfname) {
        soOut.println(cs425.mp3.ElectionService.Message
                .MessageBuilder
                .buildGetMessage(sdfsfname)
                .toString());
        soOut.flush();
        cs425.mp3.ElectionService.Message reply= cs425.mp3.ElectionService.Message
                .extractMessage(soIn.next());
        if (reply.messageParams[0].equals("NOT_OK")) {
            System.out.println("Get operation cannot be completed. File does not exist in sdfs");
        } else {
            boolean got=false;
            for (int i=1;i<reply.messageParams.length && !got;i++) {
                got=receiveFile(Pid.getPid(reply.messageParams[i]), sdfsfname, destfname);
            }

            if (!got)
                System.out.println("Get operation cannot be completed. " +
                        "All file servers rejected replying the file");
        }
    }

    private static boolean receiveFile(Pid pid, String sdfsfname, String destfname) {
        try {
            Socket sock = new Socket(pid.hostname, pid.port+FSPortDelta);
            Scanner in = new Scanner(new InputStreamReader(sock.getInputStream()));
            in.useDelimiter("\n");
            PrintWriter out = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
            out.println(Message.createGetMessage(sdfsfname));
            out.flush();
            Message reply = Message.retrieveMessage(in.next());
            if (reply.type.equals(MessageType.NO))
                return false;
            FileOutputStream fs=new FileOutputStream(destfname);
            byte[] buffer=new byte[1024];
            DataInputStream din=new DataInputStream(sock.getInputStream());
            int readlen;
            while ((readlen=din.read(buffer))!=-1) {
                fs.write(buffer,0,readlen);
            }

            fs.close();
            sock.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static void putOperation(Scanner soIn, PrintWriter soOut, String srcfname, String sdfsfname) {
        soOut.println(cs425.mp3.ElectionService.Message
                .MessageBuilder
                .buildPutMessage(sdfsfname)
                .toString());
        soOut.flush();
        System.out.println("put message sent");
        String replyString=soIn.next();
        System.out.println("reply = "+replyString);
        cs425.mp3.ElectionService.Message reply= cs425.mp3.ElectionService.Message
                .extractMessage(replyString);
        if (reply.messageParams[0].equals("NOT_OK")) {
            System.out.println("Put operation cannot be completed. File already exists in sdfs");
        } else {
            boolean written=false;
            for (int i=1;i<reply.messageParams.length;i++) {
                written|=sendFile(Pid.getPid(reply.messageParams[i]),srcfname,sdfsfname);
            }

            if (!written)
                System.out.println("Put operation cannot be completed. " +
                        "All file servers rejected taking the file");
        }
    }

    private static boolean sendFile(Pid fileServer, String srcfname,String sdfsfname) {
        try {
            Socket sock = new Socket(fileServer.hostname, fileServer.port+FSPortDelta);
            Scanner in = new Scanner(new InputStreamReader(sock.getInputStream()));
            in.useDelimiter("\n");
            PrintWriter out = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
            out.println(Message.createPutMessage(sdfsfname));
            out.flush();
            Message reply = Message.retrieveMessage(in.next());
            if (reply.type.equals(MessageType.NO))
                return false;

            DataOutputStream fout = new DataOutputStream(sock.getOutputStream());
            byte[] buffer = new byte[1024];
            FileInputStream fileIn = new FileInputStream(srcfname);
            int readlen;
            while ((readlen = fileIn.read(buffer)) != -1) {
                fout.write(buffer, 0, readlen);
            }

            fout.flush();
            fileIn.close();
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
