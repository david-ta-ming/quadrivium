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
 * Worker class for MagicSquare that implements a work-stealing approach to search for magic squares in parallel.
 * This implementation divides the search space into smaller tasks that can be stolen by idle workers,
 * improving CPU utilization and load balancing.
 */
public class MagicSquareWorker {
    /** Logger instance for this class */
    private static final Logger logger = LoggerFactory.getLogger(MagicSquareWorker.class);

    /** Lock for synchronizing updates to the best square */
    private static final Lock LOCK = new ReentrantLock();

    /** Number of initial populations to create (set to 4x number of threads for better work distribution) */
    private static final int POPULATION_MULTIPLIER = 4;

    /** Number of iterations each task will perform before checking for solution or splitting */
    private static final int ITERATIONS_PER_TASK = 100000;

    /** How often to save state regardless of score improvements (in milliseconds) */
    private static final long STATE_SAVE_INTERVAL_MS = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    /**
     * Search for a magic square of the given order using a work-stealing approach.
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
     * Search for a magic square of the given order using work-stealing parallelism.
     * This method creates multiple initial starting points and distributes them as tasks
     * in a ForkJoinPool, allowing idle threads to steal work from busy ones.
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

        // Create a work-stealing thread pool
        final ForkJoinPool forkJoinPool = new ForkJoinPool(numThreads);

        // Shared state: the best square found so far, and a future for the first solution
        final CompletableFuture<MagicSquare> solutionFuture = new CompletableFuture<>();
        final AtomicReference<MagicSquare> bestSquare = new AtomicReference<>(null);

        // Create a scheduler for periodic state saving
        final ScheduledExecutorService stateSaveScheduler = (stateFile != null) ? Executors.newSingleThreadScheduledExecutor() : null;

        try {
            final boolean restoreState = stateFile != null && stateFile.canRead() && stateFile.length() > 0;
            if (restoreState) {
                logger.info("Restoring state from file: {}", stateFile.getAbsolutePath());
            }

            // Initialize starting state from file or create new
            final MagicSquare initialSquare = restoreState ? MagicSquareState.load(stateFile) : MagicSquare.build(order);

            bestSquare.set(initialSquare);
            logger.info("Initial magic square with score: {}/{}", initialSquare.getScore(), initialSquare.getMaxScore());

            // Set up periodic state saving if a state file is specified
            if (stateFile != null) {
                // Schedule periodic state saving regardless of improvements
                stateSaveScheduler.scheduleAtFixedRate(() -> {
                    MagicSquare currentBest = bestSquare.get();
                    if (currentBest != null) {
                        try {
                            LOCK.lock();
                            logger.info("Periodic state save - current best score: {}/{}", currentBest.getScore(), currentBest.getMaxScore());
                            MagicSquareState.save(currentBest, stateFile);
                        } catch (IOException e) {
                            logger.error("Failed to save state during periodic save", e);
                        } finally {
                            LOCK.unlock();
                        }
                    }
                }, STATE_SAVE_INTERVAL_MS, STATE_SAVE_INTERVAL_MS, TimeUnit.MILLISECONDS);
            }

            // Create initial task
            final MagicSquareSearchTask initialTask = new MagicSquareSearchTask(
                    initialSquare, bestSquare, solutionFuture, stateFile);

            // Create and submit additional tasks with diverse starting points
            final List<MagicSquareSearchTask> additionalTasks = new ArrayList<>();
            for (int i = 1; i < numThreads * POPULATION_MULTIPLIER; i++) {
                // Create diverse starting squares for better search space coverage
                final MagicSquare startingSquare = restoreState ? MagicSquare.build(initialSquare.getValues()) : MagicSquare.build(order);
                final MagicSquareSearchTask task = new MagicSquareSearchTask(
                        startingSquare, bestSquare, solutionFuture, stateFile);
                additionalTasks.add(task);
            }

            // Submit all tasks to the ForkJoinPool
            final List<ForkJoinTask<MagicSquare>> allTasks = new ArrayList<>();
            allTasks.add(forkJoinPool.submit(initialTask));
            for (MagicSquareSearchTask task : additionalTasks) {
                allTasks.add(forkJoinPool.submit(task));
            }

            // Wait for a solution or for all tasks to complete
            MagicSquare result = null;
            try {
                // Wait for the first solution
                result = solutionFuture.get();
                logger.info("Solution found by one of the workers");

                // Save the final result
                if (result != null && result.isMagic() && stateFile != null) {
                    try {
                        LOCK.lock();
                        logger.info("Saving final solution to state file");
                        MagicSquareState.save(result, stateFile);
                    } catch (IOException e) {
                        logger.error("Failed to save final solution", e);
                    } finally {
                        LOCK.unlock();
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error waiting for solution", e);
            }

            // If no solution was found through the future, check the results of each task
            if (result == null) {
                for (ForkJoinTask<MagicSquare> task : allTasks) {
                    try {
                        MagicSquare taskResult = task.get();
                        if (taskResult != null && taskResult.isMagic()) {
                            result = taskResult;

                            // Save the final result
                            if (stateFile != null) {
                                try {
                                    LOCK.lock();
                                    logger.info("Saving final solution from task result to state file");
                                    MagicSquareState.save(result, stateFile);
                                } catch (IOException e) {
                                    logger.error("Failed to save final solution from task", e);
                                } finally {
                                    LOCK.unlock();
                                }
                            }
                            break;
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        logger.debug("Task execution failed", e);
                    }
                }
            }

            return result;
        } finally {
            // Shutdown the executor and scheduler
            forkJoinPool.shutdown();
            if (stateSaveScheduler != null) {
                stateSaveScheduler.shutdown();
            }
        }
    }

    /**
     * RecursiveTask implementation for magic square search that supports work stealing.
     * Each task evolves a magic square for a fixed number of iterations, then either:
     * 1. Returns a valid magic square if found
     * 2. Splits into subtasks if it's beneficial to do so
     * 3. Continues evolving the square
     */
    private static class MagicSquareSearchTask extends RecursiveTask<MagicSquare> {
        /** The current magic square being evolved by this task */
        private MagicSquare currentSquare;

