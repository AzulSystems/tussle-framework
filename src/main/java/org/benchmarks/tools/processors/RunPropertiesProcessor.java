/*
 * Copyright (c) 2021, Azul Systems
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

package org.benchmarks.tools.processors;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.benchmarks.RunProperties;
import org.benchmarks.metrics.MetricData;
import org.benchmarks.tools.JsonTool;
import org.benchmarks.tools.LoggerTool;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
class RunPropertiesData {
    RunProperties runProperties = new RunProperties();
}

@Data
@ToString
class RunPropertiesDoc {
    RunPropertiesData doc = new RunPropertiesData();
}

public class RunPropertiesProcessor implements DataLogProcessor {
    @Override
    public boolean processData(MetricData metricData, InputStream inputStream, String host, Logger logger) {
        try {
            RunPropertiesDoc doc = JsonTool.readJson(inputStream, RunPropertiesDoc.class);
            if (logger.isLoggable(Level.INFO)) {
                logger.info(doc.toString());
            }
            metricData.setRunProperties(doc.doc.runProperties);
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
            return false;
        }
        return true;
    }
}
