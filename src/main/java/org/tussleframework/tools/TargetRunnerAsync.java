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
import static org.tussleframework.Globals.*;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import org.tussleframework.RunResult;
import org.tussleframework.TimeRecorder;
import org.tussleframework.TussleException;

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

    public TargetRunnerAsync(int threads) {
        this.threadsCount = threads;
    }

    @AllArgsConstructor
    private class WorkloadCall implements Runnable {
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
                recorder.recordTimes(operation, startTime + timeOffset, intendedStartTime + timeOffset, finishTime + timeOffset, 1, success);
            }
        }
    }

    @Override
    public RunResult runWorkload(String operationName, double targetRate, int runTime, Callable<Boolean> workload, TimeRecorder recorder) throws TussleException {
        if (targetRate <= 0) {
            throw new IllegalArgumentException(String.format("Ivalid targetRate=%f value for %s", targetRate, TargetRunnerAsync.class.getSimpleName()));
        }
        long delayBetweenOps = (long) (NS_IN_S / targetRate);
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        log("Starting: target rate %s op/s, time %d ms, delayBetweenOps %d ns", roundFormat(targetRate), runTime, delayBetweenOps);
        opsCount.set(0);
        errorsCount.set(0);
        long startRunTime = System.nanoTime();
        long deadline = startRunTime + runTime * NS_IN_MS;
        long opIndex = 0;
        Future<?>[] lastOnes = new Future[1000];
        int lastOneIdx = 0;
        while (deadline - System.nanoTime() > 0) {
            long intendedStartTime = startRunTime + opIndex * delayBetweenOps;
            lastOnes[(lastOneIdx++) % lastOnes.length] = executor.submit(new WorkloadCall(workload, recorder, operationName, intendedStartTime));
            opIndex++;
            long intendedNextStartTime = startRunTime + opIndex * delayBetweenOps;
            SleepTool.sleepUntil(intendedNextStartTime);
        }
        log("Finishing tasks...");
        for (Future<?> lastOne : lastOnes) {
            if (lastOne != null) {
                try {
                    lastOne.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new TussleException(e);
                } catch (ExecutionException e) {
                    throw new TussleException(e);
                }
            }
        }
        log("Executor shutdown...");
        executor.shutdownNow();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TussleException(e);
        }
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
                .actualRate(ops > 0 ? ops / ((double) time / MS_IN_S) : 0)
                .build();
        SleepTool.sleep(NS_IN_S);
        log("Result: " + result);
        return result;
    }
}
