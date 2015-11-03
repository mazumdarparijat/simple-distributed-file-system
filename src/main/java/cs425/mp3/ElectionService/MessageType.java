package cs425.mp3.ElectionService;

import java.io.IOException;

/**
 * Annum for Type of messages
 */
public enum MessageType {
    MASTER ('M'),
    REPLY ('R'),
    COORDINATOR('C'),
	OK('K');
    private final char messagePrefix;
    MessageType(char p) {
        messagePrefix =p;
    }

    /** Get message prefix from message
     * @return
     */
    public char getMessagePrefix() {
        return messagePrefix;
    }

    /** Get message type from message
     * @param prefix
     * @return
     * @throws IOException
     */
    public static MessageType getMessageType(char prefix) throws IOException {
        if (prefix=='M')
            return MASTER;
        else if (prefix=='R')
            return REPLY;
        else if (prefix=='C')
            return COORDINATOR;
        else if (prefix=='K')
            return OK;
        else
            throw new IOException("Message prefix supplied is not recognized!");
    }
}
