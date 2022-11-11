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
    protected Collection<HdrResult> collectedHdrResults = new ArrayList<>();

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
        for (int runStep = 0; runStep < config.runSteps; runStep++) {
            runOnce(benchmark, new RunArgs(targetRate, 100, warmupTime, runTime, runStep, ""), true, config.reset);
        }
        if (runnerConfig.makeReport) {
            report();
        }
    }
    
    @Override
    public void report() throws TussleException {
        if (collectedHdrResults.isEmpty()) {
            return;
        }
        AnalyzerConfig analyzerConfig = new AnalyzerConfig();
        analyzerConfig.copy(runnerConfig);
        analyzerConfig.reportDir = runnerConfig.reportDir;
        analyzerConfig.highBound = runnerConfig.highBound;
        analyzerConfig.sleConfig = runnerConfig.sleConfig;
        analyzerConfig.makeReport = true;
        try {
            new Analyzer().processResults(analyzerConfig, collectedHdrResults);
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
        }
    }

    /**
     * Reset and run benchmark using run once recorder
     */
    public RunResult runOnce(Benchmark benchmark, RunArgs runArgs, boolean collect, boolean reset) throws TussleException {
        HdrWriter.resetProgressHeader();
        ResultsRecorder recorder = new ResultsRecorder(runnerConfig, runArgs, collect, true);
        try {
            return runOnce(benchmark, runArgs, collect, recorder, reset);
        } finally {
            recorder.cancel();
        }
    }

    /**
     * Just run benchmark
     */
    public RunResult runOnce(Benchmark benchmark, RunArgs runArgs, boolean collect, ResultsRecorder recorder, boolean reset) throws TussleException {
        log("===================================================================");
        log("Run once: %s (step %d) started", benchmark.getName(), runArgs.runStep + 1);
        if (reset) {
            log("Benchmark reset...");
            benchmark.reset();
        }
        benchmark.getConfig().runName = runArgs.name; 
        String rateUnits = benchmark.getConfig().rateUnits;
        String timeUnits = benchmark.getConfig().timeUnits;
        log("Benchmark run at %s...", runArgs.format(rateUnits));
        RunResult runResult = benchmark.run(runArgs.targetRate, runArgs.warmupTime, runArgs.runTime, recorder);
        Collection<HdrResult> newHdrResults = recorder.getHdrResults();
        if (!newHdrResults.isEmpty()) {
            if (collect) {
                collectedHdrResults.addAll(newHdrResults);
            }
            if (runResult.actualRate <= 0) {
                RunResult runResultSummary = HdrResult.getSummaryResult(newHdrResults);
                runResult.actualRate = runResultSummary.actualRate;
                runResult.time = runResultSummary.time;
            }
        }
        if (runResult.rateUnits == null) {
            runResult.rateUnits = rateUnits;
        }
        if (runResult.timeUnits == null) {
            runResult.timeUnits = timeUnits;
        }
        if (runArgs.ratePercent > 0) {
            log("Reguested rate %s %s (%s%%) , actual rate %s %s", roundFormat(runArgs.targetRate), rateUnits, roundFormat(runArgs.ratePercent), roundFormat(runResult.actualRate), rateUnits);
        } else {
            log("Reguested rate %s %s, actual rate %s %s", roundFormat(runArgs.targetRate), rateUnits, roundFormat(runResult.actualRate), rateUnits);
        }
        log("-----------------------------------------------------");
        log("Run once: %s (step %d) finished", benchmark.getName(), runArgs.runStep + 1);
        logResult(runResult, runArgs.runStep);
        logResults();
        return runResult;
    }

    public void logResult(RunResult runResult, int step) {
        log("Results (step %d)", step + 1);
        log("Count: %d", runResult.getCount());
        log("Time: %s s", roundFormat(runResult.time / 1000d));
        log("Rate: %s %s", roundFormat(runResult.actualRate), runResult.getRateUnits());
        log("Errors: %d", runResult.getErrors());
    }

    public void logResults() {
        collectedHdrResults.stream().filter(r -> r.getCount() > 0).forEach(result -> {
            log(result.formatTime());
            double[] percentiles = runnerConfig.logPercentiles;
            for (int i = 0; i < percentiles.length; i++) {
                log(result.formatPercentile(percentiles[i]));
            }
            log(result.formatMean());
            log(result.formatRate());
        });
    }
}
