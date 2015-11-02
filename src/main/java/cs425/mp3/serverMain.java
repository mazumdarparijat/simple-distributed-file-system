package cs425.mp3;

import java.io.IOException;

import cs425.mp3.FailureDetector.FDIntroducer;
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
public class serverMain {
    private static int port=0;
    private static int intro_port=0;
    private static String intro_address="";
    private static boolean isIntroducer=false;
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
		if (!line.hasOption("i")) {	
			serverMain.port = Integer.parseInt(line.getOptionValue("port"));
			isIntroducer=true;
		}
		else {
            isIntroducer=false;
			serverMain.port = Integer.parseInt(line.getOptionValue("port"));
			serverMain.intro_address=line.getOptionValues("i")[0];
			serverMain.intro_port=Integer.parseInt(line.getOptionValues("i")[1]);
		}
	}

	/** Creates the required options to look for in command line arguments
	 * @return Options object
	 */
	private static Options createOptions() {
		Option port = Option.builder("port").argName("serverPort").hasArg().desc("Port to run failure detector server")
				.required().build();
		Option i = Option.builder("i").desc("Describes the address and port of introducer").numberOfArgs(2).build();
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
    private static FailureDetector startNode() {
        if (serverMain.isIntroducer)
            return new FDIntroducer(serverMain.port);
        else
            return new FailureDetector(serverMain.port,serverMain.intro_address,serverMain.intro_port);
    }

	public static void main(String [] args) throws IOException {
		FormatCommandLineInputs(args);
        boolean restart=serverMain.startNode().startFD();
        while (restart) {
            restart=serverMain.startNode().startFD();
        }
	}
}
