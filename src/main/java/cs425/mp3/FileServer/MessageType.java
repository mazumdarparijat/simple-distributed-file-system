package cs425.mp3.FileServer;

import java.io.IOException;
import java.io.Serializable;

/**
 * Annum for Type of messages
 */
public enum MessageType implements Serializable {
    GET ("G"),
    PUT ("P"),
    REP ("R"),
    YES ("Y"),
    DEL ("D"),
    NO ("N");

    public final String messagePrefix;
    MessageType(String n) {
        messagePrefix=n;
    }

    @Override
    public String toString() {
        return messagePrefix;
    }
    /** Get message type from message
     * @param prefix
     * @return
     * @throws IOException
     */
    public static MessageType fromString(String string) throws IOException {
        if (string.equals("G"))
            return MessageType.GET;
        else if (string.equals("P"))
            return MessageType.PUT;
        else if (string.equals("R"))
            return MessageType.REP;
        else if (string.equals("Y"))
            return MessageType.YES;
        else if (string.equals("D"))
            return MessageType.DEL;
        else if (string.equals("N"))
            return MessageType.NO;
        else
            throw new IOException("Type not found");
    }
}
