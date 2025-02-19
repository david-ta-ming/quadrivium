package net.noisynarwhal.quadrivium;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker class for MagicSquare
 */
public class MagicSquareWorker implements Callable<MagicSquare> {
    private static final Logger logger = LoggerFactory.getLogger(MagicSquareWorker.class);
    private final int order;
    private final AtomicBoolean solutionFound;

    /**
     * Constructor for MagicSquareWorker
     *
     * @param order The order of the magic square
     * @param solutionFound Atomic boolean to track if a solution has been found
     */
    public MagicSquareWorker(int order, AtomicBoolean solutionFound) {
        this.order = order;
        this.solutionFound = solutionFound;
    }

    @Override
    public MagicSquare call() {
        MagicSquare magic = MagicSquare.build(order);
        while (!(magic.isMagic() || solutionFound.get())) {
            magic = magic.evolve();
        }
        solutionFound.compareAndSet(false, magic.isMagic());
        return magic;
    }

    /**
     * Search for a magic square of the given order using the specified number of threads
     * @param order The order of the magic square
     * @param numThreads The number of threads to use
     * @return A magic square if found, otherwise null
     */
    public static MagicSquare search(int order, int numThreads) {

        if (order < 3) {
            throw new IllegalArgumentException("Order must be at least 3");
        }

        if (numThreads <= 0) {
            throw new IllegalArgumentException("Number of threads must be greater than 0");
        }

        final List<Future<MagicSquare>> futures = new ArrayList<>();
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);

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
                    return magic;
                }
            } catch (Exception e) {
                logger.error("Error processing worker result", e);
            }
        }

        return null;
    }
}
