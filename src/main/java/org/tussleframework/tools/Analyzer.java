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
import static org.tussleframework.tools.FormatTool.matchFilters;
import static org.tussleframework.tools.FormatTool.parseValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.DoubleStream;

import org.tussleframework.RunArgs;
import org.tussleframework.TussleException;
import org.tussleframework.metrics.HdrData;
import org.tussleframework.metrics.HdrIntervalResult;
import org.tussleframework.metrics.HdrResult;
import org.tussleframework.metrics.Interval;
import org.tussleframework.metrics.Marker;
import org.tussleframework.metrics.Metric;
import org.tussleframework.metrics.MetricData;
import org.tussleframework.metrics.MetricInfo;
import org.tussleframework.metrics.MetricType;
import org.tussleframework.metrics.MetricValue;
import org.tussleframework.metrics.MovingWindowSLE;
import org.tussleframework.metrics.ServiceLevelExpectation;
import org.tussleframework.tools.processors.DiskstatProcessor;
import org.tussleframework.tools.processors.HiccupProcessor;
import org.tussleframework.tools.processors.IpstatProcessor;
import org.tussleframework.tools.processors.MpstatProcessor;
import org.tussleframework.tools.processors.OMBProcessor;
import org.tussleframework.tools.processors.RunPropertiesProcessor;
import org.tussleframework.tools.processors.SamplesProcessor;
import org.tussleframework.tools.processors.SamplesProcessorConfig;
import org.tussleframework.tools.processors.TLPStressProcessor;
import org.tussleframework.tools.processors.TopProcessor;
import org.yaml.snakeyaml.Yaml;

public class Analyzer implements Tool {

    class MetricDataDoc {
        public MetricData doc;
        public MetricDataDoc() {}
        public MetricDataDoc(MetricData doc) {
            this.doc = doc;
        }
    }

    public static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Analyzer.class.getName());
    public static final String[] EMPTY = {};
    private static final String SAMPLES2 = "samples_";

