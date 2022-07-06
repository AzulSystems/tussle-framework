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

import java.io.File;

import org.tussleframework.HdrConfig;
import org.tussleframework.metrics.MovingWindowSLE;
import org.tussleframework.tools.FileTool;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Basic benchmark configuration
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RunnerConfig extends HdrConfig {
    public boolean reset = true;            // reset benchmark before run in the Runner scenario
    public boolean rawData = false;         // collect each request raw data: start and finish times
    public boolean makeReport = false;      // generate detailed report in addition to the summary results printed to log
    public boolean serviceTimeOnly = false; // collect service-time or service-time+response-time
    public String reportDir = "./report";   // location for report files
    public String[] collectOps = {};        // if set collect metrics for only specified operations
    public double[] logPercentiles = { 0, 50, 90, 99, 99.9, 99.99, 100 };
    public MovingWindowSLE[] sleConfig = {};

    @Override
    public void validate(boolean runMode) {
        super.validate(runMode);
        if (runMode) {
            FileTool.backupAndCreateDir(new File(histogramsDir));
            if (makeReport) {
                FileTool.backupAndCreateDir(new File(reportDir));;
            }
        }
    }
}
