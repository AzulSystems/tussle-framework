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

import static org.tussleframework.tools.FormatTool.roundFormat;

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
public class RunArgs {
    public double targetRate;
    public double ratePercent;
    public int warmupTime;
    public int runTime;
    public int runStep;
    public String name;

    public int fillValues(String[] parts) {
        int filledParts = 0;
        try {
            // operation-name _ metric-name _ percent-of-high-bound _ target-rate _ step
            // OR
            // operation-name _ percent-of-high-bound _ target-rate _ step
            runStep = Integer.valueOf(parts[parts.length - 1]);
            targetRate = Double.valueOf(parts[parts.length - 2]);
            ratePercent = Double.valueOf(parts[parts.length - 3]);
            filledParts = 3;
        } catch (NumberFormatException e) {
            try {
                // operation-name _ metric-name _ percent-of-high-bound _ step
                // OR
                // operation-name _ percent-of-high-bound _ step
                runStep = Integer.valueOf(parts[parts.length - 1]);
                targetRate = 0;
                ratePercent = Double.valueOf(parts[parts.length - 2]);
                filledParts = 2;
            } catch (NumberFormatException e2) {
                try {
                    // operation-name _ metric-name _ step
                    // OR
                    // operation-name _ step
                    runStep = Integer.valueOf(parts[parts.length - 1]);
                    targetRate = 0;
                    ratePercent = 0;
                    filledParts = 2;
                } catch (NumberFormatException e3) {
                    runStep = 0;
                    targetRate = 0;
                    ratePercent = 0;
                }
            }
        }
        return filledParts;
    }

    public String format(String rateUnits) {
        if (rateUnits == null) {
            rateUnits = "";
        } else {
            rateUnits = " " + rateUnits;
        }
        return String.format("target rate %s%s (%s%%), warmup %d s, run time %d s", roundFormat(targetRate), rateUnits, roundFormat(ratePercent), warmupTime, runTime);
    }
}
