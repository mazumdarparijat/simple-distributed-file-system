package cs425.mp3.FileServer;

import java.io.IOException;
import java.io.Serializable;

/**
 * Class Message
 */
public class Message implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String EOF="$$$";
    private static final String ARG_SEPARATOR=" ";
    public final MessageType type;
    protected final String fileName;
    protected final String ipAddress;
    protected final int port;

    private Message(MessageType t) {
        type=t;
        fileName="";
        ipAddress="";
        port=0;
    }

    private Message(MessageType t, String filename) {
        type=t;
        fileName=filename;
        ipAddress="";
        port=0;
    }

    private Message(MessageType t, String filename, String ipAddress, int port) {
        type=t;
        fileName=filename;
        this.ipAddress=ipAddress;
        this.port=port;
    }

    @Override
    /** Gets String from message
     * @return String of the message
     */
    public String toString() {
        return type.toString()+ARG_SEPARATOR+fileName+ARG_SEPARATOR+ipAddress+ARG_SEPARATOR+String.valueOf(port);
    }

    public static Message createGetMessage(String filename) {
        return new Message(MessageType.GET,filename);
    }

    public static Message createPutMessage(String filename) {
        return new Message(MessageType.PUT,filename);
    }

    public static Message createDelMessage(String filename) {
        return new Message(MessageType.DEL, filename);
    }

    public static Message createReplicateMessage(String filename, String ipAddress, int port) {
        return new Message(MessageType.REP,filename,ipAddress,port);
    }

    public static Message createOkayMessage() {
        return new Message(MessageType.YES);
    }

    public static Message createNayMessage() {
        return new Message(MessageType.NO);
    }
    /** Extract message info
     * @param msg
     * @return
     */
    public static Message retrieveMessage(String message) throws IOException {
        String [] args=message.split(ARG_SEPARATOR);
        return new Message(MessageType.fromString(args[0]),args[1],args[2],Integer.parseInt(args[3]));
    }
}
