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
import static org.tussleframework.tools.FormatTool.matchFilters;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.tussleframework.RunArgs;
import org.tussleframework.RunProperties;
import org.tussleframework.TussleException;
import org.tussleframework.metrics.HdrData;
import org.tussleframework.metrics.HdrResult;
import org.tussleframework.metrics.Interval;
import org.tussleframework.metrics.MetricData;
import org.tussleframework.metrics.MetricInfo;
import org.tussleframework.metrics.MovingWindowSLE;
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

public class Analyzer {

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

    private static double[] percentilesBasic = {
            0, 50, 90, 99, 99.9, 99.99, 100
    };

    private static double[] percentilesShort = {
            0, 50, 90, 95, 99, 99.5, 99.9, 99.95, 99.99, 99.995, 99.999, 99.9995, 100
    };

    private static double[] percentilesLong = {
            0, 10, 20, 30, 40, 50, 55, 60, 65, 70, 75, 77.5, 80, 82.5, 85, 87.5, 88.75, 90, 91.25, 92.5, 
            93.75, 94.375, 95, 95.625, 96.25, 96.875, 97.1875, 97.5, 97.8125, 98.125, 98.4375, 98.5938,
            98.75, 98.9062, 99.0625, 99.2188, 99.2969, 99.375, 99.4531, 99.5313, 99.6094, 99.6484, 99.6875,
            99.7266, 99.7656, 99.8047, 99.8242, 99.8437, 99.8633, 99.8828, 99.9023, 99.9121, 99.9219, 99.9316,
            99.9414, 99.9512, 99.9561, 99.9609, 99.9658, 99.9707, 99.9756, 99.978, 99.9805, 99.9829, 99.9854,
            99.9878, 99.989, 99.9902, 99.9915, 99.9927, 99.9939, 99.9945, 99.9951, 99.9957, 99.9963, 99.9969,
            99.9973, 99.9976, 99.9979, 99.9982, 99.9985, 99.9986, 99.9988, 99.9989, 99.9991, 99.9992, 99.9993,
            99.9994, 99.9995, 99.9996, 99.9997, 99.9998, 99.9999, 100
    };

    protected MetricData metricData;
    protected AnalyzerConfig analyzerConfig;
    protected TreeSet<String> processedFiles;
    protected ArrayList<HdrResult> hdrResults;
    protected Map<String, HdrData> gdrDataMap;
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

    protected HdrData getHdrData(String operation, String name, String host) {
        MetricInfo netricInfo = new MetricInfo(operation, name, "", "", host);
        return gdrDataMap.computeIfAbsent(String.format("[%s,%s,%s]", name, operation, host), 
                key -> new HdrData(netricInfo, currentRunArgs != null ? currentRunArgs : new RunArgs(), analyzerConfig, analyzerConfig.sleConfig));
    }

    public void processResults(String[] args) throws TussleException {
        processResults(loadConfig(args));
    }

    public void processResults(AnalyzerConfig config) throws TussleException {
        init(config);
        processRecursive();
        getHdrDataMetrics();
        printResults();
    }

    public void getHdrDataMetrics() {
        gdrDataMap.forEach((key, hdrData) -> hdrData.getMetrics(metricData, percentilesBasic));
    }

    public void getHdrResults(Collection<HdrResult> hdrResults) {
        gdrDataMap.forEach((key, hdrData) -> hdrData.getHdrResults(hdrResults));
    }

    public void saveHdrs() throws TussleException {
        withException(() -> gdrDataMap.forEach((key, hdrData) -> wrapException(hdrData::saveHdrs)));
    }

    public void processResults(AnalyzerConfig config, Collection<HdrResult> hdrResults) throws TussleException {
        init(config);
        withException(() -> hdrResults.forEach(hdrResult -> wrapException(() -> loadHdrData(hdrResult))));
        printResults();
    }

    public void init(AnalyzerConfig config) {
        analyzerConfig = config;
        if (analyzerConfig.sleConfig == null || analyzerConfig.sleConfig.length == 0) {
            analyzerConfig.sleConfig = new MovingWindowSLE[] {
                    // new SLA(90, 0, 10),
            };
        }
        if (analyzerConfig.intervals == null || analyzerConfig.intervals.length == 0) {
            analyzerConfig.intervals = new Interval[] {
                    new Interval(0, 1000000, "", false),
                    // new Interval(2700, 4500, "POST_WARMUP45"),
            };
        }
        log("Config: %s", new Yaml().dump(analyzerConfig).trim());
        log(analyzerConfig.toString());
        gdrDataMap = new HashMap<>();
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
        if (metricData.getRunProperties() == null) {
            metricData.setRunProperties(new RunProperties());
        }
        File metricsJson = new File(analyzerConfig.histogramsDir, "metrics.json");
        try (PrintStream out = new PrintStream(metricsJson)) {
            if (analyzerConfig.doc) {
                JsonTool.printJson(new MetricDataDoc(metricData), out);
            } else {
                JsonTool.printJson(metricData, out);
            }
        } catch (Exception e) {
            throw new TussleException(e);
        }
        metricData.getMetrics().forEach(m -> log("Collected metric: %s", m));
        if (analyzerConfig.makeReport) {
            if (analyzerConfig.doc) {
                Reporter.make(metricData, analyzerConfig.reportDir);
            } else {
                Reporter.make(analyzerConfig.reportDir, metricsJson.getAbsolutePath());
            }
        }
    }

