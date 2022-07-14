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

import static org.tussleframework.tools.FormatTool.applyArgs;
import static org.tussleframework.tools.FormatTool.roundFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.tussleframework.tools.ConfigLoader;
import org.tussleframework.tools.FileTool;
import org.tussleframework.tools.FormatTool;
import org.tussleframework.tools.JsonTool;
import org.tussleframework.tools.LoggerTool;
import org.tussleframework.tools.ProcTool;
import org.tussleframework.tools.ProcTool.CmdVars;

public class ProcBenchmark implements Benchmark {

    private static void log(String format, Object... args) {
        LoggerTool.log(ProcBenchmark.class.getSimpleName(), format, args);
    }

    private OutputStream procLog;
    private ProcConfig config;
    private int runStep;

    public ProcBenchmark() {
    }

    public ProcBenchmark(String[] args) throws TussleException {
        init(args);
    }

    public ProcBenchmark(ProcConfig config) throws TussleException {
        config.validate(true);
        this.config = config;
        initCmd();
    }

    @Override
    public String getName() {
        return config.name;
    }

    @Override
    public BenchmarkConfig getConfig() {
        return config;
    }

    @Override
    public void init(String[] args) throws TussleException {
        config = ConfigLoader.loadConfig(args, true, ProcConfig.class);
        initCmd();
    }

    protected void initCmd() throws TussleException {
        procLog = new LoggerTool.LogOutputStream(config.logPrefix != null ? config.logPrefix : String.format("[%s] ", config.name), config.logSuffix != null ? config.logSuffix : "");
        if (config.init != null && !config.init.isEmpty()) {
            log(" --- BENCHMARK PROCESS INIT --- ");
            ProcTool.runProcess("init", config.makeCmd(config.init, config.getArgs(new RunArgs())), procLog);
        }
    }

    @Override
    public void reset() throws TussleException {
        if (config.reset != null && !config.reset.isEmpty()) {
            log(" --- BENCHMARK PROCESS RESET --- ");
            ProcTool.runProcess("reset", config.makeCmd(config.reset, config.getArgs(new RunArgs())), procLog);
        }
    }

    @Override
    public void cleanup() throws TussleException {
        if (config.cleanup != null && !config.cleanup.isEmpty()) {
            log(" --- BENCHMARK PROCESS CLEANUP --- ");
            ProcTool.runProcess("cleanup", config.makeCmd(config.cleanup, config.getArgs(new RunArgs())), procLog);
        }
    }

    @Override
    public RunResult run(double targetRate, int warmupTime, int runTime, TimeRecorder recorder) throws TussleException {
        log(" --- BENCHMARK PROCESS RUN --- ");
        RunArgs runArgs = new RunArgs(targetRate, 0, warmupTime, runTime, runStep, config.runName);
        try {
            int delay = FormatTool.parseTimeLength(config.run.delay);
            log("Starting process run command targetRate %s, warmup %ds, duration %ds, delay %ds, step %d", roundFormat(targetRate), warmupTime, runTime, delay, runStep);
            Map<String, String> params = config.getArgs(runArgs);
            CmdVars runCmd = config.makeCmd(config.run, params).setDelay(warmupTime + runTime + delay);
            ProcTool.runProcess("run", runCmd, procLog);
            Collection<String> resultFileNames = applyArgs(Arrays.asList(config.resultFiles.clone()), params);
            if (resultFileNames != null && !resultFileNames.isEmpty()) {
                resultFileNames.forEach(fileName -> log("Looking for result file(s) '%s', runDir '%s'", fileName, runCmd.dir));
                Collection<File> resultFiles = FileTool.listFiles(runCmd.dir, resultFileNames);
                resultFiles.forEach(file -> log("Found file '%s'", file));
                recorder.addResults(resultFiles, config.rateUnits, config.timeUnits);
            }
            RunResult runResult = new RunResult();
            if (config.runResult != null && !config.runResult.isEmpty()) {
                File resultFile = runCmd.dir != null ? new File(runCmd.dir, config.runResult) : new File(config.runResult);
                try (FileInputStream inputStream = new FileInputStream(resultFile)) {
                    runResult = JsonTool.readJson(inputStream, RunResult.class, false, false);
                } catch (Exception e) {
                    throw new TussleException(e);
                }
            }
            return runResult;
        } finally {
            runStep++;
        }
    }
}
