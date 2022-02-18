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

package org.tussleframework.tools.processors;

import java.io.InputStream;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;

import org.HdrHistogram.Histogram;
import org.tussleframework.metrics.Metric;
import org.tussleframework.metrics.MetricData;
import org.tussleframework.metrics.MetricValue;
import org.tussleframework.tools.FormatTool;
import org.tussleframework.tools.LoggerTool;

public class SamplesProcessor implements DataLogProcessor {

    private SamplesProcessorConfig config;

    public SamplesProcessor() {
        this.config = new SamplesProcessorConfig();
    }

    public SamplesProcessor(SamplesProcessorConfig config) {
        this.config = config;
    }

    /**
Samples format: 
timestamp(s),value,..
1618204400.390739,48,..
1618204401.727753,34,..
1618204403.057668,40,..
1618204404.391447,45,..
...
     */
    @Override
    public boolean processData(MetricData metricData, InputStream inputStream, String host, Logger logger) {
        int stampsIdx = 0;
        int valuesIdx = 1;
        long start = 0;
        long finish = Long.MAX_VALUE;
        TreeMap<Long, Histogram> data = new TreeMap<>();
        try (Scanner scanner = new Scanner(inputStream)) {
            int lineNo = 0;
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                lineNo++;
                if (lineNo == 1 && config.hasHeader) {
                    String[] header = line.split(",");
                    for (int i = 0; i < header.length; i++) {
                        String h = header[i].toLowerCase();
                        if (h.indexOf("stamp") > 0) {
                            stampsIdx = i;
                        } else if (h.indexOf("value") > 0) {
                            valuesIdx = i;
                        }
                    }
                    continue;
                }
                String[] parts = line.split(",");
                long stamp = Math.round(Double.valueOf(parts[stampsIdx]) * config.timestampFactor);
                long value = Long.parseLong(parts[valuesIdx]);
                if (start > stamp) {
                    start = stamp;
                }
                if (finish < stamp) {
                    finish = stamp;
                }
                long intervalStamp = stamp / config.interval * config.interval;
                if (!data.containsKey(intervalStamp)) {
                    data.put(intervalStamp, new Histogram(3));
                }
                data.get(intervalStamp).recordValue(value);
            }
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
            return false;
        }
        DoubleStream.Builder[] buffValues = new DoubleStream.Builder[config.percentiles.length + 1];
        long intervalStampStart = start / config.interval * config.interval;
        long intervalStampFinish = finish / config.interval * config.interval;
        for (long intervalStamp = intervalStampStart; intervalStamp <= intervalStampFinish; intervalStamp += config.interval) {
            if (data.containsKey(intervalStamp)) {
                Histogram histogram = data.get(intervalStamp);
                for (int i = 0; i < config.percentiles.length; i++) {
                    buffValues[i].add(histogram.getValueAtPercentile(config.percentiles[i]) / config.histogramFactor);
                }
                buffValues[config.percentiles.length].add(histogram.getTotalCount());
            } else {
                for (int i = 0; i < config.percentiles.length; i++) {
                    buffValues[i].add(0);
                }
                buffValues[config.percentiles.length].add(0);
            }
        }
        Metric metric = Metric.builder()
                .name(config.name)
                .host(host)
                .start(start)
                .finish(finish)
                .delay(config.interval)
                .build();
        for (int i = 0; i < config.percentiles.length; i++) {
            metric.add(new MetricValue("P" + FormatTool.roundFormat(config.percentiles[i]).replace(".", "_") + "_VALUES", buffValues[i].build().toArray()));
        }
        metric.add(new MetricValue("COUNT", buffValues[config.percentiles.length].build().toArray()));
        return true;
    }
}
