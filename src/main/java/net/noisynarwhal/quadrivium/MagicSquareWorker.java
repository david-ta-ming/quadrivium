package net.noisynarwhal.quadrivium;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Worker class for MagicSquare that implements Callable to search for magic squares in parallel.
 * A magic square is a square grid filled with distinct numbers such that the numbers in each row,
 * each column, and both main diagonals all add up to the same value.
 * This implementation includes a shared best solution mechanism that allows workers to
 * periodically check for and adopt better solutions found by other workers.
 */
public class MagicSquareWorker implements Callable<MagicSquare> {
    /** Logger instance for this class */
    private static final Logger logger = LoggerFactory.getLogger(MagicSquareWorker.class);
    /** The order of the magic square (size of the square grid) */
    private final int order;
    /** CompletableFuture that will be completed with the first valid solution */
    private final CompletableFuture<MagicSquare> firstSolution;
    /** Shared reference to the current best square across all workers */
    private final AtomicReference<MagicSquare> bestSquare;
    /** Number of iterations between checks for better solutions */
    private static final long SHARING_FREQUENCY = 500000L;

    /**
     * Constructor for MagicSquareWorker
     *
     * @param order The order of the magic square (size of the square grid)
     * @param firstSolution CompletableFuture that will be completed with the first valid solution
     * @param bestSquare Shared atomic reference to the current best square
     */
    private MagicSquareWorker(int order, CompletableFuture<MagicSquare> firstSolution,
                              AtomicReference<MagicSquare> bestSquare) {
        this.order = order;
        this.firstSolution = firstSolution;
        this.bestSquare = bestSquare;
    }

    /**
     * Executes the magic square search algorithm.
     * Continuously evolves the magic square until either a valid solution is found
     * or another thread has found a solution.
     * Periodically checks for better solutions from other workers and adopts them if found.
     *
     * @return A MagicSquare object, which may or may not be a valid magic square
     */
    @Override
    public MagicSquare call() {
        // Start with a random magic square
        MagicSquare magic = MagicSquare.build(this.order);

        // Initialize the best square with our starting point if no better one exists
        updateBestSquare(magic);

        long iteration = 0;
        while (!(magic.isMagic() || this.firstSolution.isDone())) {
            // Evolve the current square
            magic = magic.evolve();

            // Update the best square if this one is better
            if (magic.getScore() > bestSquare.get().getScore()) {
                updateBestSquare(magic);

                final float scoreRatio = ((float) magic.getScore()) / ((float) magic.getMaxScore());
                // Log when we find a significantly better square
                logger.debug("Found square with score: {}/{} ({}%)",
                        magic.getScore(), magic.getMaxScore(), String.format("%.2f", scoreRatio * 100));
            }

            // Periodically check if another worker has found a better solution
            if (iteration % SHARING_FREQUENCY == 0) {
                MagicSquare currentBest = bestSquare.get();

                // If another worker has a better solution, adopt it
                if (currentBest.getScore() > magic.getScore()) {
                    // Clone the best square and continue evolving from there
                    magic = MagicSquare.build(currentBest.getValues());
                }
            }

            // Log progress periodically
            iteration++;
        }

        if (magic.isMagic()) {
            this.firstSolution.complete(magic);
            logger.info("Found magic square solution!");
        }

        return magic;
    }

    /**
     * Updates the shared best square if the current square has a better score.
     * Uses atomic operations to ensure thread safety.
     *
     * @param current The current square to consider
     */
    private void updateBestSquare(MagicSquare current) {
        bestSquare.updateAndGet(existing ->
                (existing == null || current.getScore() > existing.getScore()) ? current : existing);
    }

    /**
     * Search for a magic square of the given order using the specified number of threads.
     * This method creates a thread pool and distributes the search work across multiple threads.
     * Workers share their best solutions to improve overall efficiency.
     * The search stops as soon as any thread finds a valid magic square.
     *
     * @param order The order of the magic square (size of the square grid)
     * @param numThreads The number of threads to use for parallel search
     * @return A valid magic square if found, otherwise null
     * @throws IllegalArgumentException if order is less than 3 or numThreads is less than or equal to 0
     */
    public static MagicSquare search(int order, int numThreads) {
        if (order < 3) {
            throw new IllegalArgumentException("Order must be at least 3");
        }

        if (numThreads <= 0) {
            throw new IllegalArgumentException("Number of threads must be greater than 0");
        }

        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final CompletableFuture<MagicSquare> firstSolution = new CompletableFuture<>();
        final AtomicReference<MagicSquare> bestSquare = new AtomicReference<>(null);

        try {
            List<CompletableFuture<MagicSquare>> futures = new ArrayList<>();

            for (int i = 0; i < numThreads; i++) {
                final Callable<MagicSquare> worker = new MagicSquareWorker(order, firstSolution, bestSquare);
                final CompletableFuture<MagicSquare> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return worker.call();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, executor);
                futures.add(future);
            }

            // Wait for either the first solution or all threads to complete
            CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0])).join();

            // Get the first solution if it exists
            return firstSolution.getNow(null);

        } finally {
            executor.shutdown();
        }
    }
}