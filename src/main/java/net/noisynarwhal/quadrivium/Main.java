package net.noisynarwhal.quadrivium;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
            int numThreads = requestedThreads;
            if (numThreads <= 0) {
                numThreads = Math.max(3, Runtime.getRuntime().availableProcessors() / 2);
                logger.debug("Auto-detected thread count: {}", numThreads);
            }

            logger.info("Starting magic square search with order={}, threads={}", order, numThreads);

            final List<Future<MagicSquare>> futures = new ArrayList<>();
            final ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            final long start = System.nanoTime();

            try {

                final AtomicBoolean solutionFound = new AtomicBoolean(false);

                for (int i = 0; i < numThreads; i++) {
                    final Callable<MagicSquare> worker = new MagicSquareWorker(order, solutionFound);
                    final Future<MagicSquare> future = executor.submit(worker);
                    futures.add(future);
                }
            } finally {
                executor.shutdown();
            }

            for (final Future<MagicSquare> future : futures) {
                try {
                    final MagicSquare magic = future.get();
                    if (magic.isMagic()) {
                        String result = MatrixUtils.print(magic.getValues());
                        logger.info("Found magic square solution:\n{}", result);
                        break;
                    }
                } catch (Exception e) {
                    logger.error("Error processing worker result", e);
                }
            }

            final long secs = TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            logger.info("Search completed in {} seconds", secs);

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