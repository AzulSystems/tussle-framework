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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.tussleframework.tools.LoggerTool;
import org.tussleframework.tools.StepRaterAnalyser;

public class StepRateAnalyserTest {

    {
        LoggerTool.init("", "java.util.logging.ConsoleHandler");
    }

    @Test
    public void testAnalyser() {
        String resultsDir = "test_data/step_rate_analyser_test";
        String reportDir = "results/step_rate_analyser_test/report" + System.currentTimeMillis();
        try {
            Files.createDirectories(Paths.get(reportDir));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        String[] args = {
                "makeReport=true",
                "-p", "histogramsDir = " + resultsDir,
                "-p", "reportDir = " + reportDir,
                "-p", "highBound = 100000",
                "-p", "sleConfig = [[50, 1, 10], [99, 10, 10], [99.9, 50, 60], [99.99, 200, 120], [100, 1000, 120]]",
                "rateUnits=operations/sec",
                "sleFor=[.*]"
        };
        try {
            StepRaterAnalyser.main(args);
            assertTrue("metrics.json should be created", Files.exists(Paths.get(resultsDir, "metrics.json")));
            assertTrue("index.html should be created", Files.exists(Paths.get(reportDir, "index.html")));
        } catch (Throwable e) {
            e.printStackTrace();
            fail();
        }
    }
}
