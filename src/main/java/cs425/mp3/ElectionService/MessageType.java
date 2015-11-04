package cs425.mp3.ElectionService;

import java.io.IOException;

/**
 * Annum for Type of messages
 */
public enum MessageType {
    MASTER ('M'),
    MASTER_REPLY ('R'),
    COORDINATOR('C'),
	OK('K'),
	PUT('P'),
	PUT_REPLY('U'),
	GET('G'),
	GET_REPLY('E'),
	DELETE('D'),
	DELETE_REPLY('L'),
	NEWFILES('N'),
	LIST('I'),
	LIST_REPLY('S');
	
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
            return MASTER_REPLY;
        else if (prefix=='C')
            return COORDINATOR;
        else if (prefix=='K')
            return OK;
        else if (prefix=='P')
        	return PUT;
        else if (prefix=='U')
        	return PUT_REPLY;
        else if (prefix=='G')
        	return GET;
        else if (prefix=='E')
        	return GET_REPLY;
        else if (prefix=='D')
        	return DELETE;
        else if (prefix=='L')
        	return DELETE_REPLY;
        else if (prefix=='N')
        	return NEWFILES;
        else if (prefix=='I')
        	return LIST;
        else if (prefix=='S')
        	return LIST_REPLY;
        else
            throw new IOException("Message prefix supplied is not recognized!");
    }
}
