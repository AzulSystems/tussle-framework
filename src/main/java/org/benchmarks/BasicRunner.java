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

package org.benchmarks;

import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.HdrHistogram.Histogram;
import org.benchmarks.tools.Analyzer;
import org.benchmarks.tools.AnalyzerConfig;
import org.benchmarks.tools.LoggerTool;
import org.yaml.snakeyaml.Yaml;

import static org.benchmarks.tools.FormatTool.*;

public class BasicRunner {

    private static final Logger logger = Logger.getLogger(BasicRunner.class.getName());

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", BasicRunner.class.getSimpleName(), String.format(format, args)));
        }
    }

    public void run(Class<? extends Benchmark> benchmarkClass, String[] args) {
        Benchmark benchmark = null;
        try {
            Constructor<? extends Benchmark> benchmarkCtor = benchmarkClass.getConstructor();
            benchmark = benchmarkCtor.newInstance();
            benchmark.init(args);
            run(benchmark);
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
        }
    }

    public void run(Benchmark benchmark) {
        BenchmarkConfig benchmarkConfig = benchmark.getConfig();
        boolean benchmarkPassed = false;
        try {
            log("Test config: %s", new Yaml().dump(benchmarkConfig).trim());
            double targetRate = parseValue(benchmarkConfig.getTargetRate());
            int warmupTime = parseTimeLength(benchmarkConfig.getWarmupTime());
            int runTime = parseTimeLength(benchmarkConfig.getRunTime());
            int steps = benchmarkConfig.getRunSteps();
            for (int step = 0; step < steps; step++) {
                log("Benchmark: %s (step: %d)", benchmark.getName(), step + 1);
                if (benchmarkConfig.isReset()) {
                    log("Resetting benchmark: %s", benchmark.getName());
                    benchmark.reset();
                }
                ResultsRecorder resultsRecorder = new ResultsRecorder(benchmarkConfig, 0, 100, targetRate, step, runTime);
                RunResult runResult;
                try {
                    log("Reguesting rate %s, warmup %ds, time %ds...", roundFormat(targetRate), warmupTime, runTime);
                    runResult = benchmark.run(targetRate, warmupTime, runTime, resultsRecorder);
                } finally {
                    resultsRecorder.cancel();
                }
                log("Run finished: %s", benchmark.getName());
                logResult(runResult, resultsRecorder, benchmarkConfig.getHistogramFactor(), step);
            }
            benchmarkPassed = true;
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
        }
        benchmark.cleanup();
        if (benchmarkPassed && benchmarkConfig.isMakeReport()) {
            AnalyzerConfig analyzerConfig = new AnalyzerConfig();
            analyzerConfig.setMakeReport(benchmarkConfig.isMakeReport());
            analyzerConfig.setResultsDir(benchmarkConfig.getHistogramsDir());
            analyzerConfig.setReportDir(benchmarkConfig.getReportDir());
            try {
                new Analyzer().processResults(analyzerConfig);
            } catch (Exception e) {
            }
        }
    }

    public void logResult(RunResult runResult, ResultsRecorder resultsRecorder, double histogramFactor, int step) {
        double[] basicPercentiles = { 0, 50, 90, 99, 99.9, 99.99, 100 };
        log("Results (%d):", step + 1);
        log("Count: %d", runResult.count);
        log("Time: %s s", roundFormat(runResult.time / 1000d));
        log("Rate: %s %s", roundFormat(runResult.rate), runResult.rateUnits != null ? runResult.rateUnits : "");
        log("Errors: %d", runResult.errors);
        Histogram h1 = resultsRecorder.responseTimeWriter.getAllHistogram();
        if (h1.getTotalCount() > 0) {
            for (int i = 0; i < basicPercentiles.length; i++) {
                log("%sth percentile response time: %s ms", roundFormat(basicPercentiles[i]), roundFormat(h1.getValueAtPercentile(basicPercentiles[i]) / histogramFactor));
            }
            log("Mean response time: %s ms", roundFormat(h1.getMean() / histogramFactor));
        }
        Histogram h2 = resultsRecorder.serviceTimeWriter.getAllHistogram();
        if (h2.getTotalCount() > 0) {
            for (int i = 0; i < basicPercentiles.length; i++) {
                log("%sth percentile service time: %s ms", roundFormat(basicPercentiles[i]), roundFormat(h2.getValueAtPercentile(basicPercentiles[i]) / histogramFactor));
            }
            log("Mean service time: %s ms", roundFormat(h2.getMean() / histogramFactor));
        }
    }
}
