/*
 * Copyright (c) 2021-2023, Azul Systems
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

package org.tussleframework.tools;

import java.io.File;

import org.tussleframework.BasicProperties;
import org.tussleframework.HdrConfig;
import org.tussleframework.metrics.HdrResult;
import org.tussleframework.metrics.Interval;
import org.tussleframework.metrics.MovingWindowSLE;

public class AnalyzerConfig extends HdrConfig {
    public boolean doc = true;
    public boolean makeReport = false;
    public boolean saveMetrics = true;
    public boolean allPercentiles = true;
    public String reportDir = "./report";
    public String highBound = "0";
    public Interval[] intervals = {};
    public MovingWindowSLE[] sleConfig = {};
    public String[] sleFor = { HdrResult.RESPONSE_TIME };
    public BasicProperties processors;

    public AnalyzerConfig() {
    }

    public AnalyzerConfig(HdrConfig config) {
        copy(config);
    }

    @Override
    public void validate(boolean runMode) {
        super.validate(runMode);
        if (FormatTool.parseValue(highBound) < 0) {
            throw new IllegalArgumentException(String.format("Invalid highBound(%s) - should be non-negative", highBound));
        }
        if (runMode) {
            FileTool.backupAndCreateDir(new File(histogramsDir));
            if (makeReport) {
                FileTool.backupAndCreateDir(new File(reportDir));
            }
        }
    }
}
