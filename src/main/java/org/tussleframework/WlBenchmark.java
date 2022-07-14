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

package org.tussleframework;

import java.util.logging.Level;

import org.tussleframework.tools.ConfigLoader;
import org.tussleframework.tools.TargetRunner;
import org.tussleframework.tools.TargetRunnerAsync;
import org.tussleframework.tools.TargetRunnerMT;
import org.tussleframework.tools.TargetRunnerST;

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

    protected WlConfig config;

    public void init(String[] args) throws TussleException {
        config = ConfigLoader.loadConfig(args, true, getConfigClass());
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
    public void reset() throws TussleException {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public RunResult run(double targetRate, int warmupTime, int runTime, TimeRecorder recorder) throws TussleException {
        if (warmupTime > 0) {
            doSomeWork(targetRate, warmupTime, null);
        }
        return doSomeWork(targetRate, runTime, recorder);
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

    protected RunResult doSomeWork(double targetRate, int runTime, TimeRecorder recorder) throws TussleException {
        if (recorder != null) {
            recorder.startRecording(getOperationName(), "op/s", "ms");
        }
        RunResult result = getTargetRunner().runWorkload(getOperationName(), targetRate, runTime * 1000, getWorkload(), recorder);
        if (recorder != null) {
            recorder.stopRecording();
        }
        result.rateUnits = config.rateUnits;
        result.timeUnits = config.timeUnits;
        return result;
    }
}
