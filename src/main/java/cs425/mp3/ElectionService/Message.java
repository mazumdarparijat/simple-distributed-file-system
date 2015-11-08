package cs425.mp3.ElectionService;
import java.io.IOException;
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

    /** Gets String from message
     * @return String of the message
     */
    public String toString() {
        StringBuilder builder=new StringBuilder();
        builder.append(type.getMessagePrefix());
        for (String param : messageParams)
            builder.append(PARAM_DELIM).append(param);
        String ret = builder.toString();
        return ret;
    }

    /** Extract message info
     * @param msg
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
    /**Class for building message
     *
     */
    public static class MessageBuilder {
        /**Builds Master Message
         * @param senderId
         * @return
         */
        public static Message buildMasterMessage(String senderId) {
        	String [] args = new String[1];
        	args[0]=senderId;
        	return new Message(MessageType.MASTER,args);
        }
        /**Builds reply to master message
         * @param masterID
         * @return
         */
        public static Message buildMasterReplyMessage(String masterID) {
        	String [] args = new String[1];
        	args[0]=masterID;
        	return new Message(MessageType.MASTER_REPLY,args);
        }
        /**Builds coordinator message 
         * @param masterID
         * @return
         */
        public static Message buildCoordMessage(String masterID) {
        	String [] args = new String[1];
        	args[0]=masterID;
        	return new Message(MessageType.COORDINATOR,args);
        }
        /**Builds ok message
         * @param senderID
         * @return
         */
        public static Message buildOKMessage(String senderID) {
        	String [] args = new String[1];
        	args[0]=senderID;
        	return new Message(MessageType.OK,args);
        }
        /**Builds put message
         * @param filename
         * @return
         */
        public static Message buildPutMessage(String filename) {
        	String [] args = new String[1];
        	args[0]=filename;
        	return new Message(MessageType.PUT,args);
        }
        /**Builds reply to put message
         * @param validity
         * @param servers
         * @return
         */
        public static Message buildPutReplyMessage(String validity, String [] servers) {
        	String [] args = new String[1+servers.length];
        	args[0]=validity;
        	for(int i=1;i<args.length;i++){args[i]=servers[i-1];}
        	return new Message(MessageType.PUT_REPLY,args);
        }
        /**Builds get message
         * @param filename
         * @return
         */
        public static Message buildGetMessage(String filename) {
        	String [] args = new String[1];
        	args[0]=filename;
        	return new Message(MessageType.GET,args);
        }
        /**Build reply to get message
         * @param validity
         * @param servers
         * @return
         */
        public static Message buildGetReplyMessage(String validity, String[] servers ) {
        	String [] args = new String[1+servers.length];
        	args[0]=validity;
        	for(int i=1;i<args.length;i++){args[i]=servers[i-1];}
        	return new Message(MessageType.GET_REPLY,args);
        }
        /**Build delete message
         * @param filename
         * @return
         */
        public static Message buildDeleteMessage(String filename) {
        	String [] args = new String[1];
        	args[0]=filename;
        	return new Message(MessageType.DELETE,args);
        }
        /**Build reply to delete message
         * @param validity
         * @return
         */
        public static Message buildDeleteReplyMessage(String validity) {
        	String [] args = new String[1];
        	args[0]=validity;
        	return new Message(MessageType.DELETE_REPLY,args);
        }
        /**Build new files message 
         * @param serverID
         * @param files
         * @return
         */
        public static Message buildNewfilesMessage(String serverID, String[] files) {
        	String [] args = new String[1+files.length];
        	args[0]=serverID;
        	for(int i=1;i<args.length;i++){args[i]=files[i-1];}
        	return new Message(MessageType.NEWFILES,args);
        }
        /**Builds list message
         * @param senderID
         * @return
         */
        public static Message buildListMessage(String senderID) {
        	String [] args = new String[1];
        	args[0]=senderID;
        	return new Message(MessageType.LIST,args);
        }
        /**Build reply to list message
         * @param senderID
         * @param files
         * @return
         */
        public static Message buildListReplyMessage(String senderID, String[] files) {
        	String [] args = new String[1+files.length];
        	args[0]=senderID;
        	for(int i=1;i<args.length;i++){args[i]=files[i-1];}
        	return new Message(MessageType.LIST_REPLY,args);
        }
    }
}
