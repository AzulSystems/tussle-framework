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

package org.tussleframework;

import java.util.ArrayList;

import org.tussleframework.tools.FormatTool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunParams implements AbstractConfig {

    public String targetRate;
    public String warmupTime;
    public String runTime;

    @Override
    public void validate(boolean runMode) {
        if (FormatTool.parseValue(targetRate) < 0) {
            throw new IllegalArgumentException(String.format("Invalid targetRate(%s) - should be non-negative", targetRate));
        }
        if (FormatTool.parseTimeLength(warmupTime) < 0) {
            throw new IllegalArgumentException(String.format("Invalid warmupTime(%s) - should be non-negative", warmupTime));
        }
        if (FormatTool.parseTimeLength(runTime) < 0) {
            throw new IllegalArgumentException(String.format("Invalid runTime(%s) - should be non-negative", runTime));
        }
        if (FormatTool.parseTimeLength(warmupTime) + FormatTool.parseTimeLength(runTime) <= 0) {
            throw new IllegalArgumentException(String.format("Invalid warmupTime(%s) or runTime(%s) - sum should be positive", warmupTime, runTime));
        }        
    }

    public static RunParams[] spike(String min, String max, String minLen, String maxLen) {
        return new RunParams[] {
                new RunParams(min, "0", minLen),
                new RunParams(max, "0", maxLen),
                new RunParams(min, "0", minLen)
        };
    }

    public static RunParams[] ramp(int n, double from, double to) {
        ArrayList<RunParams> arr = new ArrayList<>();
        if (n > 1) {
            double d = (to - from) / (n - 1);
            for (int i = 0; i < n; i++) {
                arr.add(new RunParams(String.valueOf(from), "0", "1m"));
                from += d;
            }
        } else if (n == 1) {
            arr.add(new RunParams(String.valueOf(from), "0", "1m"));
        }
        return arr.toArray(new RunParams[0]);
    }
}
