package cs425.mp3;

import java.io.IOException;
import java.util.List;

import cs425.mp3.ElectionService.ElectionService;
import cs425.mp3.FailureDetector.FailureDetector;
import cs425.mp3.FileServer.FileServer;
import cs425.mp3.MasterService.MasterService;
import cs425.mp3.MasterService.MasterServiceThread;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**Main class for SDFSServer
 * 
 */
public class sdfsserverMain {
    private static int FDport=0;
    public static int intro_port=0;
    public static String intro_address="";
    private static int SERVICE_START_DELAY=100;
    public static FailureDetector FD;
    public static ElectionService ES;
    public static MasterService MS;
    public static FileServer FS;
    public static final int FSPortDelta=2;
    public static final int ESPortDelta=1;
    public static final int MSPortDelta=3;
	/**
	 * Formats commandLine inputs and flags
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
        sdfsserverMain.FDport = Integer.parseInt(line.getOptionValue("port"));
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

    /**Setup Failure Detector, Election Service, Master Service and FileServer
     * @return
     * @throws IOException 
     */
    public static void setupServices() throws IOException {
    	FD=new FailureDetector(sdfsserverMain.FDport,sdfsserverMain.intro_address,sdfsserverMain.intro_port);
    	ES=new ElectionService(FDport+ESPortDelta);
    	MS=new MasterService(FDport+MSPortDelta);
    	FS=new FileServer(FDport+FSPortDelta);
    }
    
    /**Launches Master Service when self is Master
     * @param filenames
     */
    public static void launchMaster(List<String> filenames){
		MS.updateSelfFiles(FS.getFilesInServer());
    	MasterServiceThread MSThread= new MasterServiceThread(MS);
    	MSThread.setDaemon(true);
    	MSThread.start();
    }
	/**Main function for launching SDFSServer
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String [] args) throws IOException, InterruptedException {
		FormatCommandLineInputs(args);
		setupServices();
		//Start Failure Detector
		FailureDetectorThread FDThread = new FailureDetectorThread(FD);
		FDThread.setDaemon(true);
		FDThread.start();
		Thread.sleep(SERVICE_START_DELAY);
		//Start Election Service
		ElectionServiceThread ESThread = new ElectionServiceThread(ES);
		ESThread.setDaemon(true);
		ESThread.start();
		//Start FileServer
		FS.setDaemon(true);
		FS.start();
		//Start User input Processor
		InputProcessorThread InputThread = new InputProcessorThread();
		InputThread.setDaemon(true);
		InputThread.start();
		//Wait for Failure Detector
		FDThread.join();
	}
}
