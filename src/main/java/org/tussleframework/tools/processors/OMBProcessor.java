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

package org.tussleframework.tools.processors;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;
import java.util.stream.DoubleStream.Builder;

import org.tussleframework.BasicProperties;
import org.tussleframework.metrics.HdrData;
import org.tussleframework.metrics.HdrResult;
import org.tussleframework.metrics.Metric;
import org.tussleframework.metrics.MetricData;
import org.tussleframework.metrics.MetricValue;
import org.tussleframework.tools.JsonTool;
import org.tussleframework.tools.LoggerTool;

import lombok.Data;

@Data
class OMBMetrics {
    String workload;
    String driver;
    double[] publishRate;
    double[] consumeRate;
    double[] backlog;
    double[] publishLatencyAvg;
    double[] publishLatency50pct;
    double[] publishLatency75pct;
    double[] publishLatency95pct;
    double[] publishLatency99pct;
    double[] publishLatency999pct;
    double[] publishLatency9999pct;
    double[] publishLatencyMax;
    double[] endToEndLatencyAvg;
    double[] endToEndLatency50pct;
    double[] endToEndLatency75pct;
    double[] endToEndLatency95pct;
    double[] endToEndLatency99pct;
    double[] endToEndLatency999pct;
    double[] endToEndLatency9999pct;
    double[] endToEndLatencyMax;
    HashMap<Double, Double> aggregatedEndToEndLatencyQuantiles;
}

public class OMBProcessor implements DataLogProcessor {

    @Override
    public boolean processData(MetricData metricData, HdrData hdrData, BasicProperties processorsProps, InputStream inputStream, String host, Logger logger) {
        long start = 0;
        long finish = 0;
        OMBMetrics omb;
        try {
            omb = JsonTool.readJson(inputStream, OMBMetrics.class, false, false);
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
            return false;
        }
        Metric publishMetric = Metric.builder()
            .name(HdrResult.SERVICE_TIME)
            .operation("publish")
            .units("ms")
            .host(host)
            .start(start)
            .finish(finish)
            .delay(10000)
            .build();
        publishMetric.add(new MetricValue("AVG_VALUES", omb.publishLatencyAvg));
        publishMetric.add(new MetricValue("P50_VALUES", omb.publishLatency50pct));
        publishMetric.add(new MetricValue("P75_VALUES", omb.publishLatency75pct));
        publishMetric.add(new MetricValue("P95_VALUES", omb.publishLatency95pct));
        publishMetric.add(new MetricValue("P99_VALUES", omb.publishLatency99pct));
        publishMetric.add(new MetricValue("P999_VALUES", omb.publishLatency999pct));
        publishMetric.add(new MetricValue("P9999_VALUES", omb.publishLatency9999pct));
        publishMetric.add(new MetricValue("P100_VALUES", omb.publishLatencyMax));
        publishMetric.add(new MetricValue("THROUGHPUT", omb.publishRate));
        metricData.add(publishMetric);
        Metric backlogMetric = Metric.builder()
                .name("backlog")
                .host(host)
                .start(start)
                .finish(finish)
                .delay(10000)
                .build();
        backlogMetric.add(new MetricValue("VALUES", omb.backlog));
        metricData.add(backlogMetric);
        Metric consumeMetric = Metric.builder()
                .name(HdrResult.SERVICE_TIME)
                .operation("consume")
                .units("ms")
                .host(host)
                .start(start)
                .finish(finish)
                .delay(10000)
                .build();
        consumeMetric.add(new MetricValue("THROUGHPUT", omb.consumeRate));
        metricData.add(consumeMetric);
        Metric endToEndMetric = Metric.builder()
                .name(HdrResult.SERVICE_TIME)
                .operation("endToEnd")
                .units("ms")
                .host(host)
                .start(start)
                .finish(finish)
                .delay(10000)
                .build();
        endToEndMetric.add(new MetricValue("AVG_VALUES", omb.endToEndLatencyAvg));
        endToEndMetric.add(new MetricValue("P50_VALUES", omb.endToEndLatency50pct));
        endToEndMetric.add(new MetricValue("P75_VALUES", omb.endToEndLatency75pct));
        endToEndMetric.add(new MetricValue("P95_VALUES", omb.endToEndLatency95pct));
        endToEndMetric.add(new MetricValue("P99_VALUES", omb.endToEndLatency99pct));
        endToEndMetric.add(new MetricValue("P999_VALUES", omb.endToEndLatency999pct));
        endToEndMetric.add(new MetricValue("P9999_VALUES", omb.endToEndLatency9999pct));
        endToEndMetric.add(new MetricValue("P100_VALUES", omb.endToEndLatencyMax));
        metricData.add(endToEndMetric);
        Metric aggMetric = Metric.builder()
                .name(HdrResult.SERVICE_TIME + "_aggregated_quantiles")
                .operation("endToEnd")
                .units("ms")
                .host(host)
                .build();
        ArrayList<Double> aggPNames = new ArrayList<>(omb.aggregatedEndToEndLatencyQuantiles.keySet());
        aggPNames.sort(Double::compare);
        ArrayList<String> aggNames = new ArrayList<>();
        Builder aggValues = DoubleStream.builder();
        for (Double pname : aggPNames) {
            aggNames.add(String.valueOf(pname));
            aggValues.add(omb.aggregatedEndToEndLatencyQuantiles.get(pname));
        }
        aggMetric.setXValues(aggNames.toArray(new String[] {}));
        aggMetric.add(new MetricValue("VALUES", aggValues.build().toArray()));
        metricData.add(aggMetric);
        return true;
    }
}
