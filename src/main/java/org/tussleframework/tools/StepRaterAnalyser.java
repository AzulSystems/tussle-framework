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

package org.tussleframework.tools;

import static org.tussleframework.WithException.withException;
import static org.tussleframework.WithException.wrapException;
import static org.tussleframework.metrics.HdrIntervalResult.metricType;
import static org.tussleframework.metrics.HdrIntervalResult.metricTypeCount;
import static org.tussleframework.tools.FormatTool.parseValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.DoubleStream;

import org.tussleframework.TussleException;
import org.tussleframework.metrics.HdrIntervalResult;
import org.tussleframework.metrics.HdrResult;
import org.tussleframework.metrics.Marker;
import org.tussleframework.metrics.Metric;
import org.tussleframework.metrics.MetricType;
import org.tussleframework.metrics.MetricValue;
import org.tussleframework.metrics.MovingWindowSLE;
import org.tussleframework.metrics.ServiceLevelExpectation;

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
        withException(() -> results.forEach(result -> wrapException(() -> loadHdrData(result))));
        processSummary();
        printResults();
    }

    private HashMap<String, ArrayList<HdrResult>> getResultsMap() {
        HashMap<String, ArrayList<HdrResult>> resultsMap = new HashMap<>();
        for (HdrResult result : hdrResults) {
            if (FormatTool.matchFilters(result.metricName(), analyzerConfig.sleFor, null)) {
                String name = result.operationName() + " " + result.metricName();
                resultsMap.computeIfAbsent(name, key -> new ArrayList<>()).add(result);
            }
        }
        resultsMap.forEach((metricName, hdrResuls) -> hdrResuls
                .sort((a, b) -> (int) (a.targetRate() == b.targetRate() ? a.step() - b.step() : a.targetRate() - b.targetRate())));
        return resultsMap;
    }
    
    protected HashMap<String, Double> brokenSLEs(String opAndMetricName, List<HdrResult> hdrResults, Collection<Marker> sleMarkers) {
        ArrayList<MovingWindowSLE> unbrokenSleConfig = new ArrayList<>();
        Collections.addAll(unbrokenSleConfig, analyzerConfig.sleConfig);
        HashMap<String, Double> sleBroken = new HashMap<>();
        for (HdrResult hdrResult : hdrResults) {
            Iterator<MovingWindowSLE> iterator = unbrokenSleConfig.iterator();
            while (iterator.hasNext()) {
                ServiceLevelExpectation sle = iterator.next();
                if (!hdrResult.checkSLE(sle, analyzerConfig.intervals[0])) {
                    log("%s SLE for %s broken on %s %s", opAndMetricName, sle, FormatTool.format(hdrResult.targetRate()), hdrResult.rateUnits());
                    iterator.remove();
                    sleBroken.put(sle.longName(), hdrResult.targetRate());
                    sleMarkers.add(new Marker(sle.markerName(), hdrResult.targetRate(), sle.markerValue()));
                }
            }
        }
        unbrokenSleConfig.forEach(sle -> log("%s SLE for %s was not broken", opAndMetricName, sle));
        return sleBroken;
    }

    public void processOperationResults(String opAndMetricName, List<HdrResult> hdrResults) {
        ArrayList<Marker> sleMarkers = new ArrayList<>();
        HashMap<String, Double> sleBroken = brokenSLEs(opAndMetricName, hdrResults, sleMarkers);
        DoubleStream.Builder[] valBuffersMax = new DoubleStream.Builder[metricTypeCount()];
        DoubleStream.Builder[] valBuffersAvg = new DoubleStream.Builder[metricTypeCount()];
        for (int i = 0; i < metricTypeCount(); i++) {
            valBuffersMax[i] = DoubleStream.builder();
            valBuffersAvg[i] = DoubleStream.builder();
        }
//        DoubleStream.Builder[] valBuffersMW = new DoubleStream.Builder[sleConfig.length];
//        for (int j = 0; j < sleConfig.length; j++) {
//            valBuffersMW[j] = DoubleStream.builder();
//        }
        ArrayList<String> xValuesBuff = new ArrayList<>();
        for (HdrResult hdrResult : hdrResults) {
            if (hdrResult.recordsCount() == 0) {
                continue;
            }
            xValuesBuff.add(FormatTool.format(hdrResult.targetRate()));
            HdrIntervalResult hdrPrimeResult = hdrResult.getPrimeResult();
            for (int metricIdx = 0; metricIdx < metricTypeCount(); metricIdx++) {
                Metric metric = hdrPrimeResult.getMetric();
                if (metric == null) {
                    valBuffersMax[metricIdx].add(-1.0);
                    valBuffersAvg[metricIdx].add(-1.0);
                    continue;
                }
                MetricValue metricValue = metric.byType(metricType(metricIdx));
                if (metricValue == null) {
                    valBuffersMax[metricIdx].add(-1.0);
                    valBuffersAvg[metricIdx].add(-1.0);
                } else if (metricType(metricIdx) == MetricType.COUNTS) {
                    if (metric.getFinish() > metric.getStart()) {
                        double throughput = metricValue.sumValue() * 1000.0 / (metric.getFinish() - metric.getStart());
                        valBuffersMax[metricIdx].add(throughput);
                        valBuffersAvg[metricIdx].add(throughput);
                    } else {
                        valBuffersMax[metricIdx].add(-1.0);
                        valBuffersAvg[metricIdx].add(-1.0);
                    }
                } else {
                    valBuffersMax[metricIdx].add(metricValue.maxValue());
                    valBuffersAvg[metricIdx].add(metricValue.avgValue());
                }
            }
        }
        Optional<HdrResult> optionalHdrResult = hdrResults.stream().filter(result -> result.recordsCount() > 0).findFirst();
        if (!optionalHdrResult.isPresent()) {
            return;
        }
        HdrResult firstHdrResult = optionalHdrResult.get();
        String[] xValues = xValuesBuff.toArray(EMPTY);
        Metric mAvg = addMetric(firstHdrResult.metricName() + " summary_avg", firstHdrResult.operationName(), firstHdrResult.timeUnits(), firstHdrResult.rateUnits(), xValues);
        Metric mMax = addMetric(firstHdrResult.metricName() + " summary_max", firstHdrResult.operationName(), firstHdrResult.timeUnits(), firstHdrResult.rateUnits(), xValues);
        for (int i = 0; i < metricTypeCount(); i++) {
            mAvg.add(new MetricValue(metricType(i).name(), valBuffersAvg[i].build().toArray()));
            mMax.add(new MetricValue(metricType(i).name(), valBuffersMax[i].build().toArray()));
        }
        double maxActualRate = hdrResults.get(hdrResults.size() - 1).getRate();
        String opName = firstHdrResult.operationName();
        addMetric(firstHdrResult.metricName() + " max_rate", opName, maxActualRate, firstHdrResult.rateUnits());
        double maxTargetRate = hdrResults.get(hdrResults.size() - 1).targetRate();
        if (maxTargetRate > 0 && !hasMetric("max_target_rate")) {
            addMetric("max_target_rate", null, maxTargetRate, firstHdrResult.rateUnits());
        }
        double highBound = parseValue(analyzerConfig.highBound);
        if (highBound > 0 && !hasMetric("high_bound")) {
            addMetric("high_bound", null, highBound, firstHdrResult.rateUnits());
        }
        ///String[] sleTypes = getTypes(sleConfig);
        for (int i = 0; i < analyzerConfig.sleConfig.length; i++) {
            MovingWindowSLE sle = analyzerConfig.sleConfig[i];
            String sleName = sle.longName();
            String mName = firstHdrResult.metricName() + " " + sleName;
            if (sleBroken.containsKey(sleName)) {
                addMetric(mName + " conforming_rate", opName, sleBroken.get(sleName), firstHdrResult.rateUnits());
            } else {
                addMetric(mName + " conforming_rate (unbroken)", opName, maxTargetRate, firstHdrResult.rateUnits());
            }
            //TODO: targetMetric.add(new MetricValue(valName + "_max", movingWindowMax[i]))
//            metricData.add(Metric.builder()
//                    .name(mName + " summary_max")
//                    .operation(oName)
//                    .units("ms")
//                    .xunits(firstHdrResult.rateUnits())
//                    .xValues(xValues)
//                    .group(opAndMetricName + " mw_summary_max")
//                    .build()
//                    .add(new MetricValue(sleTypes[i], valBuffersMW[i].build().toArray())));
        }
        mMax.setMarkers(sleMarkers);
        mAvg.setMarkers(sleMarkers);
    }

    public void processSummary() {
        // split results by operation and metric names: reads response-time, reads service-time, writes response-time, etc.
        HashMap<String, ArrayList<HdrResult>> resultsMap = getResultsMap();
        // process each metric group separately
        resultsMap.forEach(this::processOperationResults);
    }
}
