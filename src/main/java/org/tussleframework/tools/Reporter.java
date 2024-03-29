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
import java.util.Arrays;
import java.util.logging.Level;

import org.tussleframework.Tool;
import org.tussleframework.TussleException;

public class Reporter implements Tool {

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
                String reportDir = args[0];
                String[] metricsJsons = Arrays.copyOfRange(args, 1, args.length);
                make(reportDir, metricsJsons);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

// TODEL
//    public static void make(MetricData metricData, String reportDir) throws TussleException {
//        try {
//            Files.createDirectories(Paths.get(reportDir));
//            File dataJson = new File(reportDir, "data.json");
//            try (PrintStream out = new PrintStream(dataJson)) {
//                log("Generating data.json: " + dataJson);
//                JsonTool.printJson(metricData, out);
//            }
//            make(reportDir, new String[] { dataJson.getAbsolutePath() });
//            Files.delete(dataJson.toPath());
//        } catch (Exception e) {
//            throw new TussleException(e);
//        }
//    }

    public static void make(String reportDir, String metricsJson) throws TussleException {
        make(reportDir, new String[] { metricsJson }); 
    }

    public static void make(String reportDir, String[] metricsJsons) throws TussleException {
        try {
            extractReportFiles(reportDir);
            int idx = 0;
            for (String metricsJson : metricsJsons) {
                idx++;
                File reportDirJs = new File(reportDir, "js");
                File dataJs = new File(reportDirJs, String.format("data%d.js", idx));
                Files.deleteIfExists(dataJs.toPath());
                log("Generating report data #%d js from '%s' ", idx, metricsJson);
                try (PrintStream out = new PrintStream(dataJs); BufferedReader br = new BufferedReader(new FileReader(metricsJson))) {
                    out.printf("window.metricsData%d = [{ \"_source\": ", idx);
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
        } catch (Exception e) {
            throw new TussleException(e);
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
