package cs425.mp3.FailureDetector;

import java.io.IOException;

/**
 * Annum for Type of messages
 */
public enum MessageType {
    PING ('P'),
    PING_REQUEST ('Q'),
    ACK ('A'),
    ACK_REQUEST ('B'),
    MISSING_NOTICE ('M'),
    END ('E');

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
        if (prefix=='P')
            return PING;
        else if (prefix=='Q')
            return PING_REQUEST;
        else if (prefix=='A')
            return ACK;
        else if (prefix=='B')
            return ACK_REQUEST;
        else if (prefix=='M')
            return MISSING_NOTICE;
        else if (prefix=='E')
            return END;
        else
            throw new IOException("Message prefix supplied is not recognized!");
    }
}
