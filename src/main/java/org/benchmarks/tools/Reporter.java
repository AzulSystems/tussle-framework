package org.benchmarks.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

import org.benchmarks.metrics.MetricData;

public class Reporter {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Reporter.class.getName());
    public static final String RES_REPORT = "/repfiles";
    private static final Class<Reporter> cloader = Reporter.class;

    public static void log(String format, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(String.format("[Reporter] %s", String.format(format, args)));
        }
    }

    public static void main(String[] args) {
        LoggerTool.init("reporter");
        try {
            if (args[0].equals("--get-res")) {
                extractReportFiles(args[1]);
            } else {
                make(args[0], args[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void make(MetricData metricData, String reportDir) throws Exception {
        Files.createDirectories(Paths.get(reportDir));
        File dataJson = new File(reportDir, "data.json");
        try (PrintStream out = new PrintStream(dataJson)) {
            log("Generating data.json: " + dataJson);
            JsonTool.printJson(metricData, out);
        }
        make(reportDir, dataJson.getAbsolutePath());
        Files.delete(dataJson.toPath());
    }

    public static void make(String reportDir, String metricsJson) throws IOException {
        extractReportFiles(reportDir);
        File reportDirJs = new File(reportDir, "js");
        File dataJs = new File(reportDirJs, "data.js");
        Files.deleteIfExists(dataJs.toPath());
        log("Generating report data.js: " + metricsJson);
        try (PrintStream out = new PrintStream(dataJs); BufferedReader br = new BufferedReader(new FileReader(metricsJson))) {
            out.print("const metricsData = [{ \"_source\": ");
            String line = br.readLine();
            int i = 0;
            while (line != null) {
                if (i > 0) out.println();
                out.print(line);
                line = br.readLine();
                i++;
            }
            out.println(" }];");
        }
    }

    private static final String[] RES = {
            "index.html",
            "css/app.css",
            "js/Chart.min.js",
            "js/angular-chart.min.js",
            "js/angular.min.js",
            "js/app.js",
            "js/utils.js"
    };

    public static void extractReportFiles(String reportDir) throws IOException {
        for (int i = 0; i < RES.length; i++) {
            InputStream is = cloader.getResourceAsStream(RES_REPORT + "/" + RES[i]);
            new File(reportDir, RES[i]).getParentFile().mkdirs();
            Path to = Paths.get(reportDir + "/" + RES[i]);
            log(" -- resource: %s -> %s", RES[i], to);
            Files.copy(is, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
