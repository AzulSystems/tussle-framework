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

import static org.tussleframework.tools.FormatTool.parseValue;
import static org.tussleframework.tools.FormatTool.parseInt;
import static org.tussleframework.tools.FormatTool.getParam;

import java.util.Arrays;

import org.tussleframework.RunParams;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Basic benchmark configuration
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScenarioRunnerConfig extends RunnerConfig {

    /**
     * sequence of runs, each steps consisting of its own target rate and run time used by ScenarioRunner
     */
    public RunParams[] scenario = {
            new RunParams("1k", "0", "1m"),
            new RunParams("2k", "0", "1m"),
            new RunParams("3k", "0", "1m")
    };

    /**
     * Predefined scenario: SPIKE, RAMP, etc. 
     */
    public String def;

    @Override
    public void validate(boolean runMode) {
        if (def != null) {
            if (def.startsWith("SPIKE")) {
                scenario = RunParams.spike(
                        getParam(def, 1, "1K"),
                        getParam(def, 2, "1M"),
                        getParam(def, 3, "5m"),
                        getParam(def, 4, "10s"));
            } else if (def.startsWith("RAMP")) {
                scenario = RunParams.ramp(
                        parseInt(getParam(def, 1, "10")),
                        parseValue(getParam(def, 2, "1k")),
                        parseValue(getParam(def, 3, "10k")),
                        getParam(def, 4, "1m"));
            }
        }
        if (scenario == null) {
            throw new IllegalArgumentException("Invalid scenario - null");
        }
        if (scenario.length == 0) {
            throw new IllegalArgumentException("Invalid scenario - 0 length");
        }
        Arrays.asList(scenario).forEach(s -> s.validate(runMode));
        for (int step = 0; step < scenario.length; step++) {
            if (parseValue(scenario[step].getTargetRate()) == 0) {
                serviceTimeOnly = true;
            }
        }
        super.validate(runMode);
    }
}
