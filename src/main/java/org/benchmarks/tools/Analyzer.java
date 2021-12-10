package org.benchmarks.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.benchmarks.RunProperties;
import org.benchmarks.metrics.Interval;
import org.benchmarks.metrics.MetricData;
import org.benchmarks.metrics.SLA;
import org.benchmarks.tools.processors.DiskstatProcessor;
import org.benchmarks.tools.processors.HiccupProcessor;
import org.benchmarks.tools.processors.IpstatProcessor;
import org.benchmarks.tools.processors.RunPropertiesProcessor;
import org.benchmarks.tools.processors.TLPStressProcessor;
import org.benchmarks.tools.processors.TopProcessor;
import org.yaml.snakeyaml.Yaml;

public class Analyzer {

    public class MetricDataDoc {
        public MetricData doc;
        public MetricDataDoc() {}
        public MetricDataDoc(MetricData doc) {
            this.doc = doc;
        }
    }

    public static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Analyzer.class.getName());
    public static final String[] EMPT = {};

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

    protected TreeSet<String> processedFiles;
    protected MetricData metricData;
    protected AnalyzerConfig analyzerConfig;
    protected ArrayList<HdrResult> hdrResults;

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

    public void processResults(String[] args) throws Exception {
        processResults(loadConfig(args));
    }

    public void processResults(AnalyzerConfig config) throws Exception {
        init(config);
        processRecursive();
        printResults();
    }

    public void init(String[] args) throws IOException, ReflectiveOperationException {
        init(loadConfig(args));
    }

    public void init(AnalyzerConfig config) {
        analyzerConfig = config;
        if (analyzerConfig.slaConfig == null || analyzerConfig.slaConfig.length == 0) {
            analyzerConfig.slaConfig = new SLA[] {
                    // new SLA(90, 0, 10),
            };
        }
        if (analyzerConfig.intervals == null || analyzerConfig.intervals.length == 0) {
            analyzerConfig.intervals = new Interval[] {
                    new Interval(0, 1000000, ""),
                    // new Interval(2700, 4500, "POST_WARMUP45"),
            };
        }
        log("Config: %s", new Yaml().dump(analyzerConfig).trim());
        log(analyzerConfig.toString());
        metricData = new MetricData();
        processedFiles = new TreeSet<>();
        hdrResults = new ArrayList<>();
    }

    public AnalyzerConfig loadConfig(String[] args) throws IOException, ReflectiveOperationException  {
        return ConfigLoader.loadObject(args, AnalyzerConfig.class);
    }

    public void processRecursive() throws Exception {
        processRecursive(new File(analyzerConfig.getResultsDir()));
    }

    public void printResults() throws Exception {
        if (metricData.getRunProperties() == null) {
            metricData.setRunProperties(new RunProperties());
        }
        File metricsJson = new File(analyzerConfig.getResultsDir(), "metrics.json");
        try (PrintStream out = new PrintStream(metricsJson)) {
            if (analyzerConfig.doc) {
                JsonTool.printJson(new MetricDataDoc(metricData), out);
            } else {
                JsonTool.printJson(metricData, out);
            }
        }
        if (analyzerConfig.isMakeReport()) {
            if (analyzerConfig.doc) {
                Reporter.make(metricData, analyzerConfig.getReportDir());
            } else {
                Reporter.make(analyzerConfig.getReportDir(), metricsJson.getAbsolutePath());
            }
        }
    }

    public static String[] getTypes(SLA[] slaConfig) {
        ArrayList<String> types = new ArrayList<>();
        for (int i = 0; i < slaConfig.length; i++) {
            types.add("P" + FormatTool.format(slaConfig[i].percentile) + "_VALUES");
        }
        return types.toArray(EMPT);
    }

    public static boolean isRunPropertiesFile(String name) {
        return name.equals("run.properties.json") || name.equals("run-properties.json");
    }

    public static boolean isTopFile(String name) {
        return name.endsWith("top.log");
    }

    public static boolean isMpstatFile(String name) {
        return name.endsWith("mpstats.log") || name.endsWith("mpstat.log");
    }

    public static boolean isDiskstatFile(String name) {
        return name.endsWith("diskstats.log") || name.endsWith("diskstat.log");
    }

    public static boolean isIpstatFile(String name) {
        return name.endsWith("ipstats.log") || name.endsWith("ipstat.log");
    }

    public static boolean isHiccupFile(String name) {
        return name.startsWith("hiccup") && name.endsWith(".hlog");
    }

    public static boolean isHistogramFile(String name) {
        return name.endsWith(".hgrm") && !name.contains("processed") ||
                name.startsWith("tlp_stress_metrics") && name.indexOf(".hdr-") > 0;
    }

    public static boolean isTLPStressResults(String name) {
        return name.startsWith("tlp_stress_metrics") && name.endsWith(".csv");
    }

    public static boolean isResultsFile(String fileName) {
        return isRunPropertiesFile(fileName)
                || isTopFile(fileName)
                || isMpstatFile(fileName)
                || isDiskstatFile(fileName)
                || isIpstatFile(fileName)
                || isHiccupFile(fileName)
                || isHistogramFile(fileName)
                || isTLPStressResults(fileName);
    }

    public boolean processResultsStream(InputStream inputStream, String host, String fileName) {
        boolean res = true;
        if (isRunPropertiesFile(fileName)) {
            new RunPropertiesProcessor().processData(metricData, inputStream, host, logger);
        } else if (isTopFile(fileName)) {
            new TopProcessor().processData(metricData, inputStream, host, logger);
        } else if (isMpstatFile(fileName)) {
            // TODO:
        } else if (isDiskstatFile(fileName)) {
            new DiskstatProcessor().processData(metricData, inputStream, host, logger);
        } else if (isIpstatFile(fileName)) {
            new IpstatProcessor().processData(metricData, inputStream, host, logger);
        } else if (isHiccupFile(fileName)) {
            new HiccupProcessor().processData(metricData, inputStream, host, logger);
        } else if (isHistogramFile(fileName)) {
            processResultHistograms(fileName, inputStream);
        } else if (isTLPStressResults(fileName)) {
            new TLPStressProcessor().processData(metricData, inputStream, host, logger);
        } else {
            res = false;
        }
        return res;
    }

    protected void processRecursive(File dir) throws Exception {
        log("processRecursive: " + dir);
        if (!dir.exists()) {
            throw new IOException(String.format("Input dir '%s' does not exists", dir));
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

    public boolean processFile(File file) throws IOException {
        if (file.getName().endsWith(".zip")) {
            processZipFile(file);
            return true;
        } else {
            return processResultsFile(file, null, null);
        }
    }

    public void processZipFile(File file) throws IOException {
        log("Processing ZIP file: " + file);
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.isDirectory()) {
                    continue;
                }
                processResultsFile(file, zipFile, zipEntry);
            }
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
     * Extract host name from directory name, e.g. /path/to/node_actor@hostname -> 'hostname' 
     * 
     * @param dir
     * @return
     */
    public static String detectHost(File dir) {
        String host = "";
        if (dir == null)
            return host;
        String dirName = dir.getName();
        if (dirName.startsWith("node_")) {
            host = dirName.substring(dirName.indexOf('_') + 1);
        } else if (dir.getParentFile() != null) {
            dirName = dir.getParentFile().getName();
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

    public boolean processResultsFile(File file, ZipFile zipFile, ZipEntry zipEntry) throws IOException {
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
            String host = detectHost(dir);
            if (zipEntry != null) {
                log("Processing file from archive '%s//%s'...", dir, file.getName());
                try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
                    return processResultsStream(inputStream, host, file.getName());
                }
            } else {
                log("Processing file '%s'...", file);
                try (InputStream inputStream = new FileInputStream(file)) {
                    return processResultsStream(inputStream, host, file.getName());
                }
            }
        } else {
            return false;
        }
    }

    public void processResultHistograms(String fileName, InputStream inputStream) {
        log("Processing histogram file '%s'...", fileName);
        HdrResult result = HdrResult.getIterationResult(fileName);
        addAndProcessHistograms(result, inputStream);
    }

    public void processResultHistograms(HdrResult result) throws IOException {
        log("Processing histogram file '%s'...", result.hdrFile);
        try (InputStream inputStream = new FileInputStream(result.hdrFile)) {
            addAndProcessHistograms(result, inputStream);
        }
    }

    public void addAndProcessHistograms(HdrResult result, InputStream inputStream) {
        result.processHistograms(metricData, inputStream, analyzerConfig.slaConfig, analyzerConfig.intervals, analyzerConfig.allPercentiles ? percentilesLong : percentilesShort, analyzerConfig.mergeHistos);
        hdrResults.add(result);
    }
}
