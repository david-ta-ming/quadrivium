package net.noisynarwhal.quadrivium;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark class for measuring the performance of magic square search.
 * Uses JMH (Java Microbenchmark Harness) to perform accurate benchmarking.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 0)
@Measurement(iterations = 7)
public class SearchBenchmark {
    /**
     * The order (size) of the magic square to search for.
     * Tested with different values to measure performance scaling.
     */
    @Param({"19", "29", "41", "59"})  // Test different square sizes
    private int order;

    /**
     * Benchmark method that searches for a magic square of the specified order.
     * Uses auto-detected number of threads based on available processors.
     *
     * @return The found magic square, or null if not found
     */
    @Benchmark
    public MagicSquare benchmarkFindSolution() {
        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        final int numThreads = Math.max(3, (availableProcessors * 2) / 3);
        return MagicSquareWorker.search(order, numThreads);
    }

    /**
     * Main entry point for running the benchmark.
     * Configures and executes the JMH benchmark suite.
     *
     * @param args Command line arguments (not used)
     * @throws RunnerException if there is an error running the benchmark
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SearchBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}