    public static String[] getTypes(MovingWindowSLE[] slaConfig) {
        ArrayList<String> types = new ArrayList<>();
        for (int i = 0; i < slaConfig.length; i++) {
            types.add("P" + FormatTool.format(slaConfig[i].percentile) + "_VALUES");
        }
        return types.toArray(EMPTY);
    }

    public static boolean isRunPropertiesFile(String name) {
        name = FileTool.clearPath(name);
        return name.equals("run.properties.json") || name.equals("run-properties.json");
    }

    public static boolean isTopFile(String name) {
        name = FileTool.clearPath(name);
        return name.endsWith("top.log");
    }

    public static boolean isMpstatFile(String name) {
        name = FileTool.clearPath(name);
        return name.endsWith("mpstats.log") || name.endsWith("mpstat.log");
    }

    public static boolean isDiskstatFile(String name) {
        name = FileTool.clearPath(name);
        return name.endsWith("diskstats.log") || name.endsWith("diskstat.log");
    }

    public static boolean isIpstatFile(String name) {
        name = FileTool.clearPath(name);
        return name.endsWith("ipstats.log") || name.endsWith("ipstat.log");
    }

    public static boolean isHiccupFile(String name) {
        name = FileTool.clearPath(name);
        return name.startsWith("hiccup") && name.endsWith(".hlog");
    }

    public static boolean isHistogramFile(String name) {
        name = FileTool.clearPath(name);
        return  name.endsWith(".hgrm") && !name.contains("processed") ||
                name.endsWith(".hlog") && !name.contains("processed") && !name.startsWith("hiccup") ||
                name.startsWith("tlp_stress_metrics") && name.indexOf(".hdr-") > 0;
    }

    public static boolean isTLPStressResults(String name) {
        name = FileTool.clearPath(name);
        return name.startsWith("tlp_stress_metrics") && name.endsWith(".csv");
    }

    public static boolean isSamplesFile(String name) {
        name = FileTool.clearPath(name);
        return name.startsWith("samples") && name.endsWith(".csv") ||
                name.startsWith(SAMPLES2) && name.endsWith(".txt");
    }

    public static boolean isArchFile(String name) {
        name = FileTool.clearPath(name);
        return name.endsWith(".zip");
    }

    public static boolean isOMBFile(String name) {
        name = FileTool.clearPath(name);
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
        String name = new File(fileName).getName();
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
            processResultHistograms(fileName, inputStream);
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
            processZipFile(file);
        } else {
            processResultFile(file, null, null);
        }
    }

    public void processZipFile(File file) throws TussleException {
        log("Processing ZIP file: " + file);
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.isDirectory()) {
                    continue;
                }
                processResultFile(file, zipFile, zipEntry);
            }
        } catch (Exception e) {
            throw new TussleException(e);
        }
    }

    public boolean isProcessed(String fileName) {
        if (processedFiles.contains(fileName)) {
            log("File already processed: " + fileName);
            return true;
        }
        processedFiles.add(fileName);
        return false;
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

    public boolean processResultFile(File file, ZipFile zipFile, ZipEntry zipEntry) throws TussleException {
        File dir = file.getParentFile();
        if (zipEntry != null) {
            File fileZip = new File(zipEntry.getName());
            String subDir = fileZip.getParent();
            if (subDir == null) {
                subDir = ".";
            }
            file = new File(new File(file + ":", subDir), fileZip.getName());
        }
        if (isResultsFile(file.getName())) {
            if (isProcessed(file.getAbsolutePath()))
                return true;
            String host = getHostnameFromPath(dir);
            if (zipEntry != null) {
                log("Processing file from archive '%s/%s'...", dir, file.getName());
                try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
                    return processResultStream(inputStream, host, file.getName());
                } catch (Exception e) {
                    throw new TussleException(e);
                }
            } else {
                log("Processing file '%s'...", file);
                try (InputStream inputStream = new FileInputStream(file)) {
                    return processResultStream(inputStream, host, file.getName());
                } catch (Exception e) {
                    throw new TussleException(e);
                }
            }
        } else {
            return false;
        }
    }

    public Interval[] getIntervals() {
        ArrayList<Interval> a = new ArrayList<>();
        a.addAll(Arrays.asList(analyzerConfig.intervals));
        return a.toArray(new Interval[0]);
    }

    public void processResultHistograms(String fileName, InputStream inputStream) {
        log("Processing histogram file '%s'...", fileName);
        HdrResult result = new HdrResult(fileName, analyzerConfig);
        result.loadHdrData(inputStream, analyzerConfig.sleConfig, getIntervals());
        result.getMetrics(metricData, analyzerConfig.allPercentiles ? percentilesLong : percentilesShort);
        hdrResults.add(result);
    }

    public void loadHdrData(HdrResult result) throws TussleException {
        if (result.hdrFile() != null) {
            log("Loading HDR data from file '%s'...", result.hdrFile());
            try (InputStream inputStream = new FileInputStream(result.hdrFile())) {
                result.loadHdrData(inputStream, analyzerConfig.sleConfig, getIntervals());
            } catch (Exception e) {
                throw new TussleException(e);
            }
        }
        result.getMetrics(metricData, analyzerConfig.allPercentiles ? percentilesLong : percentilesShort);
        hdrResults.add(result);
    }
}
