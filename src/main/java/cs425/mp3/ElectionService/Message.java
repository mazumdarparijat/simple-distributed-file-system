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
        public static Message buildReplyMessage(String masterID) {
        	String [] args = new String[1];
        	args[0]=masterID;
        	return new Message(MessageType.REPLY,args);
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
    }
}
