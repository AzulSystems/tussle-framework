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
                RunResult res;
                try {
                    log("Reguesting rate %s, warmup %ds, time %ds...", roundFormat(targetRate), warmupTime, runTime);
                    res = benchmark.run(targetRate, warmupTime, runTime, resultsRecorder);
                } finally {
                    resultsRecorder.cancel();
                }
                log("Run finished: %s", benchmark.getName());
                logResult(res, resultsRecorder, benchmarkConfig.getHistogramFactor(), step);
            }
            benchmarkPassed = true;
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
        }
        benchmark.cleanup();
        if (benchmarkPassed && benchmarkConfig.isMakeReport()) {
            AnalyzerConfig analyzerConfig = new AnalyzerConfig();
            analyzerConfig.setMakeReport(true);
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
