package net.noisynarwhal.quadrivium;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Main class for Quadrivium
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String DEFAULT_ORDER = "30";
    private static final String DEFAULT_THREADS = "0"; // 0 means auto-detect

    public static void main(String[] args) {
        final Options options = createOptions();
        final CommandLineParser parser = new DefaultParser();

        try {
            logger.info("Starting Quadrivium with arguments: {}", String.join(" ", args));
            final CommandLine cmd = parser.parse(options, args);

            // Show help and exit
            if (cmd.hasOption('h')) {
                printHelp(options);
                return;
            }

            // Parse options with defaults
            int order = Integer.parseInt(cmd.getOptionValue('o', DEFAULT_ORDER));
            int requestedThreads = Integer.parseInt(cmd.getOptionValue('t', DEFAULT_THREADS));

            // Validate order
            if (order < 3) {
                throw new ParseException("Order must be at least 3");
            }

            // Calculate number of threads
            final int numThreads;
            if (requestedThreads <= 0) {
                numThreads = Math.max(3, Runtime.getRuntime().availableProcessors() / 2);
                logger.debug("Auto-detected thread count: {}", numThreads);
            } else {
                numThreads = requestedThreads;
            }

            logger.info("Starting magic square search with order={}, threads={}", order, numThreads);

            final long start = System.nanoTime();

            final MagicSquare magic = MagicSquareWorker.search(order, numThreads);

            if(magic != null) {
                final String result = MatrixUtils.print(magic.getValues());
                logger.info("Found magic square solution:\n{}", result);
            } else {
                logger.info("No magic square solution found");
            }

            final long durationNanos = System.nanoTime() - start;
            final long durationMillis = TimeUnit.MILLISECONDS.convert(durationNanos, TimeUnit.NANOSECONDS);
            final long durationSeconds = TimeUnit.SECONDS.convert(durationNanos, TimeUnit.NANOSECONDS);
            final long durationMinutes = TimeUnit.MINUTES.convert(durationNanos, TimeUnit.NANOSECONDS);
            final long durationHours = TimeUnit.HOURS.convert(durationNanos, TimeUnit.NANOSECONDS);

            if (durationSeconds <= 1) {
                logger.info("Search completed in {} milliseconds", durationMillis);
            } else if (durationMinutes <= 3) {
                logger.info("Search completed in {} seconds", durationSeconds);
            } else if (durationHours <= 6) {
                logger.info("Search completed in {} minutes", durationMinutes);
            } else {
                logger.info("Search completed in {} hours", durationHours);
            }

        } catch (ParseException e) {
            logger.error("Failed to parse command line arguments", e);
            printHelp(options);
            System.exit(1);
        }
    }

    /**
     * Creates and configures command line options
     */
    private static Options createOptions() {
        Options options = new Options();

        options.addOption(Option.builder("o")
                .longOpt("order")
                .desc("Order of the magic square (default: " + DEFAULT_ORDER + ")")
                .hasArg()
                .type(Number.class)
                .build());

        options.addOption(Option.builder("t")
                .longOpt("threads")
                .desc("Number of threads to use (default: auto-detect)")
                .hasArg()
                .type(Number.class)
                .build());

        options.addOption("h", "help", false, "Print this help message");

        return options;
    }

    /**
     * Prints usage information
     */
    private static void printHelp(Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar quadrivium.jar",
                "\nFinds magic squares using parallel evolution.\n\n",
                options,
                "\nExample: java -jar quadrivium.jar --order 4 --threads 8\n",
                true);
    }
}