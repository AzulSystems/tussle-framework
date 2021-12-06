package org.benchmarks;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import org.benchmarks.tools.ConfigLoader;

/**
 * 
 * @author rus
 *
 */
public abstract class WlBenchmark implements Benchmark {

    private final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(WlBenchmark.class.getName());

    public final void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", this.getClass().getName(), String.format(format, args)));
        }
    }

    protected AtomicLong totalCount = new AtomicLong();
    protected AtomicLong totalErrors = new AtomicLong();
    protected WlConfig config;

    public void init(String[] args) throws Exception {
        config = ConfigLoader.load(args, true, getConfigClass());
    }

    public abstract RunnableWithError getWorkload();
    public abstract String getOperationName();

    @Override
    public BenchmarkConfig getConfig() {
        return config;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public void reset() throws Exception {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public RunResult run(double targetRate, int warmupTime, int runTime, TimeRecorder recorder) {
        try {
            if (warmupTime > 0) {
                doSomeWork(targetRate, warmupTime, null);
            }
            return doSomeWork(targetRate, runTime, recorder);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RunResult.builder().runError(e).build(); 
        }
    }

    public Class<? extends WlConfig> getConfigClass() {
        return WlConfig.class;
    }

    public TargetRunner getTargetRunner() {
        if (config.asyncMode) {
            return new TargetRunnerAsync(config.threads);
        } else if (config.threads > 1) {
            return new TargetRunnerMT(config.threads);
        } else {
            return new TargetRunnerST();
        }
    }

    protected RunResult doSomeWork(double targetRate, int runTime, TimeRecorder recorder) throws InterruptedException {
        int durationMs = runTime * 1000;
        RunResult runResult = getTargetRunner().runWorkload(getOperationName(), targetRate, durationMs, getWorkload(), recorder);
        log(" -- check count: %d - %d", runResult.getCount(), totalCount.get());
        log(" -- check errors: %d - %d", runResult.getErrors(), totalErrors.get());
        return runResult;
    }
}
