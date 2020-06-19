import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * This program generates instruments from a specified interval, start date, and number of days.
 * 
 * <pre>
 * Usage: instrumentGen [-f filepath] -s | -c START_DAY INTERVAL_MINS NUM_DAYS
 * </pre>
 * 
 * <p>
 * Options:
 * <ul>
 * <li>{@code -f} Path to output generated instruments. If the file in the path exists then it is
 * appended to, otherwise it is created</li>
 * <li>{@code -s} Outputs instruments in parity-system format</li>
 * <li>{@code -c} Outputs instruments in parity-client format</li>
 * </ul>
 */
public class ConfigurationGenerator {

	private static final int MINUTES_IN_DAY = 1440;

	public static void main(String[] args) {
		final String USAGE =
				"Usage: instruments.jar [--help] [-f filepath] -s | -c START_DAY INTERVAL_MINS NUM_DAYS\n"
						+ "Generate instruments in parity system/client configuration format\n"
						+ "Example: java -jar -f sys.conf -s 20200704 60 1";
		final String OPTIONS = "Options:\n"
				+ "-f      Path to output generated instruments. If the file in the path exists\n"
				+ "          then it is appended to, otherwise it is created. If this option is\n"
				+ "          not provided the output will be sent to stdout\n"
				+ "-s      Outputs instruments in parity-system configuration format\n"
				+ "-c      Outputs instruments in parity-client configuration format";

		final int MIN_ARGS = 4;
		if (args.length < MIN_ARGS) {
			System.err.println(USAGE);
			if (args.length == 1 && args[0].equals("--help")) {
				System.err.printf("%n%s", OPTIONS);
			}
			System.exit(0);
		}

		String filepath = null;
		boolean sysFlag = false;
		boolean clientFlag = false;

		// Parse options
		int argPos = 0;
		while (argPos < args.length && args[argPos].startsWith("-")) {
			String arg = args[argPos++];
			switch (arg) {
				case "-f":
					if (argPos < args.length) {
						filepath = args[argPos++];
					} else {
						System.err.println("-f requires filename");
					}
					break;
				case "-s":
					sysFlag = true;
					break;
				case "-c":
					clientFlag = true;
					break;
				default:
					System.err.println(USAGE);
					System.err.println(arg.concat(" is not a valid option"));
					System.exit(1);
					break;
			}
		}
		if (argPos + 2 >= args.length) {
			System.err.println(USAGE);
			System.err.println("Missing arguments");
			System.exit(1);
		} else if ((sysFlag && clientFlag) || (!sysFlag && !clientFlag)) {
			System.err.println(USAGE);
			System.err.println("Can only output one type of configuration at a time");
			System.err.println("Must choose system OR client configuration output");
			System.exit(1);
		}

		// Parse start date
		final DateTimeFormatter startFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
		LocalDateTime startDate = null;
		try {
			startDate = LocalDateTime.from(startFormatter.parse(args[argPos++] + "0000"));
		} catch (DateTimeParseException e) {
			System.err.printf("Incorrect START_DAY argument%nUse the format: yyyyMMdd");
			System.exit(1);
		}

		// Parse interval
		int interval = Integer.parseInt(args[argPos++]);
		if (interval > MINUTES_IN_DAY) {
			System.err.println(USAGE);
			System.err.println("Cannot use an interval over a day"); // Not sure if this is the spec
			System.exit(1);
		}

		long numDays = Long.parseLong(args[argPos]);
		List<LocalDateTime> instrumentList = new ArrayList<>();
		populateInstruments(instrumentList, startDate, interval, numDays);

		StringBuilder output = new StringBuilder();
		formatOutput(instrumentList, output, sysFlag);

		writeOutput(filepath, output.toString());
	}

	/**
	 * Adds instruments to the {@code instrumentList} incrementing by minutes in interval and number
	 * of days.
	 * 
	 * @param instrumentList list to add instruments to
	 * @param startDate      start date of intervals
	 * @param interval       number of minutes between intervals
	 * @param numDays        days that intervals should span
	 */
	private static void populateInstruments(List<LocalDateTime> instrumentList,
			LocalDateTime startDate, int interval, long numDays) {
		for (int i = 0; i < numDays; i++) {
			for (int j = 0; j < (MINUTES_IN_DAY / interval); j += 1) {
				instrumentList.add(startDate);
				startDate = startDate.plusMinutes(interval);
			}
		}
	}

	/**
	 * Formats instruments in {@code instrumentList} into the string {@code output}.
	 * 
	 * @param instrumentList list of instruments in interval form
	 * @param output         compilation of instruments
	 * @param isSysConf      boolean for which format to use
	 */
	private static void formatOutput(List<LocalDateTime> instrumentList, StringBuilder output,
			boolean isSysConf) {
		final DateTimeFormatter intervalFormatter = DateTimeFormatter.ofPattern("MMddHHmm");

		final String CLIENT_PREAMBLE =
				"order-entry {\n\taddress  = 127.0.0.1\n\tport     = 4000\n\tusername = parity\n\tpassword = parity\n}\n\n";
		final String SYSTEM_PREAMBLE =
				"market-data {\n\tsession             = parity\n\tmulticast-interface = 127.0.0.1\n\tmulticast-group     = 224.0.0.1\n\tmulticast-port      = 5000\n\trequest-address     = 127.0.0.1\n\trequest-port        = 5001\n}\n\nmarket-report {\n\tsession             = parity\n\tmulticast-interface = 127.0.0.1\n\tmulticast-group     = 224.0.0.1\n\tmulticast-port      = 6000\n\trequest-address     = 127.0.0.1\n\trequest-port        = 6001\n}\n\norder-entry {\n\taddress = 127.0.0.1\n\tport    = 4000\n}\n\n";

		if (isSysConf) {
			output.append(CLIENT_PREAMBLE);
			output.append("instruments = [\n");
			final String sysFormat = "\t%s%n";
			for (LocalDateTime date : instrumentList) {
				output.append(String.format(sysFormat, date.format(intervalFormatter)));
			}
			output.append("]\n");

		} else {
			output.append(SYSTEM_PREAMBLE);
			output.append(
					"instruments = {\n\tprice-integer-digits = 8\n\tsize-integer-digits  = 8\n\n");

			final String clientFormat =
					"\t%s {%n\t\tprice-fraction-digits = 3%n\t\tsize-fraction-digits  = 0%n\t}%n";
			for (LocalDateTime date : instrumentList) {
				output.append(String.format(clientFormat, date.format(intervalFormatter)));
			}
			output.append("}\n");
		}
	}

	/**
	 * Appends {@code output} to the {@code filepath} location. If the path does not exist then the
	 * file will be created. If {@code filepath} has a value of {@code null} then the output is
	 * written to stdout
	 * 
	 * @param filepath path to write to
	 * @param output   formatted instruments
	 */
	private static void writeOutput(String filepath, String output) {
		if (filepath != null) {
			File file = new File(filepath);
			if (file.exists()) {
				System.out.println("Appending to file");
			} else {
				System.out.println("Creating ".concat(filepath));
			}

			try (FileWriter fr = new FileWriter(file, true)) {
				fr.write(output);
			} catch (FileNotFoundException e) {
				System.err.println(
						"File could not be created because a directory in the filepath is not valid");
				System.exit(1);
			} catch (IOException e) {
				System.err.println("Could not write to file");
				e.printStackTrace();
				System.exit(1);
			}
			System.out.println("Write successful");
		} else {
			System.out.print(output);
		}
	}
}
