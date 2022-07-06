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

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tussleframework.Benchmark;
import org.tussleframework.RunArgs;
import org.tussleframework.RunParams;
import org.tussleframework.TussleException;
import org.tussleframework.metrics.HdrWriter;
import org.tussleframework.metrics.HdrResult;
import org.tussleframework.metrics.ResultsRecorder;
import org.tussleframework.tools.ConfigLoader;
import org.tussleframework.tools.LoggerTool;
import org.yaml.snakeyaml.Yaml;

public class ScenarioRunner extends BasicRunner {

    private static final Logger logger = Logger.getLogger(ScenarioRunner.class.getName());

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", ScenarioRunner.class.getSimpleName(), String.format(format, args)));
        }
    }

    public ScenarioRunner() {
    }

    public ScenarioRunner(String[] args) throws TussleException {
        init(args);
    }

    @Override
    public void init(String[] args) throws TussleException {
        this.runnerConfig = ConfigLoader.loadConfig(args, true, ScenarioRunnerConfig.class);
    }

    @Override
    public void run(Benchmark benchmark) throws TussleException {
        ScenarioRunnerConfig config = (ScenarioRunnerConfig) this.runnerConfig;
        RunParams[] scenario = config.getScenario();
        int runTimeSum = 0;
        for (int runStep = 0; runStep < scenario.length; runStep++) {
            runTimeSum += parseTimeLength(scenario[runStep].getRunTime());
        }
        ResultsRecorder recorder = new ResultsRecorder(runnerConfig, new RunArgs(0, 100, 0, runTimeSum, 0), true, false);
        log("Benchmark config: %s", new Yaml().dump(benchmark.getConfig()).trim());
        log("Runner config: %s", new Yaml().dump(config).trim());
        try {
            ArrayList<HdrResult> results = new ArrayList<>();
            if (config.reset) {
                log("Benchmark initial reset...");
                benchmark.reset();
            }
            for (int runStep = 0; runStep < scenario.length; runStep++) {
                log("===================================================================");
                log("Benchmark: %s (step %d)", benchmark.getName(), runStep + 1);
                HdrWriter.progressHeaderPrinted(false);
                double targetRate = parseValue(scenario[runStep].getTargetRate());
                int warmupTime = parseTimeLength(scenario[runStep].getWarmupTime());
                int runTime = parseTimeLength(scenario[runStep].getRunTime());
                RunArgs runArgs = new RunArgs(targetRate, 100, warmupTime, runTime, runStep);
                if (config.separateSteps) {
                    recorder = new ResultsRecorder(runnerConfig, runArgs, true, false);
                }
                runOnce(benchmark, runArgs, results, recorder, false);
                recorder.clearResults();
            }
            makeReport(results);
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
        } finally {
            recorder.cancel();
        }
    }
}
