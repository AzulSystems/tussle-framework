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

package org.tussleframework.tools.processors;

import java.io.InputStream;
import java.util.Scanner;
import java.util.logging.Logger;

import org.tussleframework.metrics.HdrData;
import org.tussleframework.metrics.Metric;
import org.tussleframework.metrics.MetricData;

public class CompileLogProcessor implements DataLogProcessor {

	@Override
	public boolean processData(MetricData metricData, HdrData hdrData, InputStream inputStream, String host,
			Logger logger) {
		int step = 0;
		int count_of_methods_lvl0 = 0;
		int count_of_methods_lvl1 = 0;
		int count_of_methods_lvl2 = 0;
		int count_of_osr_methods_lvl0 = 0;
		int count_of_osr_methods_lvl1 = 0;
		int count_of_osr_methods_lvl2 = 0;
		try (Scanner scanner = new Scanner(inputStream)) {
			while (scanner.hasNext()) {
				String line = scanner.nextLine().trim();
				if (line.startsWith("Compile Queue after VM init")) {
					// Compile Queue after VM init [2s]:
					step = 1;
				} else if (line.startsWith("Compile Queue at VM exit")) {
					// Compile Queue at VM exit [678s]:
					step = 2;
				} else if (line.indexOf(" 3 installed") >= 0) {
					if (line.indexOf("%") >= 0) {
						if (line.indexOf("lvl O0") >= 0) {
							count_of_osr_methods_lvl0++;
						} else if (line.indexOf("lvl O1") >= 0) {
							count_of_osr_methods_lvl1++;
						} else if (line.indexOf("lvl O2") >= 0) {
							count_of_osr_methods_lvl2++;
						}
					} else {
						if (line.indexOf("lvl O0") >= 0) {
							count_of_methods_lvl0++;
						} else if (line.indexOf("lvl O1") >= 0) {
							count_of_methods_lvl1++;
						} else if (line.indexOf("lvl O2") >= 0) {
							count_of_methods_lvl2++;
						}
					}
				}
			}
		}
		int total_count =
				count_of_methods_lvl0 +
				count_of_methods_lvl1 +
				count_of_methods_lvl2 +
				count_of_osr_methods_lvl2 +
				count_of_osr_methods_lvl1 +
				count_of_osr_methods_lvl0;
		metricData.add(Metric.builder()
				.name("tier2_total_count").host(host)
				.value(Double.valueOf(total_count)).build());
		metricData.add(Metric.builder()
				.name("tier2_installed_methods_lvl0").host(host)
				.value(Double.valueOf(count_of_methods_lvl0)).build());
		metricData.add(Metric.builder()
				.name("tier2_installed_methods_lvl1").host(host)
				.value(Double.valueOf(count_of_methods_lvl1)).build());
		metricData.add(Metric.builder()
				.name("tier2_installed_methods_lvl2").host(host)
				.value(Double.valueOf(count_of_methods_lvl2)).build());
		metricData.add(Metric.builder()
				.name("tier2_installed_osr_methods_lvl0").host(host)
				.value(Double.valueOf(count_of_osr_methods_lvl0)).build());
		metricData.add(Metric.builder()
				.name("tier2_installed_osr_methods_lvl1").host(host)
				.value(Double.valueOf(count_of_osr_methods_lvl1)).build());
		metricData.add(Metric.builder()
				.name("tier2_installed_osr_methods_lvl2").host(host)
				.value(Double.valueOf(count_of_osr_methods_lvl2)).build());
		return true;
	}
}
