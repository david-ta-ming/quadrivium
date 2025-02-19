package net.noisynarwhal.quadrivium;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 0)
@Measurement(iterations = 7)
public class SearchBenchmark {

    @Param({"19", "29", "37"})  // Test different square sizes
    private int order;

    @Benchmark
    public MagicSquare benchmarkFindSolution() {
        final int numThreads = Math.max(3, Runtime.getRuntime().availableProcessors() / 2);
        return MagicSquareWorker.search(order, numThreads);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SearchBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}