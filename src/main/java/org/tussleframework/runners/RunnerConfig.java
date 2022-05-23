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

import org.tussleframework.AbstractConfig;
import org.tussleframework.tools.FileTool;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Basic benchmark configuration
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class RunnerConfig implements AbstractConfig {
    public int intervalLength = 1000;  // ms, histogram write interval length
    public int progressIntervals = 5;  // ms, output progress interval count
    public double histogramFactor = 1000000; // histogram's units divider to milliseconds, e.g. for ns-to-ms it is 1000000
    public boolean reset = true;       // reset benchmark before run in the Runner scenario
    public boolean rawData = false;    // collect each request raw data: start and finish times
    public boolean makeReport = false; // generate detailed report in addition to the summary results printed to log
    public boolean serviceTimeOnly = false; // collect service-time or service-time+response-time
    public String reportDir = "./report"; // location for report files
    public String histogramsDir = "./histograms"; // location for histogram (hdr) files
    public String[] collectOps = {};   // if set collect metrics for only specified operations

    @Override
    public void validate(boolean runMode) {
        if (histogramsDir == null) {
            throw new IllegalArgumentException("Invalid histogramsDir - null");
        }
        if (runMode) {
            createDirs();
        }
    }

    public void createDirs() {
        File histogramsDirFile = new File(histogramsDir);
        File reportDirFile = new File(reportDir);
        if (FileTool.isFileOrNonEmptyDir(histogramsDirFile) && !FileTool.backupDir(histogramsDirFile)) {
            throw new IllegalArgumentException(String.format("Non-empty histograms dir '%s' already exists", histogramsDirFile));
        }
        if (makeReport && FileTool.isFileOrNonEmptyDir(reportDirFile) && !FileTool.backupDir(reportDirFile)) {
            throw new IllegalArgumentException(String.format("Non-empty report dir '%s' already exists", reportDirFile));
        }
        if (!histogramsDirFile.exists() && !histogramsDirFile.mkdirs()) {
            throw new IllegalArgumentException(String.format("Failed to create histograms dir '%s'", histogramsDirFile));
        }
        if (makeReport && !reportDirFile.exists() && !reportDirFile.mkdirs()) {
            throw new IllegalArgumentException(String.format("Failed to create report dir '%s'", reportDirFile));
        }
    }
}
