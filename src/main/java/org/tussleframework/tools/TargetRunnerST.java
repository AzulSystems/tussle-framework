/*
 * Copyright (c) 2021-2022, Azul Systems
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * * Neither the name of [project] nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package org.tussleframework.tools;

import static org.tussleframework.tools.FormatTool.*;

import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.tussleframework.RunResult;
import org.tussleframework.TimeRecorder;
import org.tussleframework.TussleException;

/**
 * 
 * @author rus
 *
 */
public class TargetRunnerST implements TargetRunner {

    public static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(TargetRunnerST.class.getName());

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
    public RunResult runWorkload(String operationName, double targetRate, int runTime, Callable<Boolean> workload, TimeRecorder recorder) throws TussleException {
        if (runTime <= 0) {
            return null;
        }
        log("Starting: target rate %s op/s, time %d ms...", roundFormat(targetRate), runTime);
        boolean throttled = targetRate > 0;
        long delayBetweenOps = (long) (throttled ? (NS_IN_S / targetRate) : 0);
        long startRunTime = System.nanoTime();
        long finishRunTime = startRunTime + runTime * NS_IN_MS;
        long opIndex = 0;
        long errs = 0;
        long startTime = startRunTime;
        while (startTime < finishRunTime) {
            long intendedStartTime = startRunTime + opIndex * delayBetweenOps;
            boolean success;
            try {
                success = workload.call();
            } catch (Exception e) {
                throw new TussleException(e);
            }
            long finishTime = System.nanoTime();
            if (recorder != null) {
                recorder.recordTimes(operationName, startTime + timeOffset, throttled ? intendedStartTime + timeOffset : 0, finishTime + timeOffset, 1, success);
            }
            if (throttled) {
                long intendedNextStartTime = (startRunTime + (opIndex + 1) * delayBetweenOps);
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
        SleepTool.sleep(NS_IN_S);
        log("Result: " + result);
        return result;
    }
}
