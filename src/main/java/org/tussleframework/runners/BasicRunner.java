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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.HdrHistogram.Histogram;
import org.tussleframework.Benchmark;
import org.tussleframework.RunArgs;
import org.tussleframework.RunResult;
import org.tussleframework.Runner;
import org.tussleframework.TussleException;
import org.tussleframework.metrics.HdrLogWriterTask;
import org.tussleframework.metrics.HdrResult;
import org.tussleframework.metrics.ResultsRecorder;
import org.tussleframework.tools.Analyzer;
import org.tussleframework.tools.AnalyzerConfig;
import org.tussleframework.tools.ConfigLoader;
import org.tussleframework.tools.LoggerTool;
import org.yaml.snakeyaml.Yaml;

public class BasicRunner implements Runner {

    private static final Logger logger = Logger.getLogger(BasicRunner.class.getName());

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", BasicRunner.class.getSimpleName(), String.format(format, args)));
        }
    }

    protected RunnerConfig runnerConfig;
    protected String rateUnits = "op/s";

    public BasicRunner() {
    }

    public BasicRunner(String[] args) throws TussleException {
        init(args);
    }

    @Override
    public void init(String[] args) throws TussleException {
        this.runnerConfig = ConfigLoader.loadConfig(args, true, BasicRunnerConfig.class);
    }

    @Override
    public void run(Benchmark benchmark) throws TussleException {
        BasicRunnerConfig config = (BasicRunnerConfig) this.runnerConfig;
        try {
            log("Benchmark config: %s", new Yaml().dump(benchmark.getConfig()).trim());
            log("Runner config: %s", new Yaml().dump(config).trim());
            double targetRate = parseValue(config.getTargetRate());
            int warmupTime = parseTimeLength(config.getWarmupTime());
            int runTime = parseTimeLength(config.getRunTime());
            ArrayList<HdrResult> results = new ArrayList<>();
            for (int step = 0; step < config.runSteps; step++) {
                runOnce(benchmark, new RunArgs(targetRate, 100, warmupTime, runTime, step), results, true, config.reset);
            }
            makeReport(results);
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
        }
    }

    /**
     * Reset and run benchmark using run once recorder
     */
    public RunResult runOnce(Benchmark benchmark, RunArgs runArgs, List<HdrResult> results, boolean writeHdr, boolean reset) throws TussleException {
        ResultsRecorder recorder = new ResultsRecorder(runnerConfig, runArgs, writeHdr, true);
        RunResult result;
        try {
            result = runOnce(benchmark, runArgs, results, recorder, reset);
        } finally {
            recorder.cancel();
        }
        return result;
    }

    /**
     * Just run benchmark
     */
    public RunResult runOnce(Benchmark benchmark, RunArgs runArgs, Collection<HdrResult> results, ResultsRecorder recorder, boolean reset) throws TussleException {
        log("===================================================================");
        log("Run once: %s (step %d) started", benchmark.getName(), runArgs.step + 1);
        HdrLogWriterTask.progressHeaderPrinted(false);
        if (reset) {
            log("Benchmark reset...");
            benchmark.reset();
        }
        log("Benchmark run at target rate %s %s (%s%%), warmup %d s, run time %d s...", roundFormat(runArgs.targetRate), rateUnits, roundFormat(runArgs.ratePercent), runArgs.warmupTime, runArgs.runTime);
        RunResult result = benchmark.run(runArgs.targetRate, runArgs.warmupTime, runArgs.runTime, recorder);
        rateUnits = result.rateUnits != null ? result.rateUnits : "op/s";
        if (runArgs.ratePercent > 0) {
            log("Reguested rate %s %s (%s%%), actual rate %s %s", roundFormat(runArgs.targetRate), rateUnits, roundFormat(runArgs.ratePercent), roundFormat(result.rate), rateUnits);
        } else {
            log("Reguested rate %s %s, actual rate %s %s", roundFormat(runArgs.targetRate), rateUnits, roundFormat(result.rate), rateUnits);
        }
        log("-----------------------------------------------------");
        if (result.rate < 0) {
            log("Failed to find actual rate: %s", roundFormat(result.rate));
            return null;
        }
        recorder.getResults(results);
        log("Run once: %s (step %d) finished", benchmark.getName(), runArgs.step + 1);
        logResult(result, results, runnerConfig.getHistogramFactor(), runArgs.step);
        return result;
    }

    public void makeReport(Collection<HdrResult> results) throws TussleException {
        if (runnerConfig.isMakeReport()) {
            AnalyzerConfig analyzerConfig = new AnalyzerConfig();
            analyzerConfig.setMakeReport(runnerConfig.isMakeReport());
            analyzerConfig.setResultsDir(runnerConfig.getHistogramsDir());
            analyzerConfig.setReportDir(runnerConfig.getReportDir());
            try {
                new Analyzer().processResults(analyzerConfig, results);
            } catch (Exception e) {
                log("Analyzer failed to process results: %s", e.toString());
            }
        }
    }

    public void logResult(RunResult runResult, Collection<HdrResult> results, double histogramFactor, int step) {
        double[] basicPercentiles = { 0, 50, 90, 99, 99.9, 99.99, 100 };
        log("Results (step %d)", step + 1);
        log("Count: %d", runResult.count);
        log("Time: %s s", roundFormat(runResult.time / 1000d));
        log("Rate: %s %s", roundFormat(runResult.rate), runResult.rateUnits != null ? runResult.rateUnits : "");
        log("Errors: %d", runResult.errors);
        results.forEach(result -> {
            Histogram h = result.allHistogram;
            if (h.getTotalCount() > 0) {
                for (int i = 0; i < basicPercentiles.length; i++) {
                    log("%s %s p%s: %s ms", result.operationName, result.metricName, roundFormat(basicPercentiles[i]), roundFormat(h.getValueAtPercentile(basicPercentiles[i]) / histogramFactor), result.timeUnits);
                }
                log("%s %s mean: %s ms", result.operationName, result.metricName, roundFormat(h.getMean() / histogramFactor), result.timeUnits);
            }
        });
    }
}
