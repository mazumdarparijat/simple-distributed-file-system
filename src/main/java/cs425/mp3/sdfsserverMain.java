package cs425.mp3;

import java.io.IOException;

import cs425.mp3.FailureDetector.FailureDetector;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**Main class for FD
 * 
 */
public class sdfsserverMain {
    private static int port=0;
    private static int intro_port=0;
    private static String intro_address="";
    private static boolean isIntroducer=false;
    public static FailureDetector FD;
    public static SDFSServer FileServer;
	/**
	 * Formats commandline inputs and flags
	 */
	private static void FormatCommandLineInputs(String [] args) {
		Options op=createOptions();
		CommandLineParser parser=new DefaultParser();
		CommandLine line=null;
		try {
			line=parser.parse(op,args);	
		} catch (ParseException e) {
			printHelp(op);
			e.printStackTrace();
		}
        isIntroducer=false;
		sdfsserverMain.port = Integer.parseInt(line.getOptionValue("port"));
		sdfsserverMain.intro_address=line.getOptionValues("i")[0];
		sdfsserverMain.intro_port=Integer.parseInt(line.getOptionValues("i")[1]);
	}
	/** Creates the required options to look for in command line arguments
	 * @return Options object
	 */
	private static Options createOptions() {
		Option port = Option.builder("port").argName("serverPort").hasArg().desc("Port to run faliure detector server")
				.required().build();
		Option i = Option.builder("i").desc("Describes the address and port of introducer").numberOfArgs(2).required().build();
		Options op=new Options();
		op.addOption(port);
		op.addOption(i);
		return op;
	}

	/** print helper for usage
	 * @param op options
	 */
	private static void printHelp(Options op) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("failureDetector", op);
	}

    /**Start FD module
     * @return
     */
    public static void setupServices() {
    	FD=new FailureDetector(sdfsserverMain.port,sdfsserverMain.intro_address,sdfsserverMain.intro_port);
    	FileServer = new SDFSServer();
    }

	public static void main(String [] args) throws IOException, InterruptedException {
		FormatCommandLineInputs(args);
		setupServices();
		//Start Faliure Detector
		FailureDetectorThread FDThread = new FailureDetectorThread();
		FDThread.setDaemon(true);
		FDThread.start();
		//Start HDFS server Object
		SDFSServerThread FSThread = new SDFSServerThread();
		FSThread.setDaemon(true);
		FSThread.start();
		
		//Wait for Failure Detector
		FDThread.join();
	}
}
