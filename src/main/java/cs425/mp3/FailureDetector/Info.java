package cs425.mp3.FailureDetector;

import java.io.IOException;


/**
 * Message Info Class
 *
 */
public class Info {
    private static char PARAM_DELIM=' ';
    /**
     * Defines types of messages
     *
     */
    public enum InfoType {
        JOIN ('J'),
        LEAVE ('L'),
        FAILED ('F'),
        ALIVE ('V');

        public final char prefix;
        InfoType(char c) {
            prefix=c;
        }

        /** Get type of message
         * @param prefix
         * @return Type of message 
         * @throws IOException
         */
        public static InfoType getInfoType(char prefix) throws IOException {
            if (prefix=='J')
                return JOIN;
            else if (prefix=='L')
                return LEAVE;
            else if (prefix=='F')
                return FAILED;
            else if (prefix=='V')
                return ALIVE;
            else
                throw new IOException("Message prefix supplied is not recognized!");
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("");
        sb.append(type.prefix).append(PARAM_DELIM).append(param);
        return sb.toString();
    }

    /** Deserializes a string
     * @param infoAsString
     * @return
     * @throws IOException
     */
    public static Info fromString(String infoAsString) throws IOException {
        String [] tokens=infoAsString.split(" ");
        assert tokens.length==2 : "Expected size is 2";
        return new Info(Info.InfoType.getInfoType(tokens[0].charAt(0)),tokens[1]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Info info = (Info) o;

        if (type != info.type) return false;
        return !(param != null ? !param.equals(info.param) : info.param != null);

    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (param != null ? param.hashCode() : 0);
        return result;
    }

    public final InfoType type;
    public final String param;
    Info(InfoType type, String param) {
        this.type=type;
        this.param=param;
    }
}
