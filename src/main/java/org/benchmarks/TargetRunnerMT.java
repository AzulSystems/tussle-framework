package org.benchmarks;

import static org.benchmarks.tools.FormatTool.*;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.benchmarks.tools.SleepTool;

/**
 * 
 * @author rus
 *
 */
public class TargetRunnerMT implements TargetRunner {

    public static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(TargetRunnerMT.class.getName());

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", TargetRunnerMT.class.getSimpleName(), String.format(format, args)));
        }
    }

    private final int threadCount;

    public TargetRunnerMT(int threads) {
        this.threadCount = threads;
    }

    @Override
    public RunResult runWorkload(String operationName, double targetRate, int runTime, Callable<Boolean> workload, TimeRecorder recorder) throws InterruptedException {
        log("Starting: target rate %s op/s, time %d ms...", roundFormat(targetRate), runTime);
        final ConcurrentHashMap<Integer, RunResult> runResults = new ConcurrentHashMap<>(threadCount);
        final Thread[] threads = new Thread[threadCount];
        double targetPerThread = targetRate / threadCount;
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> runResults.put(idx, new TargetRunnerST().runWorkload(operationName, targetPerThread, runTime, workload, recorder)));
        }
        for (Thread thread : threads) {
            thread.start();
        }
        Thread.sleep(runTime);
        for (Thread thread : threads) {
            thread.join();
        }
        long maxTime = 0;
        long countSum = 0;
        long errorSum = 0;
        for (RunResult runResult : runResults.values()) {
            if (maxTime < runResult.time) {
                maxTime = runResult.time;
            }
            countSum += runResult.count;
            errorSum += runResult.errors;
        }
        RunResult result = RunResult.builder()
                .timeUnits("ms")
                .time(maxTime)
                .count(countSum)
                .rateUnits("op/s")
                .rate(maxTime > 0 ? countSum / ((double) maxTime / MS_IN_S) : 0)
                .errors(errorSum)
                .build();
        SleepTool.sleep(NS_IN_S);
        log("Result: " + result);
        return result;
    }
}
