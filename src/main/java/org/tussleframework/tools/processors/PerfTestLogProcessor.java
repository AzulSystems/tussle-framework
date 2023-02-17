/*
 * Copyright (c) 2021-2023, Azul Systems
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;

import org.tussleframework.metrics.HdrData;
import org.tussleframework.metrics.Metric;
import org.tussleframework.metrics.MetricData;
import org.tussleframework.metrics.MetricType;
import org.tussleframework.metrics.MetricValue;

public class PerfTestLogProcessor implements DataLogProcessor {

	@Override
	public boolean processData(MetricData metricData, HdrData hdrData, InputStream inputStream, String host, Logger logger) {
        Pattern pattern = Pattern.compile(".+ records sent, (.+) records/sec \\(.+\\), (.+) ms avg latency, (.+) ms max latency.");
        DoubleStream.Builder buffAvgValues = DoubleStream.builder();
        DoubleStream.Builder buffMaxValues = DoubleStream.builder();
        DoubleStream.Builder buffRecsValues = DoubleStream.builder();
		try (Scanner scanner = new Scanner(inputStream)) {
			while (scanner.hasNext()) {
				String line = scanner.nextLine().trim();
				Matcher matcher = pattern.matcher(line);
				if (matcher.matches()) {
				    buffRecsValues.add(Double.valueOf(matcher.group(1)));
				    buffAvgValues.add(Double.valueOf(matcher.group(2)));
				    buffMaxValues.add(Double.valueOf(matcher.group(3)));
				}
			}
		}
		metricData.add(Metric.builder()
                .operation("send")
				.name("throughput")
				.units("records/s")
				.host(host)
				.build()
				.add(new MetricValue(MetricType.VALUES, buffRecsValues.build().toArray()))
				);
		metricData.add(Metric.builder()
                .operation("send")
                .name("avg_latency")
                .units("ms")
                .host(host)
                .build()
                .add(new MetricValue(MetricType.VALUES, buffAvgValues.build().toArray()))
                );
		metricData.add(Metric.builder()
                .operation("send")
                .name("max_latency")
                .units("ms")
                .host(host)
                .build()
                .add(new MetricValue(MetricType.VALUES, buffMaxValues.build().toArray()))
                );
		return true;
	}
}
