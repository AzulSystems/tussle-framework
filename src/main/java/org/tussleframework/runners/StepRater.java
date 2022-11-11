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

package org.tussleframework.runners;

import static org.tussleframework.tools.FormatTool.parseTimeLength;
import static org.tussleframework.tools.FormatTool.parseValue;
import static org.tussleframework.tools.FormatTool.roundFormat;
import static org.tussleframework.tools.FormatTool.withS;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.tussleframework.Benchmark;
import org.tussleframework.RunArgs;
import org.tussleframework.RunResult;
import org.tussleframework.TussleException;
import org.yaml.snakeyaml.Yaml;

public class StepRater extends BasicRunner {

    private static final Logger logger = Logger.getLogger(StepRater.class.getName());

    static final Exception USAGE = new Exception("Expected args: benchmark-class-name [benchmark-parameters...] [--tussle tussle-parameters...]");

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", StepRater.class.getSimpleName(), String.format(format, args)));
        }
    }

    public StepRater() {
    }

    public StepRater(String[] args) throws TussleException {
        init(args);
    }

    public StepRater(StepRaterConfig config) {
        this.runnerConfig = config;
        this.runnerConfig.validate(true);
    }

    @Override
    public void init(String[] args) throws TussleException {
        this.runnerConfig = StepRaterConfig.load(args);
        this.runnerConfig.validate(true);
    }

    public double highBoundFromMaxRate(Benchmark benchmark) throws TussleException {
        log("Detecting high-bound from max rate...");
        StepRaterConfig config = (StepRaterConfig) this.runnerConfig;
        String rateUnits = benchmark.getConfig().rateUnits;
        int warmupTime = parseTimeLength(config.highBoundWarmupTime);
        int runTime = parseTimeLength(config.highBoundRunTime);
        runTime = runTime > 0 ? runTime : parseTimeLength(config.runTime);
        RunArgs runArgs = new RunArgs(0, 0.0, warmupTime, runTime, 0, "find-high-bound");
        RunResult result = runOnce(benchmark, runArgs, false, config.resetEachStep);
        if (result.actualRate <= 0) {
            log("Failed to find max-rate using unthrottled targetRate, warmupTime %d s, runTime %d s", warmupTime, runTime);
            return -1;
        }
        double step = config.highBoundSteps[config.highBoundSteps.length - 1];
        double highBound = (1 + Math.floor(result.actualRate / step)) * step;
        log("High-bound found: %s %s (max-rate: %s %s)", roundFormat(highBound), rateUnits, roundFormat(result.actualRate), rateUnits);
        return highBound;
    }

    public double highBoundBySteps(Benchmark benchmark) throws TussleException {
        log("Searching for high-bound by steps...");
        StepRaterConfig config = (StepRaterConfig) this.runnerConfig;
        String rateUnits = benchmark.getConfig().rateUnits;
        int[] steps = config.highBoundSteps;
        double highBound = parseValue(config.highBound);
        int warmupTime = parseTimeLength(config.highBoundWarmupTime);
        int runTime = parseTimeLength(config.highBoundRunTime);
        runTime = runTime > 0 ? runTime : parseTimeLength(config.runTime);
        int stepFactorMax = 10;
        for (int i = 0; i < steps.length; i++) {
            int step = steps[i];
            log("Starting iterating with step %d", step);
            for (int stepFactor = 1; stepFactor <= stepFactorMax; stepFactor++) {
                double targetRate = highBound + (double) step * stepFactor;
                RunArgs runArgs = new RunArgs(targetRate, 0.0, warmupTime, runTime, 0, "find-high-bound");
                RunResult result = runOnce(benchmark, runArgs, false, config.resetEachStep);
                if (result.actualRate <= 0) {
                    log("Failed to find high-bound on this step=%d inc=%d: targetRate %s %s, warmupTime %d s, runTime %d s", step, stepFactor, roundFormat(targetRate), rateUnits, warmupTime, runTime);
                    return -1;
                } else if (result.actualRate * config.rateFactor <= targetRate) {
                    if (stepFactor == 1 && i == steps.length - 1 && highBound == 0) {
                        highBound = step; // 1000
                        log("Using minimal high-bound on this step=%d inc=%d: %s %s", step, stepFactor, roundFormat(highBound), rateUnits);
                    } else {
                        highBound += step * (stepFactor - 1);
                        log("High-bound on this step=%d inc=%d: %s %s", step, stepFactor, roundFormat(highBound), rateUnits);
                    }
                    break;
                }
            }
        }
        log("High-bound found: %s %s", roundFormat(highBound), rateUnits);
        return highBound;
    }

    public void iterateRates(Benchmark benchmark, double highBound, double startingRatePercent, double finishingRatePercent, double ratePercentStep, int finerRateSteps) throws TussleException {
        log("Iterating target rates agains high-bound %s from %s%% to %s%% (d=%s%%)...", roundFormat(highBound), roundFormat(startingRatePercent), roundFormat(finishingRatePercent), roundFormat(ratePercentStep));
        StepRaterConfig runnerConfig = (StepRaterConfig) this.runnerConfig;
        int retry = 0;
        int runTime = parseTimeLength(runnerConfig.runTime);
        int warmupTime = parseTimeLength(runnerConfig.warmupTime);
        String rateUnits = benchmark.getConfig().rateUnits;
        double ratePercent = startingRatePercent;
        boolean reachedFinishingRate = false;
        while (!reachedFinishingRate) {
            double targetRate = (highBound * ratePercent) / 100.0;
            RunArgs runArgs = new RunArgs(targetRate, ratePercent, warmupTime, runTime, retry, "");
            log("Iteration run: %s", runArgs);
            RunResult result = runOnce(benchmark, runArgs, true, runnerConfig.resetEachStep);
            log("Iteration result: %s", result);
            if (result == null) {
                finerRateSteps = 0;
                break;
            }
            if (result.actualRate * runnerConfig.rateFactor <= targetRate) {
                if (retry < runnerConfig.retriesMax) {
                    log("Retrying rate %s %s", roundFormat(targetRate), rateUnits);
                    retry++;
                } else {
                    reachedFinishingRate = true;
                    log("Max rate found: %s %s", roundFormat(targetRate), rateUnits);
                }
            } else if (ratePercent >= finishingRatePercent) {
                reachedFinishingRate = true;
                log("Stopped iterating (high-bound: %s, current ratePercent: %s%%) - reached finishing rate: %s", roundFormat(highBound), roundFormat(ratePercent), roundFormat(finishingRatePercent));
            } else {
                retry = 0;
                ratePercent += ratePercentStep;
            }
        }
        if (finerRateSteps > 0 && ratePercentStep < ratePercent) {
            double finerStartRate = ratePercent - ratePercentStep;
            double finerFinishRate = ratePercent;
            double finerStep = (finerFinishRate - finerStartRate) / (finerRateSteps + 1);
            log("Performing %s between %s%% and %s%% (d=%s%%)...", withS(finerRateSteps, "finer iteration"), roundFormat(finerStartRate), roundFormat(finerFinishRate), roundFormat(finerStep));
            iterateRates(benchmark, highBound, finerStartRate + finerStep, finerFinishRate - finerStep, finerStep, 0);
        }
    }

    /**
     * 
     * StepRater workflow:
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
    @Override
    public void run(Benchmark benchmark) throws TussleException {
        log("Tussle StepRater started");
        StepRaterConfig runnerConfig = (StepRaterConfig) this.runnerConfig;
        log("Benchmark config: %s", new Yaml().dump(benchmark.getConfig()).trim());
        log("Runner config: %s", new Yaml().dump(runnerConfig).trim());
        log("First benchmark reset...");
        benchmark.reset();
        int initialWarmupTime = parseTimeLength(runnerConfig.initialWarmupTime);
        int initialRunTime = parseTimeLength(runnerConfig.initialRunTime);
        if (initialWarmupTime + initialRunTime > 0) {
            double initialTargetRate = parseValue(runnerConfig.initialTargetRate);
            log("Initial warmup run %s %s (%ds+%ds)...", roundFormat(initialTargetRate), benchmark.getConfig().rateUnits, initialWarmupTime, initialRunTime);
            runOnce(benchmark, new RunArgs(initialTargetRate, 0.0, initialWarmupTime, initialRunTime, 0, "initial_warmup"), false, false);
        }
        double highBound = parseValue(runnerConfig.highBound);
        if (highBound == 0 || runnerConfig.highBoundOnly) {
            if (runnerConfig.highBoundFromMaxRate) {
                highBound = highBoundFromMaxRate(benchmark);
            } else {
                highBound = highBoundBySteps(benchmark);
            }
            if (runnerConfig.highBoundOnly) {
                log("Tussle end - found high-bound only");
                return;
            }
        } else {
            log("High-bound from setup %s op/s", roundFormat(highBound));
        }
        runnerConfig.highBound = roundFormat(highBound);
        if (highBound > 0) {
            iterateRates(benchmark, highBound, runnerConfig.startingRatePercent, runnerConfig.finishingRatePercent, runnerConfig.rateStepPercent, runnerConfig.finerRateSteps);
            if (runnerConfig.makeReport) {
                report();
            }
        }
        log("Tussle StepRater finished");
    }
}
