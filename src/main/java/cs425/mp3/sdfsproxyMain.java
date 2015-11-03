package cs425.mp3;

import java.io.IOException;

import cs425.mp3.FailureDetector.FDIntroducer;
import cs425.mp3.ElectionService.MasterTracker;
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
public class sdfsproxyMain {
    private static int FDport=0;
    public static int intro_port=0;
    public static String intro_address="";
    private static boolean isIntroducer=false;
    public static FailureDetector FD;
    public static SDFSServer FileServer;
    public static MasterTracker MT;
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
        isIntroducer=true;
		sdfsproxyMain.FDport = Integer.parseInt(line.getOptionValue("port"));
	}
	/** Creates the required options to look for in command line arguments
	 * @return Options object
	 */
	private static Options createOptions() {
		Option port = Option.builder("port").argName("serverPort").hasArg().desc("Port to run faliure detector server")
				.required().build();
		Options op=new Options();
		op.addOption(port);
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
     * @throws IOException 
     */
    public static void setupServices() throws IOException {
    	FD=new FDIntroducer(sdfsproxyMain.FDport);
    	MT=new MasterTracker(FDport+1);
    }

	public static void main(String [] args) throws IOException, InterruptedException {
		FormatCommandLineInputs(args);
		setupServices();
		//Start Faliure Detector
		FailureDetectorThread FDThread = new FailureDetectorThread(FD);
		FDThread.setDaemon(true);
		FDThread.start();
		//Start Faliure Detector
		MasterTrackerThread MTThread = new MasterTrackerThread(MT);
		MTThread.setDaemon(true);
		MTThread.start();
		
		while(true){
			if(MT.getMaster()!=null){
				System.err.println("Master "+MT.getMaster());
			}
			else{
				System.err.println("Master Null");
			}
			Thread.sleep(1000);
		}
	}
}
