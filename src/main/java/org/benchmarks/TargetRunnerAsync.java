package org.benchmarks;

import static org.benchmarks.tools.FormatTool.*;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import org.benchmarks.tools.SleepTool;

import lombok.AllArgsConstructor;

/**
 * 
 * @author rus
 *
 */
public class TargetRunnerAsync implements TargetRunner {

    public static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(TargetRunnerAsync.class.getName());

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", TargetRunnerAsync.class.getSimpleName(), String.format(format, args)));
        }
    }

    private final long timeOffset = System.currentTimeMillis() * NS_IN_MS - System.nanoTime();

    private final AtomicLong opsCount = new AtomicLong();
    private final AtomicLong errorsCount = new AtomicLong();
    private final int threadsCount;

    public TargetRunnerAsync (int threads) {
        this.threadsCount = threads;    
    }

    @AllArgsConstructor
    private class C implements Runnable {
        Callable<Boolean> workload;
        TimeRecorder recorder;
        String operation;
        long intendedStartTime;
        @Override
        public void run() {
            boolean success;
            long startTime = System.nanoTime();
            try {
                success = workload.call();
            } catch (Exception e) {
                success = false;
            }
            long finishTime = System.nanoTime();
            opsCount.incrementAndGet();
            if (!success) {
                errorsCount.incrementAndGet();
            }
            if (recorder != null) {
                recorder.recordTimes(operation, startTime + timeOffset, intendedStartTime + timeOffset, finishTime + timeOffset, success);
            }
        }
    }

    @Override
    public RunResult runWorkload(String operationName, double targetRate, int runTime, Callable<Boolean> workload, TimeRecorder recorder) throws InterruptedException {
        if (targetRate <= 0) {
            throw new IllegalArgumentException(String.format("Ivalid targetRate value for %s", TargetRunnerAsync.class.getSimpleName()));
        }
        long delayBetweenOps = (long) (NS_IN_S / targetRate);
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        log("runWorkload: target %s op/s, time %d ms, delayBetweenOps %d ns", roundFormat(targetRate), runTime, delayBetweenOps);
        opsCount.set(0);
        errorsCount.set(0);
        long startRunTime = System.nanoTime();
        long deadline = startRunTime + runTime * NS_IN_MS;
        long opIndex = 0;
        Future<?>[] lastOnes = new Future[1000];
        int lastOneIdx = 0;
        while (deadline - System.nanoTime() > 0) {
            long intendedStartTime = startRunTime + opIndex * delayBetweenOps;
            lastOnes[(lastOneIdx++) % lastOnes.length] = executor.submit(new C(workload, recorder, operationName, intendedStartTime));
            long intendedNextStartTime = startRunTime + (opIndex + 1) * delayBetweenOps;
            SleepTool.sleepUntil(intendedNextStartTime);
            opIndex++;
        }
        log("runWorkload: finishing tasks...");
        for (Future<?> lastOne : lastOnes) {
            if (lastOne != null) {
                try {
                    lastOne.get();
                } catch (ExecutionException e) {
                    ///
                }
            }
        }
        log("runWorkload: executor shutdown...");
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        long ops = opsCount.get();
        long errs = errorsCount.get();
        long actualFinishRunTime = System.nanoTime();
        long time = (actualFinishRunTime - startRunTime) / NS_IN_MS;
        RunResult result = RunResult.builder()
                .timeUnits("ms")
                .time(time)
                .count(ops)
                .errors(errs)
                .rateUnits("op/s")
                .rate(ops > 0 ? ops / ((double) time / MS_IN_S) : 0)
                .build();
        log("runWorkload result: " + result);
        return result;
    }
}
