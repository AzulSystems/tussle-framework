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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tussleframework.Benchmark;
import org.tussleframework.RunArgs;
import org.tussleframework.RunResult;
import org.tussleframework.Runner;
import org.tussleframework.TussleException;
import org.tussleframework.metrics.HdrResult;
import org.tussleframework.metrics.HdrWriter;
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

    public BasicRunner() {
    }

    public BasicRunner(String[] args) throws TussleException {
        init(args);
    }

    public BasicRunner(BasicRunnerConfig runnerConfig) {
        runnerConfig.validate(true);
        this.runnerConfig = runnerConfig;
    }

    @Override
    public void init(String[] args) throws TussleException {
        this.runnerConfig = ConfigLoader.loadConfig(args, true, BasicRunnerConfig.class);
    }

    @Override
    public void run(Benchmark benchmark) throws TussleException {
        BasicRunnerConfig config = (BasicRunnerConfig) this.runnerConfig;
        log("Benchmark config: %s", new Yaml().dump(benchmark.getConfig()).trim());
        log("Runner config: %s", new Yaml().dump(config).trim());
        double targetRate = parseValue(config.targetRate);
        int warmupTime = parseTimeLength(config.warmupTime);
        int runTime = parseTimeLength(config.runTime);
        ArrayList<HdrResult> results = new ArrayList<>();
        for (int runStep = 0; runStep < config.runSteps; runStep++) {
            runOnce(benchmark, new RunArgs(targetRate, 100, warmupTime, runTime, runStep, ""), results, true, config.reset);
        }
        makeReport(results);
    }

    /**
     * Reset and run benchmark using run once recorder
     */
    public RunResult runOnce(Benchmark benchmark, RunArgs runArgs, Collection<HdrResult> hdrResults, boolean writeHdr, boolean reset) throws TussleException {
        HdrWriter.progressHeaderPrinted(false);
        ResultsRecorder recorder = new ResultsRecorder(runnerConfig, runArgs, writeHdr, true);
        try {
            return runOnce(benchmark, runArgs, hdrResults, recorder, reset);
        } finally {
            recorder.cancel();
        }
    }

    /**
     * Just run benchmark
     */
    public RunResult runOnce(Benchmark benchmark, RunArgs runArgs, Collection<HdrResult> hdrResults, ResultsRecorder recorder, boolean reset) throws TussleException {
        log("===================================================================");
        log("Run once: %s (step %d) started", benchmark.getName(), runArgs.runStep + 1);
        if (reset) {
            log("Benchmark reset...");
            benchmark.reset();
        }
        benchmark.getConfig().runName = runArgs.name; 
        String rateUnits = benchmark.getConfig().rateUnits;
        String timeUnits = benchmark.getConfig().timeUnits;
        log("Benchmark run at target rate %s %s (%s%%), warmup %d s, run time %d s...", roundFormat(runArgs.targetRate), rateUnits, roundFormat(runArgs.ratePercent), runArgs.warmupTime, runArgs.runTime);
        RunResult runResult = benchmark.run(runArgs.targetRate, runArgs.warmupTime, runArgs.runTime, recorder);
        Collection<HdrResult> newHdrResults = recorder.getHdrResults();
        if (!newHdrResults.isEmpty()) {
            if (hdrResults != null) {
                hdrResults.addAll(newHdrResults);
            }
            runResult = HdrResult.getSummaryResult(newHdrResults);
        }
        if (runResult.rateUnits == null) {
            runResult.rateUnits = rateUnits;
        }
        if (runResult.timeUnits == null) {
            runResult.timeUnits = timeUnits;
        }
        if (runArgs.ratePercent > 0) {
            log("Reguested rate %s %s (%s%%), actual rate %s %s", roundFormat(runArgs.targetRate), rateUnits, roundFormat(runArgs.ratePercent), roundFormat(runResult.rate), rateUnits);
        } else {
            log("Reguested rate %s %s, actual rate %s %s", roundFormat(runArgs.targetRate), rateUnits, roundFormat(runResult.rate), rateUnits);
        }
        log("-----------------------------------------------------");
        log("Run once: %s (step %d) finished", benchmark.getName(), runArgs.runStep + 1);
        logResult(runResult, runArgs.runStep);
        logResults(newHdrResults);
        return runResult;
    }

    public void makeReport(Collection<HdrResult> hdrResults) throws TussleException {
        if (hdrResults !=  null && runnerConfig.isMakeReport()) {
            AnalyzerConfig analyzerConfig = new AnalyzerConfig();
            analyzerConfig.copy(runnerConfig);
            analyzerConfig.reportDir = runnerConfig.reportDir;
            analyzerConfig.makeReport = true;
            try {
                new Analyzer().processResults(analyzerConfig, hdrResults);
            } catch (Exception e) {
                LoggerTool.logException(logger, e);
            }
        }
    }

    public void logResult(RunResult runResult, int step) {
        log("Results (step %d)", step + 1);
        log("Count: %d", runResult.count);
        log("Time: %s s", roundFormat(runResult.time / 1000d));
        log("Rate: %s %s", roundFormat(runResult.rate), runResult.rateUnits);
        log("Errors: %d", runResult.errors);
    }

    public void logResults(Collection<HdrResult> hdrResults) {
        if (hdrResults == null) {
            return;
        }
        hdrResults.stream().filter(r -> r.getCount() > 0).forEach(result -> {
            log("%s %s time: %d s", result.operationName(), result.metricName(), result.getTimeMs() / 1000L);
            double[] percentiles = runnerConfig.logPercentiles;
            for (int i = 0; i < percentiles.length; i++) {
                log("%s %s p%s: %s %s", result.operationName(), result.metricName(), roundFormat(percentiles[i]), roundFormat(result.getValueAtPercentile(percentiles[i])), result.timeUnits());
            }
            log("%s %s mean: %s %s", result.operationName(), result.metricName(), roundFormat(result.getMean()), result.timeUnits());
            log("%s %s rate: %s %s", result.operationName(), result.metricName(), roundFormat(result.getRate()), result.rateUnits());
        });
    }
}
