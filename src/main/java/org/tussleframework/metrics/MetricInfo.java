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

package org.tussleframework.metrics;

import org.tussleframework.HdrConfig;
import org.tussleframework.RunArgs;
import org.tussleframework.tools.FormatTool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricInfo {
    private static String intendedPref = "intended-";

    public String operationName = "";
    public String metricName = "";
    public String rateUnits = "";
    public String timeUnits = "";
    public String hostName = "";

    public MetricInfo replaceMetricName(String metricName) {
        return new MetricInfo(operationName, metricName, rateUnits, timeUnits, hostName);
    }

    public String formatFileName(RunArgs runArgs) {
        return String.format("%s_%s_%s_%s_%d.hlog", operationName, metricName, FormatTool.roundFormatPercent(runArgs.ratePercent), FormatTool.format(runArgs.targetRate), runArgs.runStep);
    }

    public void fillValues(String[] parts, int filledParts, HdrConfig c) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - filledParts; i++) {
            if (sb.length() > 0) {
                sb.append('_');
            }
            sb.append(parts[i]);
        }
        String opName;
        String metrName = sb.toString();
        int pos = metrName.indexOf('_');
        if (pos > 0) {
            opName = metrName.substring(0, pos);
            metrName = metrName.substring(pos + 1);
        } else {
            opName = metrName;
            metrName = c.metricName;
            if (opName.toLowerCase().startsWith(intendedPref) && (metrName.equals(HdrResult.SERVICE_TIME2) || metrName.equals(HdrResult.SERVICE_TIME))) {
                opName = opName.substring(intendedPref.length());
                metrName = HdrResult.RESPONSE_TIME;
            }
        }
        metrName = metrName.replace(HdrResult.SERVICE_TIME2, HdrResult.SERVICE_TIME).replace(HdrResult.RESPONSE_TIME2, HdrResult.RESPONSE_TIME);
        this.operationName = opName;
        this.metricName = metrName;
        this.rateUnits = c.rateUnits;
        this.timeUnits = c.timeUnits;
    }
}
