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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.tussleframework.RunResult;
import org.tussleframework.TimeRecorder;
import org.tussleframework.TussleException;

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
    
    class ThreadRunner {
        RunResult result;
        Exception error;
        void run(String operationName, double targetPerThread, int runTime, Callable<Boolean> workload, TimeRecorder recorder) {
            try {
                result = new TargetRunnerST().runWorkload(operationName, targetPerThread, runTime, workload, recorder);
            } catch (Exception e) {
                error = e;
                result = RunResult.builder().build();
            }
        }
    }

    @Override
    public RunResult runWorkload(String operationName, double targetRate, int runTime, Callable<Boolean> workload, TimeRecorder recorder) throws TussleException {
        log("Starting: target rate %s op/s, time %d ms...", roundFormat(targetRate), runTime);
        final ConcurrentHashMap<Integer, RunResult> runResults = new ConcurrentHashMap<>(threadCount);
        final Thread[] threads = new Thread[threadCount];
        final Exception[] errors = new Exception[threadCount];
        double targetPerThread = targetRate / threadCount;
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                ThreadRunner tr = new ThreadRunner();
                tr.run(operationName, targetPerThread, runTime, workload, recorder);
                runResults.put(idx, tr.result);
                errors[idx] = tr.error;
            });
        }
        for (Thread thread : threads) {
            thread.start();
        }
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TussleException(e);
        }
        for (Exception error : errors) {
            if (error != null) {
                throw new TussleException(error);
            }
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
                .actualRate(maxTime > 0 ? countSum / ((double) maxTime / MS_IN_S) : 0)
                .errors(errorSum)
                .build();
        SleepTool.sleep(NS_IN_S);
        log("Result: " + result);
        return result;
    }
}
