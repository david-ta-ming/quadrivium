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
 */
public class MagicSquareWorker {
    private static final Logger logger = LoggerFactory.getLogger(MagicSquareWorker.class);
    private static final Lock LOCK = new ReentrantLock();
    private static final int POPULATION_MULTIPLIER = 4;
    private static final int ITERATIONS_PER_TASK = 100000;
    private static final long STATE_SAVE_INTERVAL_MS = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES);

    public static MagicSquare search(int order, int numThreads) {
        try {
            return search(order, numThreads, null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load magic square", e);
        }
    }

    public static MagicSquare search(int order, int numThreads, File stateFile) throws IOException {
        if (order < 3) {
            throw new IllegalArgumentException("Order must be at least 3");
        }
        if (numThreads <= 0) {
            throw new IllegalArgumentException("Number of threads must be greater than 0");
        }

        final ForkJoinPool forkJoinPool = new ForkJoinPool(numThreads);
        final CompletableFuture<MagicSquare> solutionFuture = new CompletableFuture<>();
        final AtomicReference<MagicSquare> bestSquare = new AtomicReference<>(null);
        final ScheduledExecutorService stateSaveScheduler = (stateFile != null) ?
                Executors.newSingleThreadScheduledExecutor() : null;

        try {
            final boolean restoreState = stateFile != null && stateFile.canRead() && stateFile.length() > 0;
            if (restoreState) {
                logger.info("Restoring state from file: {}", stateFile.getAbsolutePath());
            }

            final MagicSquare initialSquare = restoreState ?
                    MagicSquareState.load(stateFile) : MagicSquare.build(order);

            bestSquare.set(initialSquare);
            logger.info("Initial magic square with score: {}/{}",
                    initialSquare.getScore(), initialSquare.getMaxScore());

            // Set up periodic state saving
            if (stateFile != null) {
                stateSaveScheduler.scheduleAtFixedRate(() -> {
                    MagicSquare currentBest = bestSquare.get();
                    if (currentBest != null) {
                        try {
                            LOCK.lock();
                            logger.debug("Periodic state save - current best score: {}/{}",
                                    currentBest.getScore(), currentBest.getMaxScore());
                            MagicSquareState.save(currentBest, stateFile);
                        } catch (IOException e) {
                            logger.error("Failed to save state during periodic save", e);
                        } finally {
                            LOCK.unlock();
                        }
                    }
                }, STATE_SAVE_INTERVAL_MS, STATE_SAVE_INTERVAL_MS, TimeUnit.MILLISECONDS);
            }

            // Create tasks
            final List<MagicSquareSearchTask> allTasks = new ArrayList<>();
            for (int i = 0; i < numThreads * POPULATION_MULTIPLIER; i++) {
                final MagicSquare startingSquare = (i == 0) ? initialSquare :
                        (restoreState ? MagicSquare.build(initialSquare.getValues()) : MagicSquare.build(order));
                allTasks.add(new MagicSquareSearchTask(startingSquare, bestSquare, solutionFuture, stateFile));
            }

            // Submit all tasks
            final List<ForkJoinTask<MagicSquare>> submittedTasks = new ArrayList<>();
            for (MagicSquareSearchTask task : allTasks) {
                submittedTasks.add(forkJoinPool.submit(task));
            }

            // Wait for solution
            MagicSquare result = null;
            try {
                logger.info("Waiting for solution...");
                result = solutionFuture.get();
                logger.info("Solution found by one of the workers!");

                // Save final result
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

                // Cancel all tasks
                for (ForkJoinTask<MagicSquare> task : submittedTasks) {
                    task.cancel(true);
                }

                result = bestSquare.get();
            }

            return result;
        } finally {
            // Ensure proper cleanup
            forkJoinPool.shutdownNow();
            try {
                if (!forkJoinPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("ForkJoinPool did not terminate cleanly");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (stateSaveScheduler != null) {
                stateSaveScheduler.shutdownNow();
                try {
                    if (!stateSaveScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                        logger.warn("State save scheduler did not terminate cleanly");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * RecursiveTask implementation for magic square search
     */
    private static class MagicSquareSearchTask extends RecursiveTask<MagicSquare> {
        private MagicSquare currentSquare;
        private final AtomicReference<MagicSquare> bestSquare;
        private final CompletableFuture<MagicSquare> solutionFuture;
        private final File stateFile;
        private long iterationCount = 0;
        private long iterationsSinceImprovement = 0;
        private int depth = 0;
        private static final int MAX_DEPTH = 10; // Reduced to prevent excessive recursion
        private static final int MAX_ITERATIONS_WITHOUT_IMPROVEMENT = ITERATIONS_PER_TASK * 5;

        public MagicSquareSearchTask(MagicSquare initialSquare, AtomicReference<MagicSquare> bestSquare,
                                     CompletableFuture<MagicSquare> solutionFuture, File stateFile) {
            this.currentSquare = initialSquare;
            this.bestSquare = bestSquare;
            this.solutionFuture = solutionFuture;
            this.stateFile = stateFile;
        }

        private MagicSquareSearchTask(MagicSquare initialSquare, AtomicReference<MagicSquare> bestSquare,
                                      CompletableFuture<MagicSquare> solutionFuture, File stateFile, int depth) {
            this(initialSquare, bestSquare, solutionFuture, stateFile);
            this.depth = depth;
        }

        @Override
        protected MagicSquare compute() {
            // Check if solution already found or task cancelled
            if (solutionFuture.isDone() || Thread.currentThread().isInterrupted()) {
                return null;
            }

            // Evolution loop
            while (iterationCount < ITERATIONS_PER_TASK && !Thread.currentThread().isInterrupted()) {
                currentSquare = currentSquare.evolve();
                iterationCount++;
                iterationsSinceImprovement++;

                // Check for solution
                if (currentSquare.isMagic()) {
                    logger.info("Found magic square solution with perfect score!");
                    solutionFuture.complete(currentSquare);
                    return currentSquare;
                }

                // Update best square
                updateBestSquare();

                // Periodic checks
                if (iterationCount % 10000 == 0) {
                    // Adopt better solution from other workers
                    adoptBetterSolution();

                    // Check if we should abort
                    if (solutionFuture.isDone() || Thread.currentThread().isInterrupted()) {
                        return null;
                    }
                }
            }

            // Decide whether to continue, split, or terminate
            if (Thread.currentThread().isInterrupted() || solutionFuture.isDone()) {
                return null;
            }

            if (depth >= MAX_DEPTH || iterationsSinceImprovement > MAX_ITERATIONS_WITHOUT_IMPROVEMENT) {
                // Too deep or no progress - return current best
                return bestSquare.get();
            }

            if (iterationsSinceImprovement > ITERATIONS_PER_TASK * 2) {
                // Split task for better exploration
                return splitIntoSubtasks();
            } else {
                // Continue with current task
                iterationCount = 0;
                return compute();
            }
        }

        private void updateBestSquare() {
            MagicSquare existingBest = bestSquare.get();
            if (existingBest == null || currentSquare.getScore() > existingBest.getScore()) {
                try {
                    LOCK.lock();
                    existingBest = bestSquare.get();
                    if (existingBest == null || currentSquare.getScore() > existingBest.getScore()) {
                        bestSquare.set(currentSquare);
                        iterationsSinceImprovement = 0;

                        float scoreRatio = ((float) currentSquare.getScore()) / ((float) currentSquare.getMaxScore());
                        logger.debug("Better square found - score: {}/{} ({}%)",
                                currentSquare.getScore(), currentSquare.getMaxScore(),
                                String.format("%.2f", scoreRatio * 100));

                        // Save state on improvement
                        if (stateFile != null) {
                            try {
                                MagicSquareState.save(currentSquare, stateFile);
                            } catch (IOException e) {
                                logger.error("Failed to save state: {}", e.getMessage());
                            }
                        }
                    }
                } finally {
                    LOCK.unlock();
                }
            }
        }

        private void adoptBetterSolution() {
            MagicSquare existingBest = bestSquare.get();
            if (existingBest != null && existingBest.getScore() > currentSquare.getScore()) {
                currentSquare = MagicSquare.build(existingBest.getValues());
                iterationsSinceImprovement = 0;
            }
        }

        private MagicSquare splitIntoSubtasks() {
            if (depth >= MAX_DEPTH) {
                return bestSquare.get();
            }

            MagicSquareSearchTask leftTask = new MagicSquareSearchTask(
                    currentSquare.newChild(), bestSquare, solutionFuture, stateFile, depth + 1);
            MagicSquareSearchTask rightTask = new MagicSquareSearchTask(
                    currentSquare.newChild(), bestSquare, solutionFuture, stateFile, depth + 1);

            leftTask.fork();
            MagicSquare rightResult = rightTask.compute();

            if (rightResult != null && rightResult.isMagic()) {
                return rightResult;
            }

            MagicSquare leftResult = leftTask.join();
            if (leftResult != null && leftResult.isMagic()) {
                return leftResult;
            }

            return bestSquare.get();
        }
    }
}