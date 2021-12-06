package org.benchmarks;

import static org.benchmarks.tools.FormatTool.roundFormat;

import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.benchmarks.tools.SleepTool;

/**
 * 
 * @author rus
 *
 */
public class TargetRunnerST implements TargetRunner {

    public static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(TargetRunnerST.class.getName());

    private static final long NS_IN_S = 1000000000L;
    private static final long NS_IN_MS = 1000000L;
    private static final long MS_IN_S = 1000L;

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", TargetRunnerST.class.getSimpleName(), String.format(format, args)));
        }
    }

    private final long timeOffset = System.currentTimeMillis() * NS_IN_MS - System.nanoTime();

    public TargetRunnerST() {
        ///
    }

    @Override
    public RunResult runWorkload(String operationName, double targetRate, int runTime, Callable<Boolean> workload, TimeRecorder recorder) {
        log("runWorkload: target %s op/s, time %d ms", roundFormat(targetRate), runTime);
        boolean throttled = targetRate > 0;
        double delayBetweenOps = throttled ? (NS_IN_S / targetRate) : 0;
        long startRunTime = System.nanoTime();
        long finishRunTime = startRunTime + runTime * NS_IN_MS;
        long opIndex = 0;
        long errs = 0;
        long startTime = startRunTime;
        while (startTime < finishRunTime) {
            long intendedStartTime = (long) (startRunTime + opIndex * delayBetweenOps);
            boolean success = false;
            try {
                success = workload.call();
            } catch (Exception e) {
            }
            long finishTime = System.nanoTime();
            if (recorder != null) {
                recorder.recordTimes(operationName, startTime + timeOffset, throttled ? intendedStartTime + timeOffset : 0, finishTime + timeOffset, success);
            }
            if (throttled) {
                long intendedNextStartTime = (long) (startRunTime + (opIndex + 1) * delayBetweenOps);
                SleepTool.sleepUntil(intendedNextStartTime);
            }
            opIndex++;
            errs += success ? 0 : 1;
            startTime = System.nanoTime();
        }
        long actualFinishRunTime = System.nanoTime();
        long time = (actualFinishRunTime - startRunTime) / NS_IN_MS;
        RunResult result = RunResult.builder()
                .timeUnits("ms")
                .time(time)
                .count(opIndex)
                .errors(errs)
                .rateUnits("op/s")
                .rate(opIndex > 0 ? opIndex / ((double) time / MS_IN_S) : 0)
                .build();
        log("runWorkload result: " + result);
        return result;
    }
}
