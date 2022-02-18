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
import java.util.logging.Logger;
import java.util.stream.DoubleStream;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.HistogramLogReader;
import org.tussleframework.metrics.Metric;
import org.tussleframework.metrics.MetricData;
import org.tussleframework.metrics.MetricValue;

public class HiccupProcessor implements DataLogProcessor {
    @Override
    public boolean processData(MetricData metricData, InputStream inputStream, String host, Logger logger) {
        long start = 0;
        long finish = 0;
        DoubleStream.Builder valBuffers = DoubleStream.builder();
        try (HistogramLogReader hdrReader = new HistogramLogReader(inputStream)) {
            int nulls = 0;
            while (true) {
                AbstractHistogram interval = (AbstractHistogram) hdrReader.nextIntervalHistogram(0, Double.MAX_VALUE);
                if (interval != null) {
                    if (start == 0) {
                        start = interval.getStartTimeStamp();
                    }
                    valBuffers.add(interval.getMaxValue() / 1000000.0);
                } else if (nulls++ > 10) {
                    break;
                }
            }
        }
        metricData.add(Metric.builder()
                .name("hiccup_times")
                .units("ms")
                .host(host)
                .start(start)
                .finish(finish)
                .delay(5000)
                .build()
                .add(new MetricValue("VALUES", valBuffers.build().toArray())));
       return true;
   }
}
