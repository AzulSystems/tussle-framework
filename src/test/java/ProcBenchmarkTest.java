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

import static org.junit.Assert.fail;

import org.junit.Test;
import org.tussleframework.ProcBenchmark;
import org.tussleframework.ProcConfig;
import org.tussleframework.runners.BasicRunner;
import org.tussleframework.tools.LoggerTool;

public class ProcBenchmarkTest {

    {
        LoggerTool.init("", "java.util.logging.ConsoleHandler");
    }

    @Test
    public void testBasic() {
        try {
            String[] runnerArgs = {
                    "targetRate=123",
                    "runTime=2s",
//                    "makeReport=true",
//                    "histogramsDir=results/proc_benchmark_test/histograms",
//                    "reportDir=results/proc_benchmark_test/report"
            };
            String[] runCmd = {
                    "bash",
                    "test_run_script.sh",
                    "run",
                    "STEP={runStep}",
                    "TIME={runTime}",
                    "WARMUP={warmupTime}",
                    "TARGET={targetRate}",
            };
            String[] runEnv = {
                    "TEST_VAR=_TEST_",
                    "TARGET={targetRate}"
            };
            String[] initCmd = {
                    "bash",
                    "test_run_script.sh",
                    "init",
            };
            String[] cleanupCmd = {
                    "bash",
                    "test_run_script.sh",
                    "cleanup",
            };
            ProcConfig procConfig = new ProcConfig();
            procConfig.name = "proc-test";
            procConfig.logPrefix = "-> ";
            procConfig.run.dir = "test_data/proc_benchmark_test";
            procConfig.run.cmd = runCmd;
            procConfig.run.env = runEnv;
            procConfig.init.dir = procConfig.run.dir;
            procConfig.init.cmd = initCmd;
            procConfig.cleanup.dir = procConfig.run.dir;
            procConfig.cleanup.cmd = cleanupCmd;
            new BasicRunner(runnerArgs).run(new ProcBenchmark(procConfig));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
