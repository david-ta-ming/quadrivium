package net.noisynarwhal.quadrivium;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    
    /** File to persist the best state found during search */
    private final File stateFile;
    
    /** CompletableFuture that will be completed with the first valid solution */
    private final CompletableFuture<MagicSquare> firstSolution;
    
    /** Shared reference to the current best square across all workers */
    private final AtomicReference<MagicSquare> bestSquare;
    
    /** Number of iterations between checks for better solutions from other workers */
    private static final long SHARING_FREQUENCY = 500000L;
    
    /** Lock for synchronizing updates to the best square */
    private static final Lock LOCK = new ReentrantLock();

    /**
     * Constructor for MagicSquareWorker
     *
     * @param firstSolution CompletableFuture that will be completed with the first valid solution
     * @param bestSquare Shared atomic reference to the current best square
     * @param stateFile File to persist the best state found during search
     */
    private MagicSquareWorker(CompletableFuture<MagicSquare> firstSolution, AtomicReference<MagicSquare> bestSquare, File stateFile) {
        this.firstSolution = firstSolution;
        this.bestSquare = bestSquare;
        this.stateFile = stateFile;
    }

    /**
     * Executes the magic square search algorithm.
     * Continuously evolves the magic square until either a valid solution is found
     * or another thread has found a solution.
     * Periodically checks for better solutions from other workers and adopts them if found.
     *
     * @return A MagicSquare object, which may or may not be a valid magic square
     * @throws IOException if there is an error saving the state
     */
    @Override
    public MagicSquare call() throws IOException {
        // Validate initial state
        if(this.bestSquare.get() == null) {
           throw new IllegalStateException("Starting magic square is null");
        }

        MagicSquare magic = this.bestSquare.get();

        long iteration = 0;
        // Continue evolving until we find a solution or another thread does
        while (!(magic.isMagic() || this.firstSolution.isDone())) {
            // Evolve the current square
            magic = magic.evolve();

            // Check if we found a better solution
            if(magic != this.bestSquare.get() && (magic.getScore() > this.bestSquare.get().getScore())) {
               try {
                   LOCK.lock();
                   // Double-check pattern to ensure thread safety
                   if(magic != this.bestSquare.get() && (magic.getScore() > this.bestSquare.get().getScore())) {
                       this.bestSquare.set(magic);

                       final float scoreRatio = ((float) magic.getScore()) / ((float) magic.getMaxScore());
                       logger.debug("Found square with score: {}/{} ({}%)", magic.getScore(), magic.getMaxScore(), String.format("%.2f", scoreRatio * 100));

                       // Persist the best state if a file is specified
                       if (this.stateFile != null) {
                           MagicSquareState.save(magic, this.stateFile);
                       }
                   }
               } finally {
                   LOCK.unlock();
               }
            }

            // Periodically check for better solutions from other workers
            if (iteration % SHARING_FREQUENCY == 0) {
                final MagicSquare currentBest = this.bestSquare.get();
                if (currentBest.getScore() > magic.getScore()) {
                    // Adopt the better solution and continue evolving from there
                    magic = MagicSquare.build(currentBest.getValues());
                }
            }

            iteration++;
        }

        // If we found a solution, complete the future
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
     * @return The updated best square
     */
    private MagicSquare updateBestSquare(final MagicSquare current) {
        return this.bestSquare.updateAndGet(existing -> (existing == null || current.getScore() > existing.getScore()) ? current : existing);
    }

    /**
     * Search for a magic square of the given order using the specified number of threads.
     * This is a convenience method that calls the main search method without state persistence.
     *
     * @param order The order of the magic square (size of the square grid)
     * @param numThreads The number of threads to use for parallel search
     * @return A valid magic square if found
     * @throws RuntimeException if there is an error loading the magic square
     */
    public static MagicSquare search(int order, int numThreads) {
        try {
            return MagicSquareWorker.search(order, numThreads, null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load magic square", e);
        }
    }

    /**
     * Search for a magic square of the given order using the specified number of threads.
     * This method creates a thread pool and distributes the search work across multiple threads.
     * Workers share their best solutions to improve overall efficiency.
     * The search stops as soon as any thread finds a valid magic square.
     *
     * @param order The order of the magic square (size of the square grid)
     * @param numThreads The number of threads to use for parallel search
     * @param stateFile Optional file to persist and load the best state found
     * @return A valid magic square if found, otherwise null
     * @throws IllegalArgumentException if order is less than 3 or numThreads is less than or equal to 0
     * @throws IOException if there is an error loading or saving the state
     */
    public static MagicSquare search(int order, int numThreads, File stateFile) throws IOException {
        // Validate input parameters
        if (order < 3) {
            throw new IllegalArgumentException("Order must be at least 3");
        }

        if (numThreads <= 0) {
            throw new IllegalArgumentException("Number of threads must be greater than 0");
        }

        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final CompletableFuture<MagicSquare> firstSolution = new CompletableFuture<>();

        // Initialize starting state from file or create new
        final MagicSquare start;
        if(stateFile != null && stateFile.canRead() && stateFile.length() > 0) {
            start = MagicSquareState.load(stateFile);
        } else {
            start = MagicSquare.build(order);
        }

        final AtomicReference<MagicSquare> bestSquare = new AtomicReference<>(start);

        try {
            List<CompletableFuture<MagicSquare>> futures = new ArrayList<>();

            // Create and start worker threads
            for (int i = 0; i < numThreads; i++) {
                final Callable<MagicSquare> worker = new MagicSquareWorker(firstSolution, bestSquare, stateFile);
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

            return firstSolution.getNow(null);

        } finally {
            executor.shutdown();
        }
    }
}