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

package org.tussleframework.runners;

import org.tussleframework.TussleException;
import org.tussleframework.metrics.MovingWindowSLE;
import org.tussleframework.tools.ConfigLoader;
import org.tussleframework.tools.FormatTool;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StepRaterConfig extends BasicRunnerConfig {
    public int retriesMax = 2;
    public int rateStepPercent = 1;
    public int startingRatePercent = 50;
    public int finishingRatePercent = 110;
    public int finerRateSteps = 0;
    public double rateFactor = 1.01;
    public double rangeStartTime = 0.0;
    public boolean resetEachStep = true;
    public boolean highBoundOnly = false;
    public boolean highBoundFromMaxRate = false;
    public boolean processHistograms = false;
    public String highBoundRunTime = "1m";
    public String highBoundWarmupTime = "0";
    public String initialWarmupTime = "0";
    public String initialRunTime = "1m";
    public String initialTargetRate = "1k";
    public int[] highBoundSteps = { 20000, 10000, 5000, 1000 };

    @Override
    public void validate(boolean runMode) {
        if (FormatTool.parseTimeLength(highBoundRunTime) < 0) {
            throw new IllegalArgumentException(String.format("Invalid highBoundRunTime(%s) - should be non-negative", highBoundRunTime));
        }
        if (FormatTool.parseTimeLength(highBoundWarmupTime) < 0) {
            throw new IllegalArgumentException(String.format("Invalid highBoundWarmupTime(%s) - should be non-negative", highBoundWarmupTime));
        }
        if (FormatTool.parseTimeLength(initialWarmupTime) < 0) {
            throw new IllegalArgumentException(String.format("Invalid initialWarmupTime(%s) - should be non-negative", initialWarmupTime));
        }
        if (FormatTool.parseTimeLength(initialRunTime) < 0) {
            throw new IllegalArgumentException(String.format("Invalid initialRunTime(%s) - should be non-negative", initialRunTime));
        }
        if (rateStepPercent <= 0) {
            throw new IllegalArgumentException(String.format("Invalid ratePercentStep(%d) - should be positive", rateStepPercent));
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
            if (highBoundSteps[i] >= highBoundSteps[i - 1]) {
                throw new IllegalArgumentException(String.format("Invalid highBoundStep[%d](%d) - should be > highBoundStep[%d](%d)", i, highBoundSteps[i], i - 1, highBoundSteps[i - 1]));
            }
        }
        super.validate(runMode);
    }

    public static StepRaterConfig load(String[] args) throws TussleException  {
        StepRaterConfig runnerConfig = ConfigLoader.loadObject(args, StepRaterConfig.class);
        if (runnerConfig.sleConfig == null || runnerConfig.sleConfig.length == 0) {
            runnerConfig.sleConfig = new MovingWindowSLE[] {
                    new MovingWindowSLE(90, 1, 10),
            };
        }
        return runnerConfig;
    }
}
