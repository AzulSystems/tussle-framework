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
import java.util.logging.Logger;
import java.util.stream.DoubleStream;

import org.tussleframework.metrics.HdrData;
import org.tussleframework.metrics.Metric;
import org.tussleframework.metrics.MetricData;
import org.tussleframework.metrics.MetricValue;
import org.tussleframework.tools.FormatTool;
import org.tussleframework.tools.LoggerTool;

public class DiskstatProcessor implements DataLogProcessor {

    /**
     * Supported format:
DELAY: 5
START: 2021-09-08 06:33:11,510
HOST: hostname

Linux 5.4.0-1045-aws (ip-172-31-94-166) <------>09/08/21 <----->_x86_64_<------>(8 CPU)

06:33:11          DEV       tps     rkB/s     wkB/s   areq-sz    aqu-sz     await     svctm     %util
06:33:16        loop0      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00
06:33:16        loop1      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00
06:33:16        loop2      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00
06:33:16      nvme1n1      0.60      0.00      4.80      8.00      0.00      0.00      4.00      0.24
06:33:16      nvme0n1      0.20      0.80      0.00      4.00      0.00      1.00      4.00      0.08
...

or

08:17:47 AM       DEV       tps  rd_sec/s  wr_sec/s  avgrq-sz  avgqu-sz     await     svctm     %util
08:17:52 AM   nvme0n1     12.80     44.80    251.00     23.11      0.02      1.39      0.16      0.20

08:17:52 AM       DEV       tps  rd_sec/s  wr_sec/s  avgrq-sz  avgqu-sz     await     svctm     %util
08:17:57 AM   nvme0n1      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00

08:17:57 AM       DEV       tps  rd_sec/s  wr_sec/s  avgrq-sz  avgqu-sz     await     svctm     %util
...
     */
    @Override
    public boolean processData(MetricData metricData, HdrData hdrData, InputStream inputStream, String host, Logger logger) {
        long start = 0;
        long finish = 0;
        int intervalLength = 5000;
        DoubleStream.Builder buffValues = DoubleStream.builder();
        try (Scanner scanner = new Scanner(inputStream)) {
            boolean accum = false;
            double accumValue = 0; 
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line.startsWith("DELAY:")) {
                    intervalLength = Integer.valueOf(line.substring("DELAY:".length()).trim()) * 1000;
                } else if (line.startsWith("START:")) {
                    finish = start = FormatTool.parseUTCDate(line);
                } else if (line.startsWith("HOST:")) {
                    host = line.substring("HOST:".length()).trim(); 
                } else if (accum) {
                    line = line.trim();
                    if (line.length() == 0) {
                        accum = false;
                        buffValues.add(accumValue);
                        accumValue = 0;
                    } else {
                        String[] s = line.split("\\s+");
                        if (s.length == 10 && s[0].length() == 8) {
                            accumValue += Double.valueOf(s[9].trim().replace(',', '.'));
                            finish += intervalLength;
                        } else if (s.length == 11 && s[0].length() == 8) {
                            accumValue += Double.valueOf(s[10].trim().replace(',', '.'));
                            finish += intervalLength;
                        } else {
                            break;
                        }
                    }
                } else if (line.indexOf("DEV") > 0) {
                    accum = true;
                }
            }
        } catch (Exception e) {
            LoggerTool.logException(logger, e);
            return false;
        }
        metricData.add(Metric.builder()
                .name("disk")
                .units("%util")
                .host(host)
                .start(start)
                .finish(finish)
                .delay(intervalLength)
                .build()
                .add(new MetricValue("VALUES", buffValues.build().toArray())));
        return true;
    }
}