//    private static double[] percentilesBasic = {
//            0, 50, 90, 99, 99.9, 99.99, 100
//    };

    private static double[] percentilesShort = {
            0, 50, 90, 95, 99, 99.5, 99.9, 99.95, 99.99, 99.995, 99.999, 99.9995, 100
    };

    private static double[] percentilesLong = {
            0, 10, 20, 30, 40, 50, 55, 60, 65, 70, 75, 77.5, 80, 82.5, 85, 87.5, 88.75, 90, 91.25, 92.5, 
            93.75, 94.375, 95, 95.625, 96.25, 96.875, 97.1875, 97.5, 97.8125, 98.125, 98.4375, 98.5938,
            98.75, 98.9062, 99, 99.2188, 99.2969, 99.375, 99.4531, 99.5313, 99.6094, 99.6484, 99.6875,
            99.7266, 99.7656, 99.8047, 99.8242, 99.8437, 99.8633, 99.8828, 99.9, 99.9121, 99.9219, 99.9316,
            99.9414, 99.9512, 99.9561, 99.9609, 99.9658, 99.9707, 99.9756, 99.978, 99.9805, 99.9829, 99.9854,
            99.9878, 99.989, 99.99, 99.9915, 99.9927, 99.9939, 99.9945, 99.9951, 99.9957, 99.9963, 99.9969,
            99.9973, 99.9976, 99.9979, 99.9982, 99.9985, 99.9986, 99.9988, 99.999, 99.9991, 99.9992, 99.9993,
            99.9994, 99.9995, 99.9996, 99.9997, 99.9998, 99.9999, 100
    };

    protected MetricData metricData;
    protected AnalyzerConfig analyzerConfig;
    protected TreeSet<String> processedFiles;
    protected ArrayList<HdrResult> hdrResults;
    protected Map<String, HdrData> hdrDataMap;
    public RunArgs currentRunArgs;

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[Analyser] %s", String.format(format, args)));
        }
    }

    public static void main(String[] args) {
        LoggerTool.init("analyser");
        try {
            new Analyzer().processResults(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init(String[] args) throws TussleException {
        init(loadConfig(args));
    }

    public void init(AnalyzerConfig config) {
        analyzerConfig = config;
        if (analyzerConfig.sleConfig == null || analyzerConfig.sleConfig.length == 0) {
            analyzerConfig.sleConfig = new MovingWindowSLE[] {
            };
        }
        if (analyzerConfig.intervals == null || analyzerConfig.intervals.length == 0) {
            analyzerConfig.intervals = new Interval[] {
                    new Interval(analyzerConfig.hdrCutTime, 1000000, "", false),
            };
        }
        log("Config: %s", new Yaml().dump(analyzerConfig).trim());
        log(analyzerConfig.toString());
        hdrDataMap = new HashMap<>();
        metricData = new MetricData();
        hdrResults = new ArrayList<>();
        processedFiles = new TreeSet<>();
    }

    public AnalyzerConfig loadConfig(String[] args) throws TussleException {
        return ConfigLoader.loadObject(args, AnalyzerConfig.class);
    }

    public void processRecursive() throws TussleException {
        processRecursive(new File(analyzerConfig.histogramsDir));
    }

    public void printResults() throws TussleException {
        if (analyzerConfig.runProperties != null) {
            metricData.setRunProperties(analyzerConfig.runProperties);
        } else if (analyzerConfig.runPropertiesFile != null) {
            metricData.loadRunProperties(analyzerConfig.runPropertiesFile);
        } else {
            metricData.loadDefaultRunProperties();
        }
        // TODO: pass run start and finish time
        metricData.getRunProperties().setProperty("start_time", FormatTool.formatIsoDatetime(Calendar.getInstance().getTime()));
        metricData.getRunProperties().setProperty("finish_time", FormatTool.formatIsoDatetime(Calendar.getInstance().getTime()));
        metricData.getMetrics().forEach(m -> m.scoreOn().forEach(s -> log("Score on %s", s)));
        File metricsJson = new File(analyzerConfig.histogramsDir, "metrics.json");
        if (analyzerConfig.makeReport || analyzerConfig.saveMetrics) {
            try (PrintStream out = new PrintStream(metricsJson)) {
                if (analyzerConfig.doc) {
                    JsonTool.printJson(new MetricDataDoc(metricData), out);
                } else {
                    JsonTool.printJson(metricData, out);
                }
            } catch (Exception e) {
                throw new TussleException(e);
            }
        }
        if (analyzerConfig.makeReport) {
            Reporter.make(analyzerConfig.reportDir, metricsJson.getAbsolutePath());
        }
    }

    protected HdrData getHdrData(String operation, String name, String host) {
        MetricInfo netricInfo = new MetricInfo(operation, name, "", "", host);
        return hdrDataMap.computeIfAbsent(String.format("[%s,%s,%s]", name, operation, host), 
                key -> new HdrData(netricInfo, currentRunArgs != null ? currentRunArgs : new RunArgs(), analyzerConfig, analyzerConfig.sleConfig));
    }

    public void processResults(String[] args) throws TussleException {
        processResults(loadConfig(args));
    }

    public void processResults(AnalyzerConfig config) throws TussleException {
        init(config);
        processRecursive();
        //getHdrDataMetrics();
        processSummary();
        printResults();
    }

//    public void getHdrDataMetrics() {
//        hdrDataMap.forEach((key, hdrData) -> hdrData.getMetrics(metricData, percentilesBasic));
//    }

    public void getHdrResults(Collection<HdrResult> hdrResults) {
        hdrDataMap.forEach((key, hdrData) -> hdrData.getHdrResults(hdrResults));
    }

    public void saveHdrs() throws TussleException {
        withException(() -> hdrDataMap.forEach((key, hdrData) -> wrapException(hdrData::saveHdrs)));
    }

    public void processResults(AnalyzerConfig config, Collection<HdrResult> hdrResults) throws TussleException {
        init(config);
        withException(() -> hdrResults.forEach(hdrResult -> wrapException(() -> loadHdrData(hdrResult))));
        processSummary();
        printResults();
    }

    protected void processSummary() {
        // split results by operation and metric names: reads response-time, reads service-time, writes response-time, etc.
        HashMap<String, ArrayList<HdrResult>> resultsMap = getResultsMap();
        // process each metric group separately
        resultsMap.forEach(this::processOperationResults);
    }

    protected HashMap<String, ArrayList<HdrResult>> getResultsMap() {
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

    public static String[] getTypes(MovingWindowSLE[] slaConfig) {
        ArrayList<String> types = new ArrayList<>();
        for (int i = 0; i < slaConfig.length; i++) {
            types.add("P" + FormatTool.format(slaConfig[i].percentile) + "_VALUES");
        }
        return types.toArray(EMPTY);
    }

    public static boolean isRunPropertiesFile(String name) {
        name = FileTool.clearPathExt(name);
        return name.equals("run.properties.json") || name.equals("run-properties.json");
    }

    public static boolean isTopFile(String name) {
        name = FileTool.clearPathExt(name);
        return name.endsWith("top.log");
    }

    public static boolean isMpstatFile(String name) {
        name = FileTool.clearPathExt(name);
        return name.endsWith("mpstats.log") || name.endsWith("mpstat.log");
    }

    public static boolean isDiskstatFile(String name) {
        name = FileTool.clearPathExt(name);
        return name.endsWith("diskstats.log") || name.endsWith("diskstat.log");
    }

    public static boolean isIpstatFile(String name) {
        name = FileTool.clearPathExt(name);
        return name.endsWith("ipstats.log") || name.endsWith("ipstat.log");
    }

    public static boolean isHiccupFile(String name) {
        name = FileTool.clearPathExt(name);
        return name.startsWith("hiccup") && name.endsWith(".hlog");
    }

    public static boolean isHistogramFile(String name) {
        name = FileTool.clearPathExt(name);
        return  name.endsWith(".hgrm") && !name.contains("processed") ||
                name.endsWith(".hlog") && !name.contains("processed") && !name.startsWith("hiccup") ||
                name.startsWith("tlp_stress_metrics") && name.indexOf(".hdr-") > 0;
    }

    public static boolean isTLPStressResults(String name) {
        name = FileTool.clearPathExt(name);
        return name.startsWith("tlp_stress_metrics") && name.endsWith(".csv");
    }

    public static boolean isSamplesFile(String name) {
        name = FileTool.clearPathExt(name);
        return name.startsWith("samples") && name.endsWith(".csv") ||
                name.startsWith(SAMPLES2) && name.endsWith(".txt");
    }

    public static boolean isArchiveFile(String name) {
        name = FileTool.clearPath(name);
        return name.endsWith(".zip");
    }

    public static boolean isOMBFile(String name) {
        name = FileTool.clearPathExt(name);
        return name.startsWith("workload") && name.indexOf("OMB") >= 0 && name.endsWith(".json");
    }

    public static boolean isResultsFile(String fileName) {
        return isRunPropertiesFile(fileName)
                || isTopFile(fileName)
                || isMpstatFile(fileName)
                || isDiskstatFile(fileName)
                || isIpstatFile(fileName)
                || isHiccupFile(fileName)
                || isHistogramFile(fileName)
                || isTLPStressResults(fileName)
                || isSamplesFile(fileName)
                || isOMBFile(fileName);
    }

    public String getOperationName(String fileName) {
        String name = FileTool.clearPathExt(fileName);
        int pos = name.indexOf(SAMPLES2);
        if (pos >= 0) {
            pos = name.indexOf("_", SAMPLES2.length());
            name = name.substring(pos + 1);
            pos = name.lastIndexOf("_");
            name = name.substring(0, pos);
        }
        return name;
    }

    public boolean processResultStream(InputStream inputStream, String host, String fileName) {
        boolean res = true;
        if (isRunPropertiesFile(fileName)) {
            new RunPropertiesProcessor().processData(metricData, null, inputStream, host, logger);
        } else if (isTopFile(fileName)) {
            new TopProcessor().processData(metricData, null, inputStream, host, logger);
        } else if (isMpstatFile(fileName)) {
            new MpstatProcessor().processData(metricData, null, inputStream, host, logger);
        } else if (isDiskstatFile(fileName)) {
            new DiskstatProcessor().processData(metricData, null, inputStream, host, logger);
        } else if (isIpstatFile(fileName)) {
            new IpstatProcessor().processData(metricData, null, inputStream, host, logger);
        } else if (isHiccupFile(fileName)) {
            new HiccupProcessor().processData(metricData, null, inputStream, host, logger);
        } else if (isHistogramFile(fileName)) {
            processResultHistograms(inputStream, fileName);
        } else if (isTLPStressResults(fileName)) {
            new TLPStressProcessor().processData(metricData, null, inputStream, host, logger);
        } else if (isSamplesFile(fileName)) {
            processSamples(inputStream, host, fileName);
        } else if (isOMBFile(fileName)) {
            new OMBProcessor().processData(metricData, null, inputStream, host, logger);
        } else {
            res = false;
        }
        return res;
    }

    protected void processRecursive(File dir) throws TussleException {
        log("processRecursive: " + dir);
        if (!dir.exists()) {
            throw new TussleException(String.format("Input dir '%s' does not exists", dir));
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                processRecursive(file);
            } else {
                processFile(file);
            }
        }
    }

    public void processSamples(InputStream inputStream, String host, String fileName) {
        String operationName = getOperationName(fileName);
        if (matchFilters(operationName, analyzerConfig.operationsInclude, analyzerConfig.operationsExclude)) {
            SamplesProcessorConfig samplesConfig = new SamplesProcessorConfig();
            samplesConfig.copy(analyzerConfig);
            new SamplesProcessor(samplesConfig).processData(metricData, getHdrData(operationName, "", host), inputStream, host, logger);
        } else {
            log("Skipped processing '%s'", fileName);
        }
    }

    public void processFile(File file) throws TussleException {
        if (file.getName().endsWith(".zip")) {
            try (StreamSources.ZipStreamSource zipSource = new StreamSources.ZipStreamSource(file)) {
                withException(() -> zipSource.list().forEach(source -> wrapException(() -> processResultFile(file.getParentFile(), source))));
            }
        } else {
            processResultFile(file.getParentFile(), new StreamSources.FileStreamSource(file));
        }
    }

    /**
     * Extract host name from results directory name, e.g. /path/to/node_actor@hostname -> 'hostname' 
     * 
     * @param resultsDir
     * @return
     */
    public static String getHostnameFromPath(File resultsDir) {
        String host = "";
        if (resultsDir == null)
            return host;
        String dirName = resultsDir.getName();
        if (dirName.startsWith("node_")) {
            host = dirName.substring(dirName.indexOf('_') + 1);
        } else if (resultsDir.getParentFile() != null) {
            dirName = resultsDir.getParentFile().getName();
            if (dirName.startsWith("node_")) {
                host = dirName.substring(dirName.indexOf('_') + 1);
            }
        }
        int pos = host.indexOf('@');
        if (pos >= 0) {
            host = host.substring(pos + 1);
        }
        return host;
    }

    private boolean isProcessed(String fileName) {
        if (processedFiles.contains(fileName)) {
            log("File already processed: " + fileName);
            return true;
        }
        processedFiles.add(fileName);
        return false;
    }

    public void processResultFile(File parentDir, StreamSources.StreamSource streamSource) throws TussleException {
        if (isResultsFile(streamSource.getName()) && !isProcessed(streamSource.getAbsName())) {
            String host = getHostnameFromPath(parentDir);
            log("Processing '%s'...", streamSource.getAbsName());
            try (InputStream inputStream = streamSource.getStream()) {
                processResultStream(inputStream, host, streamSource.getAbsName());
            } catch (Exception e) {
                throw new TussleException(e);
            }
        }
    }

    public void processResultHistograms(InputStream inputStream, String fileName) {
        log("Processing histogram file '%s'...", fileName);
        HdrResult result = new HdrResult(fileName, analyzerConfig);
        result.loadHdrData(inputStream, analyzerConfig.sleConfig, analyzerConfig.intervals);
        result.getMetrics(metricData, analyzerConfig.allPercentiles ? percentilesLong : percentilesShort);
        hdrResults.add(result);
    }

    public void loadHdrData(HdrResult result) throws TussleException {
        if (result.hdrFile() != null) {
            log("Loading HDR data from file '%s'...", result.hdrFile());
            try (InputStream inputStream = new FileInputStream(result.hdrFile())) {
                result.loadHdrData(inputStream, analyzerConfig.sleConfig, analyzerConfig.intervals);
            } catch (Exception e) {
                throw new TussleException(e);
            }
        }
        result.getMetrics(metricData, analyzerConfig.allPercentiles ? percentilesLong : percentilesShort);
        hdrResults.add(result);
    }

    protected boolean hasMetric(String name) {
        return metricData.find(name) != null;
    }

    protected Metric addMetric(Metric metric) {
        metricData.add(metric);
        return metric;
    }

    protected Metric addMetric(String name, String opName, Double value, String rateUnits) {
        return addMetric(Metric.builder()
                .name(name)
                .operation(opName)
                .value(value)
                .units(rateUnits)
                .build());
    }

    protected Metric addMetric(String name, String opName, String timeUnits, String rateUnits, String[] xValues) {
        return addMetric(Metric.builder()
                .name(name)
                .operation(opName)
                .units(timeUnits)
                .xunits(rateUnits)
                .xValues(xValues).build());
    }
}
