package org.benchmarks;

import org.HdrHistogram.Recorder;

public class HdrTimeRecorder implements TimeRecorder {
    public final Recorder serviceTimeRecorder = new Recorder(Long.MAX_VALUE, 3);
    public final Recorder responseTimeRecorder = new Recorder(Long.MAX_VALUE, 3);
    public final Recorder errorsRecorder = new Recorder(Long.MAX_VALUE, 3);

    @Override
    public void recordTimes(String operation, long startTime, long intendedStartTime, long finishTime, boolean success) {
        if (success) {
            if (startTime > 0) {
                serviceTimeRecorder.recordValue(finishTime - startTime);
            }
            if (intendedStartTime > 0) {
                responseTimeRecorder.recordValue(finishTime - intendedStartTime);
            }
        } else {
            errorsRecorder.recordValue(finishTime - intendedStartTime);
        }
    }
}
