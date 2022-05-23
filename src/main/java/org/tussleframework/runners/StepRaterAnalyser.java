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

import static org.tussleframework.WithException.withException;
import static org.tussleframework.WithException.wrapException;
import static org.tussleframework.metrics.HdrIntervalResult.reportedType;
import static org.tussleframework.metrics.HdrIntervalResult.reportedTypeCount;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.DoubleStream;

import org.tussleframework.TussleException;
import org.tussleframework.metrics.HdrResult;
import org.tussleframework.metrics.Marker;
import org.tussleframework.metrics.Metric;
import org.tussleframework.metrics.MetricType;
import org.tussleframework.metrics.MetricValue;
import org.tussleframework.metrics.MovingWindowSLE;
import org.tussleframework.metrics.ServiceLevelExpectation;
import org.tussleframework.tools.Analyzer;
import org.tussleframework.tools.AnalyzerConfig;
import org.tussleframework.tools.FormatTool;
import org.tussleframework.tools.LoggerTool;

public class StepRaterAnalyser extends Analyzer {

    public static void main(String[] args) {
        LoggerTool.init("tussle-analyser");
        try {
            new StepRaterAnalyser().processResults(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processResults(String[] args) throws TussleException {
        init(args);
        processRecursive();
        processSummary();
        printResults();
    }

    @Override
    public void processResults(AnalyzerConfig config, Collection<HdrResult> results) throws TussleException {
        init(config);
        withException(() -> results.forEach(result -> wrapException(() -> processResultHistograms(result))));
        processSummary();
        printResults();
    }

    private HashMap<String, ArrayList<HdrResult>> getResultsMap() {
        HashMap<String, ArrayList<HdrResult>> resultsMap = new HashMap<>();
        for (HdrResult result : hdrResults) {
            String name = result.operationName + " " + result.metricName;
            ArrayList<HdrResult> specificResuls;
            if (resultsMap.containsKey(name)) {
                specificResuls = resultsMap.get(name);
            } else {
                specificResuls = new ArrayList<>();
                resultsMap.put(name, specificResuls);
            }
            specificResuls.add(result);
        }
        resultsMap.forEach((metricName, specificResuls) -> specificResuls
                .sort((a, b) -> (int) (a.runArgs.targetRate == b.runArgs.targetRate ? a.runArgs.step - b.runArgs.step : a.runArgs.targetRate - b.runArgs.targetRate)));
        return resultsMap;
    }

    public void processOperationResults(String opAndMetricName, List<HdrResult> operationResults) {
        MovingWindowSLE[] sleConfig = analyzerConfig.sleConfig;
        ArrayList<MovingWindowSLE> unbrokenSleConfig = new ArrayList<>();
        Collections.addAll(unbrokenSleConfig, sleConfig);
        ArrayList<Marker> sleMarkers = new ArrayList<>();
        HashMap<String, Double> sleBroken = new HashMap<>();
        for (HdrResult result : operationResults) {
            Iterator<MovingWindowSLE> iterator = unbrokenSleConfig.iterator();
            while (iterator.hasNext()) {
                ServiceLevelExpectation sle = iterator.next();
                if (!result.checkSLE(sle)) {
                    log("%s SLE for %s broken on %s %s", opAndMetricName, sle, FormatTool.format(result.runArgs.targetRate), result.rateUnits);
                    iterator.remove();
                    sleBroken.put(sle.longName(), result.runArgs.targetRate);
                    sleMarkers.add(new Marker(sle.markerName(), result.runArgs.targetRate, sle.markerValue()));
                }
            }
        }
        unbrokenSleConfig.forEach(sle -> log("%s SLE for %s was not broken", opAndMetricName, sle));
        DoubleStream.Builder[] valBuffersMax = new DoubleStream.Builder[reportedTypeCount()];
        DoubleStream.Builder[] valBuffersAvg = new DoubleStream.Builder[reportedTypeCount()];
        for (int i = 0; i < reportedTypeCount(); i++) {
            valBuffersMax[i] = DoubleStream.builder();
            valBuffersAvg[i] = DoubleStream.builder();
        }
        DoubleStream.Builder[] valBuffersMW = new DoubleStream.Builder[sleConfig.length];
        for (int j = 0; j < sleConfig.length; j++) {
            valBuffersMW[j] = DoubleStream.builder();
        }
        ArrayList<String> xValuesBuff = new ArrayList<>();
        for (HdrResult result : operationResults) {
            xValuesBuff.add(FormatTool.format(result.runArgs.targetRate));
            if (result.recordsCount == 0) {
                continue;
            }
            for (int j = 0; j < reportedTypeCount(); j++) {
                Metric metric = result.subIntervalHistograms.get(0).getMetric();
                if (metric == null) {
                    continue;
                }
                MetricValue mv = metric.byType(reportedType(j));
                if (mv == null) {
                    valBuffersMax[j].add(-1.0);
                    valBuffersAvg[j].add(-1.0);
                } else if (reportedType(j) == MetricType.COUNTS) {
                    if (metric.getFinish() > metric.getStart()) {
                        double throughput = mv.sumValue() * 1000.0 / (metric.getFinish() - metric.getStart());
                        valBuffersMax[j].add(throughput);
                        valBuffersAvg[j].add(throughput);
                    } else {
                        valBuffersMax[j].add(-1.0);
                        valBuffersAvg[j].add(-1.0);
                    }
                } else {
                    valBuffersMax[j].add(mv.maxValue());
                    valBuffersAvg[j].add(mv.avgValue());
                }
            }
        }
        Optional<HdrResult> optionalResult = operationResults.stream().filter(result -> result.recordsCount > 0).findFirst();
        if (!optionalResult.isPresent()) {
            return;
        }
        HdrResult specificResult = optionalResult.get();
        String[] xValues = xValuesBuff.toArray(EMPT);
        Metric mMax = Metric.builder()
                .name(specificResult.metricName + " summary_max")
                .operation(specificResult.operationName)
                .units("ms")
                .xunits(specificResult.rateUnits)
                .xValues(xValues)
                .build();
        Metric mAvg = Metric.builder()
                .name(specificResult.metricName + " summary_avg" )
                .operation(specificResult.operationName)
                .units("ms")
                .xunits(specificResult.rateUnits)
                .xValues(xValues).build();
        for (int i = 0; i < reportedTypeCount(); i++) {
            mMax.add(new MetricValue(reportedType(i).name(), valBuffersMax[i].build().toArray()));
            mAvg.add(new MetricValue(reportedType(i).name(), valBuffersAvg[i].build().toArray()));
        }
        metricData.add(mMax);
        metricData.add(mAvg);
        double maxRate = operationResults.get(operationResults.size() - 1).runArgs.targetRate;
        String[] sleTypes = getTypes(sleConfig);
        for (int i = 0; i < sleConfig.length; i++) {
            MovingWindowSLE sle = sleConfig[i];
            String sleName = sle.longName();
            String oName = specificResult.operationName + " " + sleName;
            String mName = specificResult.metricName;
            if (sleBroken.containsKey(sleName)) {
                metricData.add(Metric.builder()
                        .name(mName + " conforming_rate")
                        .operation(oName)
                        .units(specificResult.rateUnits)
                        .value(sleBroken.get(sleName))
                        .build());
            } else {
                metricData.add(Metric.builder()
                        .name(mName + " conforming_rate (unbroken)")
                        .operation(oName)
                        .units(specificResult.rateUnits)
                        .value(maxRate)
                        .build());
            }
            metricData.add(Metric.builder()
                    .name(mName + " summary_max")
                    .operation(oName)
                    .units("ms")
                    .xunits(specificResult.rateUnits)
                    .xValues(xValues)
                    .group(opAndMetricName + " mw_summary_max")
                    .build()
                    .add(new MetricValue(sleTypes[i], valBuffersMW[i].build().toArray())));
        }
        mMax.setMarkers(sleMarkers);
        mAvg.setMarkers(sleMarkers);
    }

    public void processSummary() {
        // split results by operation and metric names: reads response_time, reads service_time, writes response_time, etc.
        HashMap<String, ArrayList<HdrResult>> resultsMap = getResultsMap();
        // process each metric group separately
        resultsMap.forEach(this::processOperationResults);
    }
}
