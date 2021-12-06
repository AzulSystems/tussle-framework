package org.benchmarks;

import java.util.concurrent.Callable;

public interface TargetRunner {
    RunResult runWorkload(String operationName, double targetRate, int runTime, Callable<Boolean> workload, TimeRecorder recorder) throws InterruptedException;
}