        /** Shared reference to the best square found so far */
        private final AtomicReference<MagicSquare> bestSquare;

        /** Future that will be completed with the first solution found */
        private final CompletableFuture<MagicSquare> solutionFuture;

        /** File to save the best state (may be null) */
        private final File stateFile;

        /** Count of iterations performed by this task */
        private long iterationCount = 0;

        /** Iterations since last score improvement */
        private long iterationsSinceImprovement = 0;

        /** Recursion depth to prevent excessive task splitting */
        private int depth = 0;

        /** Maximum recursion depth allowed */
        private static final int MAX_DEPTH = 15;

        /**
         * Constructor for the initial task
         */
        public MagicSquareSearchTask(MagicSquare initialSquare, AtomicReference<MagicSquare> bestSquare,
                                     CompletableFuture<MagicSquare> solutionFuture, File stateFile) {
            this.currentSquare = initialSquare;
            this.bestSquare = bestSquare;
            this.solutionFuture = solutionFuture;
            this.stateFile = stateFile;
        }

        /**
         * Constructor for subtasks (with depth tracking)
         */
        private MagicSquareSearchTask(MagicSquare initialSquare, AtomicReference<MagicSquare> bestSquare,
                                      CompletableFuture<MagicSquare> solutionFuture, File stateFile,
                                      int depth) {
            this(initialSquare, bestSquare, solutionFuture, stateFile);
            this.depth = depth;
        }

        @Override
        protected MagicSquare compute() {
            // If solution is already found, exit early
            if (solutionFuture.isDone()) {
                return null;
            }

            // Evolve the square for a fixed number of iterations
            while (iterationCount < ITERATIONS_PER_TASK) {
                // Evolve the current square
                currentSquare = currentSquare.evolve();
                iterationCount++;
                iterationsSinceImprovement++;

                // Check if we found a solution
                if (currentSquare.isMagic()) {
                    logger.info("Found magic square solution with perfect score!");
                    solutionFuture.complete(currentSquare);
                    return currentSquare;
                }

                // Update best square if we found a better one
                MagicSquare existingBest = bestSquare.get();
                if (existingBest == null || currentSquare.getScore() > existingBest.getScore()) {
                    try {
                        LOCK.lock();
                        existingBest = bestSquare.get();
                        if (existingBest == null || currentSquare.getScore() > existingBest.getScore()) {
                            bestSquare.set(currentSquare);
                            iterationsSinceImprovement = 0;

                            // Log progress
                            float scoreRatio = ((float) currentSquare.getScore()) / ((float) currentSquare.getMaxScore());
                            logger.info("Found better square with score: {}/{} ({}%)",
                                    currentSquare.getScore(), currentSquare.getMaxScore(),
                                    String.format("%.2f", scoreRatio * 100));

                            // Save the state immediately on improvement
                            if (stateFile != null) {
                                try {
                                    MagicSquareState.save(currentSquare, stateFile);
                                } catch (IOException e) {
                                    logger.error("Failed to save state after improvement: {}", e.getMessage(), e);
                                }
                            }
                        }
                    } finally {
                        LOCK.unlock();
                    }
                }

                // Periodically check if we should adopt a better solution from another worker
                if (iterationCount % 10000 == 0) {
                    existingBest = bestSquare.get();
                    if (existingBest != null && existingBest.getScore() > currentSquare.getScore()) {
                        // Clone the better solution to avoid sharing the same instance
                        currentSquare = MagicSquare.build(existingBest.getValues());
                        iterationsSinceImprovement = 0;
                    }

                    // Check if we should abort due to another thread finding a solution
                    if (solutionFuture.isDone()) {
                        return null;
                    }
                }
            }

            // If we've reached the maximum recursion depth, continue with this task
            if (depth >= MAX_DEPTH) {
                return continueSameTask();
            }

            // Consider whether to split based on progress
            if (iterationsSinceImprovement > ITERATIONS_PER_TASK * 3) {
                // We haven't seen improvement for a while, so split to try different paths
                return splitIntoSubtasks();
            } else {
                // Continue with current path as it's still improving
                return continueSameTask();
            }
        }

        /**
         * Continue evolving in the same task without splitting
         */
        private MagicSquare continueSameTask() {
            // Reset iteration count and continue
            iterationCount = 0;
            return compute();
        }

        /**
         * Split the current task into subtasks for better work distribution
         */
        private MagicSquare splitIntoSubtasks() {
            // Create two child tasks with different starting points
            MagicSquareSearchTask leftTask = new MagicSquareSearchTask(
                    currentSquare.newChild(), bestSquare, solutionFuture, stateFile, depth + 1);

            MagicSquareSearchTask rightTask = new MagicSquareSearchTask(
                    currentSquare.newChild(), bestSquare, solutionFuture, stateFile, depth + 1);

            // Submit the left task and compute the right task directly (work stealing)
            leftTask.fork();
            MagicSquare rightResult = rightTask.compute();

            // If right subtask found a solution, return it
            if (rightResult != null && rightResult.isMagic()) {
                return rightResult;
            }

            // Wait for the left task and check its result
            MagicSquare leftResult = leftTask.join();
            if (leftResult != null && leftResult.isMagic()) {
                return leftResult;
            }

            // No solution found, return the best square we have
            return bestSquare.get();
        }
    }
}