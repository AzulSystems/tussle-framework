/*
 * Copyright (c) 2021, Azul Systems
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

import static org.junit.Assert.fail;

import java.util.Random;
import java.util.concurrent.locks.LockSupport;

import org.HdrHistogram.Histogram;
import org.junit.Test;
import org.tussleframework.RunnableWithError;
import org.tussleframework.metrics.HdrTimeRecorder;
import org.tussleframework.tools.LoggerTool;
import org.tussleframework.tools.TargetRunnerAsync;
import org.tussleframework.tools.TargetRunnerMT;
import org.tussleframework.tools.TargetRunnerST;

/**
 * 
 * Spherical horse in vacuum
 * 
 * @author rus
 *
 */
public class SleepWlTest {
    
    {
        LoggerTool.init("", "java.util.logging.ConsoleHandler");
    }

    static void log(String s) {
        System.err.println(System.nanoTime() + ": " + s);
    }

    static final Random random = new Random();

    static int random(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    static String formatPercentiles(Histogram histogram) {
        final long FACTOR = 1000000L;
        return "cnt=" + histogram.getTotalCount() +
                " p0=" + histogram.getValueAtPercentile(0) / FACTOR +
                " p50=" + histogram.getValueAtPercentile(50) / FACTOR +
                " p90=" + histogram.getValueAtPercentile(90) / FACTOR +
                " p99=" + histogram.getValueAtPercentile(99) / FACTOR +
                " p100=" + histogram.getValueAtPercentile(100) / FACTOR;
    }

    static boolean sleep(int ms) {
        LockSupport.parkNanos(ms * 1000000);
        return true;
    }

    static boolean sleepR(int ms, int d) {
        return sleep(ms + random(-d, d));
    }

//    @Test
    public void testSleepWorkload() {
        int ms = 3000;
        try {
//          sleepWorkload(5, ms);
            sleepWorkload(10, ms);
//            sleepWorkload(50, ms);
//            sleepWorkload(95, ms);
//            sleepWorkload(99, ms);
            sleepWorkload(100, ms);
//          sleepWorkload(120, ms);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

//    @Test
    public void testSleepWorkloadMT() {
        int ms = 3000;
//        sleepWorkloadMT(10, ms, 4);
//        sleepWorkloadMT(50, ms, 1);
//        sleepWorkloadMT(50, ms, 4);
//        sleepWorkloadMT(95, ms, 4);
//        sleepWorkloadMT(99, ms, 4);
//        sleepWorkloadMT(100, ms, 1);
//        sleepWorkloadMT(100, ms, 2);
//        sleepWorkloadMT(100, ms, 4);
//        sleepWorkloadMT(120, ms, 1);
//        sleepWorkloadMT(120, ms, 4);
//        sleepWorkloadMT(120, ms, 8);
//        sleepWorkloadMT(200, ms, 1);
//        sleepWorkloadMT(200, ms, 2);
//        sleepWorkloadMT(200, ms, 4);
        try {
            sleepWorkloadMT(800, ms, 1);
            sleepWorkloadMT(800, ms, 2);
            sleepWorkloadMT(800, ms, 4);
            sleepWorkloadMT(800, ms, 8);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test
    public void testSleepWorkloadAsync() {
        int ms = 3000;
        try {
            sleepWorkloadMT(800, ms, 1);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void sleepWorkload(double targetRate, int timeInMs) throws Exception {
        RunnableWithError workload = () -> sleep(10);
        runWorkload(targetRate, timeInMs, workload);
    }

    public void sleepWorkloadMT(double targetRate, int timeInMs, int threads) throws Exception {
        RunnableWithError workload = () -> sleep(10);
        runWorkloadMT(targetRate, timeInMs, threads, workload);
    }

    public void sleepRandomWorkloadMT(double targetRate, int timeInMs, int threads) throws Exception {
        RunnableWithError workload = () -> sleepR(10, 1);
        runWorkloadMT(targetRate, timeInMs, threads, workload);
    }

    public void runWorkload(double targetRate, int timeInMs, RunnableWithError workload) throws Exception {
        log("runWorkload targetRate=" + targetRate + ", timeInMs=" + timeInMs);
        final HdrTimeRecorder timeRecorder = new HdrTimeRecorder();
        double actualThroughput = new TargetRunnerST().runWorkload("sleep", targetRate, timeInMs, workload, timeRecorder).rate;
        log(" actualThroughput(op/sec): " + actualThroughput);
        log(" serviceTime(ms): " + formatPercentiles(timeRecorder.serviceTimeRecorder.getIntervalHistogram()));
        log(" latency(ms): " + formatPercentiles(timeRecorder.responseTimeRecorder.getIntervalHistogram()));
    }

    public void runWorkloadMT(double targetRate, int timeInMs, int threads, RunnableWithError workload) throws Exception {
        log("runWorkloadMT targetRate=" + targetRate + ", timeInMs=" + timeInMs + ". threads=" + threads);
        final HdrTimeRecorder timeRecorder = new HdrTimeRecorder();
        double actualThroughput = new TargetRunnerMT(threads).runWorkload("sleep", targetRate, timeInMs, workload, timeRecorder).rate;
        log(" actualThroughput(op/sec): " + actualThroughput);
        log(" serviceTime(ms): " + formatPercentiles(timeRecorder.serviceTimeRecorder.getIntervalHistogram()));
        log(" latency(ms): " + formatPercentiles(timeRecorder.responseTimeRecorder.getIntervalHistogram()));
    }

    public void runWorkloadAsync(double targetRate, int timeInMs, int threads, RunnableWithError workload) throws Exception {
        log("runWorkloadAsync targetRate=" + targetRate + ", timeInMs=" + timeInMs + ". threads=" + threads);
        final HdrTimeRecorder timeRecorder = new HdrTimeRecorder();
        double actualThroughput = new TargetRunnerAsync(threads).runWorkload("sleep", targetRate, timeInMs, workload, timeRecorder).rate;
        log(" actualThroughput(op/sec): " + actualThroughput);
        log(" serviceTime(ms): " + formatPercentiles(timeRecorder.serviceTimeRecorder.getIntervalHistogram()));
        log(" latency(ms): " + formatPercentiles(timeRecorder.responseTimeRecorder.getIntervalHistogram()));
    }
}
