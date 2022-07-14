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
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tussleframework.Benchmark;
import org.tussleframework.RunArgs;
import org.tussleframework.RunParams;
import org.tussleframework.TussleException;
import org.tussleframework.metrics.HdrResult;
import org.tussleframework.metrics.ResultsRecorder;
import org.tussleframework.tools.ConfigLoader;
import org.tussleframework.tools.LoggerTool;
import org.yaml.snakeyaml.Yaml;

public class InteractiveRunner extends BasicRunner {

    private static final Logger logger = Logger.getLogger(InteractiveRunner.class.getName());

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[%s] %s", InteractiveRunner.class.getSimpleName(), String.format(format, args)));
        }
    }

    public InteractiveRunner() {
    }

    public InteractiveRunner(String[] args) throws TussleException {
        init(args);
    }

    @Override
    public void init(String[] args) throws TussleException {
        this.runnerConfig = ConfigLoader.loadConfig(args, true, InteractiveRunnerConfig.class);
    }

    protected void init(Benchmark benchmark, String line) {
        log("Performing Benchmark init...");
        try {
            String[] args = line.split("\\s+");
            args = Arrays.copyOfRange(args, 1, args.length);
            benchmark.init(args);
            log("New Benchmark config: %s", new Yaml().dump(benchmark.getConfig()).trim());
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
        }
    }

    protected void reset(Benchmark benchmark) {
        log("Performing Benchmark reset...");
        try {
            benchmark.reset();
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
        }
    }

    protected int runArg(Benchmark benchmark, String line, int step, Collection<HdrResult> results, ResultsRecorder recorder) {
        log("Performing Benchmark run with args: %s...", line);
        try {
            RunParams params = ConfigLoader.loadConfig(new String[] { "-s", line }, true, RunParams.class);
            RunArgs runArgs = new RunArgs(parseValue(params.targetRate), 100, parseTimeLength(params.warmupTime), parseTimeLength(params.runTime), step, "run");
            runOnce(benchmark, runArgs, results, recorder, false);
            return 1;
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
            return 0;
        }        
    }

    @Override
    public void run(Benchmark benchmark) throws TussleException {
        InteractiveRunnerConfig config = (InteractiveRunnerConfig) this.runnerConfig;
        ResultsRecorder recorder = new ResultsRecorder(runnerConfig, new RunArgs(0, 0, 0, 0, 0, ""), true, false);
        log("Running benchmark interactively: %s", benchmark.getName());
        log("Benchmark config: %s", new Yaml().dump(benchmark.getConfig()).trim());
        log("Runner config: %s", new Yaml().dump(config).trim());
        if (config.reset) {
            log("Performing initial Benchmark reset...");
            benchmark.reset();
        }
        try (Scanner scanner = new Scanner(System.in)) {
            ArrayList<HdrResult> results = new ArrayList<>();
            int step = 0;
            while (true) {
                String line = scanner.nextLine();
                line = line != null ? line.trim() : null;
                if (line == null || line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("stop") || line.equalsIgnoreCase("exit")) {
                    log("Done");
                    break;
                }
                String args = line.toLowerCase();
                if (args.startsWith("reset")) {
                    reset(benchmark);
                } else if (args.startsWith("init")) {
                    init(benchmark, line);
                } else if (!line.isEmpty()) {
                    step += runArg(benchmark, line, step, results, recorder);
                }
            }
            makeReport(results);
        } finally {
            recorder.cancel();
        }
    }
}
