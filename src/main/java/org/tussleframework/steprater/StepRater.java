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

package org.tussleframework.steprater;

import static org.tussleframework.tools.FormatTool.parseTimeLength;
import static org.tussleframework.tools.FormatTool.parseValue;
import static org.tussleframework.tools.FormatTool.roundFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.tussleframework.Benchmark;
import org.tussleframework.BenchmarkConfig;
import org.tussleframework.HdrResult;
import org.tussleframework.ResultsRecorder;
import org.tussleframework.RunResult;
import org.tussleframework.metrics.Interval;
import org.tussleframework.metrics.MovingWindowSLE;
import org.tussleframework.tools.ConfigLoader;
import org.tussleframework.tools.LoggerTool;

public class StepRater {

    private static final Logger logger = Logger.getLogger(StepRater.class.getName());

    static final Exception USAGE = new Exception("Expected args: benchmark-class-name [benchmark-parameters...] [--tussle tussle-parameters...]");

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", StepRater.class.getSimpleName(), String.format(format, args)));
        }
    }

    public static void log(Exception e) {
        LoggerTool.logException(logger, e);
    }

    public static void main(String[] args) throws ClassNotFoundException {
        new StepRater().initAndRun(args);
    }

    public static StepRaterConfig loadTussleConfig(String[] args) throws IOException, ReflectiveOperationException  {
        StepRaterConfig runnerConfig = ConfigLoader.loadObject(args, StepRaterConfig.class);
        if (runnerConfig.sleConfig.length == 0) {
            runnerConfig.sleConfig = new MovingWindowSLE[] {
                    new MovingWindowSLE(90, 0, 10),
            };
        }
        if (runnerConfig.intervals.length == 0) {
            runnerConfig.intervals = new Interval[] {
                    new Interval(0, Long.MAX_VALUE, ""),
            };
        }
        return runnerConfig;
    }

    protected StepRaterConfig runnerConfig;
    protected String rateUnits = "op/s";

    public StepRater() {
    }

    public StepRater(StepRaterConfig runnerConfig) {
        this.runnerConfig = runnerConfig;
    }

    public StepRater(String[] tussleArgs) throws IOException, ReflectiveOperationException  {
        this.runnerConfig = loadTussleConfig(tussleArgs);
    }

    /**
     * @param args - benchmark-class-name [benchmark-args...] --tussle [tussle-runner-args...]  
     */
    public void initAndRun(String[] args) throws ClassNotFoundException {
        LoggerTool.init("tussle-benchmark");
        if (args.length == 0) {
            throw new IllegalArgumentException("Missing arguments");
        }
        String className = args[0];
        @SuppressWarnings("unchecked")
        Class<? extends Benchmark> benchmarkClass = (Class<? extends Benchmark>) ClassLoader.getSystemClassLoader().loadClass(className);
        initAndRun(benchmarkClass, Arrays.copyOfRange(args, 1, args.length));
    }

    /**
     * @param benchmarkClass
     * @param args - [benchmark-args...] --tussle [tussle-runner-args...]
     */
    public void initAndRun(Class<? extends Benchmark> benchmarkClass, String[] args) {
        try {
            int idx = IntStream.range(0, args.length).filter(i -> args[i].equals("--tussle")).findFirst().orElse(-1);
            String[] benchmarkArgs = idx >= 0 ? Arrays.copyOfRange(args, 0, idx) : args;
            String[] tussleArgs = idx >= 0 ? Arrays.copyOfRange(args, idx + 1, args.length) : new String[0];
            initAndRun(benchmarkClass.getConstructor().newInstance(), benchmarkArgs, tussleArgs);
        } catch (Exception e) {
            log(e);
        }
    }

    public void initAndRun(Benchmark benchmark, String[] benchmarkArgs, String[] tussleArgs) throws Exception {
        runnerConfig = loadTussleConfig(tussleArgs);
        benchmark.init(benchmarkArgs);
        run(benchmark);
        benchmark.cleanup();    
    }

    protected RunResult runSingle(Benchmark benchmark, double targetRate, int warmupTime, int runTime, double ratePercent, int retry, List<HdrResult> results, boolean writeHdr, boolean reset) throws Exception {
        BenchmarkConfig config = benchmark.getConfig();
        ResultsRecorder recorder = new ResultsRecorder(config, ratePercent, targetRate, retry, runTime, writeHdr);
        log("Reguesting rate %s %s (%s%%), warmup %d s, run time %d s...", roundFormat(targetRate), rateUnits, roundFormat(ratePercent), warmupTime, runTime);
        RunResult result;
        try {
            if (reset) {
                log("Benchmark reset...");
                benchmark.reset();
            }
            result = benchmark.run(targetRate, warmupTime, runTime, recorder);
            if (ratePercent > 0) {
                log("Reguested rate %s %s (%s%%), actual rate %s %s", roundFormat(targetRate), rateUnits, roundFormat(ratePercent), roundFormat(result.rate), rateUnits);
            } else {
                log("Reguested rate %s %s, actual rate %s %s", roundFormat(targetRate), rateUnits, roundFormat(result.rate), rateUnits);
            }
            log("------------------------------------------------");
        } finally {
            recorder.cancel();
        }
        if (result.rate < 0) {
            log("Failed to find max-rate on this step: %s", roundFormat(result.rate));
            return null;
        }
        recorder.getResults(results);
        return result;
    }

    public double findHighBound(Benchmark benchmark) throws Exception {
        BenchmarkConfig benchmarkConfig = benchmark.getConfig();
        log("Searching for high-bound...");
        int[] steps = runnerConfig.getHighBoundSteps();
        double highBound = parseValue(runnerConfig.getHighBound());
        int stepFactorMax = 10;
        for (int i = 0; i < steps.length; i++) {
            int step = steps[i];
            log("Starting iterating with step %d", step);
            for (int stepFactor = 1; stepFactor <= stepFactorMax; stepFactor++) {
                double targetRate = highBound + (double) step * stepFactor;
                int highBoundWarmupTime = parseTimeLength(runnerConfig.getHighBoundWarmupTime());
                int highBoundRunTime = parseTimeLength(runnerConfig.getHighBoundTime());
                int warmupTime = highBoundWarmupTime > 0 ? highBoundWarmupTime : parseTimeLength(benchmarkConfig.getWarmupTime());
                int runTime = highBoundRunTime > 0 ? highBoundRunTime : parseTimeLength(benchmarkConfig.getRunTime());
                RunResult result = runSingle(benchmark, targetRate, warmupTime, runTime, 0.0, 0, null, false, runnerConfig.isResetEachStep());
                rateUnits = result.rateUnits;
                if (result.rate < 0) {
                    log("Failed to find high-bound on this step: %d", highBound);
                    return -1;
                }
                if (targetRate >= runnerConfig.targetFactor * result.rate) {
                    if (stepFactor == 1 && i == steps.length - 1 && highBound == 0) {
                        highBound = step; // 1000
                        log("Using minimal high-bound on this step=%d inc=%d: %s", step, stepFactor, roundFormat(highBound));
                    } else {
                        highBound += step * (stepFactor - 1);
                        log("High-bound on this step=%d inc=%d: %s", step, stepFactor, roundFormat(highBound));
                    }
                    break;
                }
            }
        }
        log("High-bound found: %s %s", roundFormat(highBound), rateUnits);
        return highBound;
    }

    public void iterateTargets(Benchmark benchmark, double highBound, double startingRatePercent, double finishingRatePercent, double ratePercentStep, int finerRateSteps, List<HdrResult> results) throws Exception {
        log("Iterating target rates agains high-bound %s from %s%% to %s%%, step %s%%...", roundFormat(highBound), roundFormat(startingRatePercent), roundFormat(finishingRatePercent), roundFormat(ratePercentStep));
        int retry = 0;
        int retriesMax = runnerConfig.getRetriesMax();
        double ratePercent = startingRatePercent;
        double targetFactor = runnerConfig.getTargetFactor();
        boolean reachedFinishingRate = false;
        int warmupTime = parseTimeLength(benchmark.getConfig().getWarmupTime());
        int runTime = parseTimeLength(benchmark.getConfig().getRunTime());
        while (!reachedFinishingRate) {
            double targetRate = (highBound * ratePercent) / 100.0;
            RunResult result = runSingle(benchmark, targetRate, warmupTime, runTime, ratePercent, retry, results, true, runnerConfig.isResetEachStep());
            if (result == null) {
                break;
            }
            if (result.rate * targetFactor <= targetRate) {
                if (retry < retriesMax) {
                    log("Retrying rate %s %s", roundFormat(targetRate), rateUnits);
                    retry++;
                } else {
                    reachedFinishingRate = true;
                    if (finerRateSteps > 0) {
                        double finerStartRate = ratePercentStep < ratePercent ? ratePercent - ratePercentStep : ratePercent / 2;
                        double finerFinishRate = ratePercent;
                        double finerStep = (finerFinishRate - finerStartRate) / finerRateSteps;
                        log("Finishing rate found: %s %s - performing finer iterations from %s%% to %s%% (%s%%)...", roundFormat(targetRate), rateUnits, roundFormat(finerStartRate), roundFormat(finerFinishRate), roundFormat(finerStep));
                        iterateTargets(benchmark, highBound, finerStartRate, finerFinishRate, finerStep, 0, results);
                    } else {
                        log("Finishing rate found: %s %s", roundFormat(targetRate), rateUnits);
                    }
                }
            } else if (ratePercent >= finishingRatePercent) {
                log("Stopped iterating (high-bound: %s, current ratePercent: %s) - reached finishing rate: %s", roundFormat(highBound), roundFormat(ratePercent), roundFormat(finishingRatePercent));
                reachedFinishingRate = true;
            } else {
                retry = 0;
                ratePercent += ratePercentStep;
            }
        }
    }

    
    /**
     * 
     * Steprater workflow:
     * 
     *  startup warmup
     *  find high bound
     *  iterate target rates:
     *    target 1: [warmup][run] -> result 1
     *    target 2: [warmup][run] -> result 2
     *    ...
     *  Analyse results:
     *    SLE 1 broken on target X
     *    SLE 2 broken on target Y 
     * 
     * @param benchmark
     * @throws Exception
     */
    public void run(Benchmark benchmark) throws Exception {
        log("Tussle started");
        BenchmarkConfig benchmarkConfig = benchmark.getConfig();
        log("Benchmark config:");
        log(benchmarkConfig.toString());
        log("Tussle config:");
        log(runnerConfig.toString());
        log("First benchmark reset...");
        benchmark.reset();
        int startupWarmupTime = parseTimeLength(runnerConfig.getStartupWarmupTime());
        if (startupWarmupTime > 0) {
            double warmupTargetRate = parseValue(benchmarkConfig.getTargetRate());
            log("Starting warmup %s %s (%ds)...", roundFormat(warmupTargetRate), rateUnits, startupWarmupTime);
            benchmark.run(warmupTargetRate, startupWarmupTime, 0, null);
            runSingle(benchmark, warmupTargetRate, startupWarmupTime, 0, 0, 0, null, false, false);
        }
        double highBound = parseValue(runnerConfig.getHighBound());
        if (highBound == 0 || runnerConfig.isHighboundOnly()) {
            highBound = findHighBound(benchmark);
            if (runnerConfig.isHighboundOnly()) {
                log("Tussle end - found high-bound only");
                return;
            }
        } else {
            log("High-bound from setup: %s", roundFormat(highBound));
        }
        if (highBound > 0) {
            ArrayList<HdrResult> results = new ArrayList<>();
            iterateTargets(benchmark, highBound, runnerConfig.getStartingRatePercent(), runnerConfig.getFinishingRatePercent(), runnerConfig.getRatePercentStep(), runnerConfig.getFinerRateSteps(), results);
            runnerConfig.setMakeReport(runnerConfig.isMakeReport() || benchmarkConfig.isMakeReport());
            runnerConfig.setResultsDir(benchmarkConfig.getHistogramsDir());
            runnerConfig.setReportDir(benchmarkConfig.getReportDir());
            // TODO: need abstraction
            new StepRaterAnalyser().processResults(runnerConfig, results);
        }
        log("Tussle end");
    }
}
