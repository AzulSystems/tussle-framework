package org.benchmarks;

public interface TimeRecorder {
    void recordTimes(String operation, long startTime, long intendedStartTime, long finishTime, boolean success);
}
