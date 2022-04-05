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

package org.tussleframework.steprater;

import org.tussleframework.tools.AnalyzerConfig;
import org.tussleframework.tools.FormatTool;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StepRaterConfig extends AnalyzerConfig {
    public int retriesMax = 2;
    public int ratePercentStep = 1;
    public int intervalLength = 1000;
    public int startingRatePercent = 50;
    public int finishingRatePercent = 110;
    public int finerRateSteps = 0;
    public double targetFactor = 1.01;
    public double rangeStartTime = 0.0;
    public boolean resetEachStep = true;
    public boolean highboundOnly = false;
    public boolean processHistograms = false;
    public String highBoundTime = "0";
    public String highBoundWarmupTime = "0";
    public String startupWarmupTime = "60";
    public int[] highBoundSteps = { 20000, 10000, 5000, 1000 };

    @Override
    public void validate(boolean runMode) {
        super.validate(runMode);
        if (FormatTool.parseValue(highBound) < 0) {
            throw new IllegalArgumentException(String.format("Invalid highBound(%s) - should be non-negative", highBound));
        }
        if (FormatTool.parseTimeLength(highBoundTime) < 0) {
            throw new IllegalArgumentException(String.format("Invalid highBoundTime(%s) - should be non-negative", highBoundTime));
        }
        if (FormatTool.parseTimeLength(highBoundWarmupTime) < 0) {
            throw new IllegalArgumentException(String.format("Invalid highBoundWarmupTime(%s) - should be non-negative", highBoundWarmupTime));
        }
        if (ratePercentStep <= 0) {
            throw new IllegalArgumentException(String.format("Invalid ratePercentStep(%d) - should be positive", ratePercentStep));
        }
        if (startingRatePercent < 0) {
            throw new IllegalArgumentException(String.format("Invalid startingRatePercent(%d) - should be non-negative", startingRatePercent));
        }
        if (finishingRatePercent < startingRatePercent) {
            throw new IllegalArgumentException(String.format("Invalid finishingRatePercent(%d) - should be >= startingRatePercent(%d)", finishingRatePercent, startingRatePercent));
        }
        if (highBoundSteps == null || highBoundSteps.length == 0) {
            throw new IllegalArgumentException("Invalid highBoundSteps - shouldn't be empty");   
        }
        for (int i = 0; i < highBoundSteps.length; i++) {
            if (highBoundSteps[i] <= 0) {
                throw new IllegalArgumentException(String.format("Invalid highBoundStep[%d](%d) - should be non-negative", i, highBoundSteps[i]));
            }
        }
        for (int i = 1; i < highBoundSteps.length; i++) {
            if (highBoundSteps[i] <= highBoundSteps[i - 1]) {
                throw new IllegalArgumentException(String.format("Invalid highBoundStep[%d](%d) - should be > highBoundStep[%d](%d)", i, highBoundSteps[i], i - 1, highBoundSteps[i - 1]));
            }
        }
    }
}