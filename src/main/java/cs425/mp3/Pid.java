package cs425.mp3;

/**
 * PID class
 */
public class Pid {
	public final String hostname;
	public final int port;
	public final long timestamp;
	public final String pidStr;

    /** Constructor
     * @param hostname
     * @param port
     * @param timestamp
     */
    public Pid(String hostname, int port, long timestamp) {
        this.port = port;
        this.timestamp = timestamp;
        this.hostname = hostname;

        StringBuilder sb=new StringBuilder();
        sb.append(hostname).append('_').append(port)
                .append('_').append(timestamp);
        pidStr=sb.toString();
    }

    /** Get pid from string
     * @param pidString
     * @return
     */
    public static Pid getPid(String pidString) {
        String [] tokens = pidString.split("_");
        return new Pid(tokens[0],Integer.parseInt(tokens[1]),Long.parseLong(tokens[2]));
    }
}
