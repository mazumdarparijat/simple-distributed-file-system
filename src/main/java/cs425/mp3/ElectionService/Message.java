package cs425.mp3.ElectionService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class Message
 */
public class Message {
    private final char PARAM_DELIM=' ';
    public final MessageType type;
    public final String [] messageParams;
    /**
     * Fields of Parameters
     *
     */
    private Message (MessageType type, String [] params) {
        this.type=type;
        messageParams=params;
    }

    /** Gets bytes from message
     * @return bytearray of the message
     */
    public String toString() {
        StringBuilder builder=new StringBuilder();
        builder.append(type.getMessagePrefix());
        for (String param : messageParams)
            builder.append(PARAM_DELIM).append(param);
        String ret = builder.toString();
        return ret;
    }

    /** Extract mesage info
     * @param messageBytes
     * @param byteLen
     * @return
     */
    public static Message extractMessage(String msg) {
        String[] mStr=msg.split(" ");
        MessageType type = null;
        try {
            type=MessageType.getMessageType(mStr[0].charAt(0));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        String [] params = new String[mStr.length-1];
        for (int i=1;i<params.length+1;i++)
            params[i-1]=mStr[i];

        Message ret=new Message(type,params);
        return ret;
    }
    public static class MessageBuilder {
        public static Message buildMasterMessage(String senderId) {
        	String [] args = new String[1];
        	args[0]=senderId;
        	return new Message(MessageType.MASTER,args);
        }
        public static Message buildMasterReplyMessage(String masterID) {
        	String [] args = new String[1];
        	args[0]=masterID;
        	return new Message(MessageType.MASTER_REPLY,args);
        }
        public static Message buildCoordMessage(String masterID) {
        	String [] args = new String[1];
        	args[0]=masterID;
        	return new Message(MessageType.COORDINATOR,args);
        }
        public static Message buildOKMessage(String senderID) {
        	String [] args = new String[1];
        	args[0]=senderID;
        	return new Message(MessageType.OK,args);
        }
        public static Message buildPutMessage(String filename) {
        	String [] args = new String[1];
        	args[0]=filename;
        	return new Message(MessageType.PUT,args);
        }
        public static Message buildPutReplyMessage(String validity, String [] servers) {
        	String [] args = new String[1+servers.length];
        	args[0]=validity;
        	for(int i=1;i<args.length;i++){args[i]=servers[i-1];}
        	return new Message(MessageType.PUT_REPLY,args);
        }
        public static Message buildGetMessage(String filename) {
        	String [] args = new String[1];
        	args[0]=filename;
        	return new Message(MessageType.GET,args);
        }
        public static Message buildGetReplyMessage(String validity, String[] servers ) {
        	String [] args = new String[1+servers.length];
        	args[0]=validity;
        	for(int i=1;i<args.length;i++){args[i]=servers[i-1];}
        	return new Message(MessageType.GET_REPLY,args);
        }
        public static Message buildDeleteMessage(String filename) {
        	String [] args = new String[1];
        	args[0]=filename;
        	return new Message(MessageType.DELETE,args);
        }
        public static Message buildDeleteReplyMessage(String validity) {
        	String [] args = new String[1];
        	args[0]=validity;
        	return new Message(MessageType.DELETE_REPLY,args);
        }
        public static Message buildNewfilesMessage(String serverID, String[] files) {
        	String [] args = new String[1+files.length];
        	args[0]=serverID;
        	for(int i=1;i<args.length;i++){args[i]=files[i-1];}
        	return new Message(MessageType.NEWFILES,args);
        }
        public static Message buildListMessage(String senderID) {
        	String [] args = new String[1];
        	args[0]=senderID;
        	return new Message(MessageType.LIST,args);
        }
        public static Message buildListReplyMessage(String senderID, String[] files) {
        	String [] args = new String[1+files.length];
        	args[0]=senderID;
        	for(int i=1;i<args.length;i++){args[i]=files[i-1];}
        	return new Message(MessageType.LIST_REPLY,args);
        }
    }
}
