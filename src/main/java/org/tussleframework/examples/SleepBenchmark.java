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

package org.tussleframework.examples;

import org.tussleframework.RunnableWithError;
import org.tussleframework.TussleException;
import org.tussleframework.WlBenchmark;
import org.tussleframework.WlConfig;
import org.tussleframework.tools.FormatTool;
import org.tussleframework.tools.SleepTool;

public class SleepBenchmark extends WlBenchmark {

    public SleepBenchmark() {
    }

    public SleepBenchmark(String[] args) throws TussleException {
        init(args);
    }

    public boolean sleep() {
        SleepBenchmarkConfig config = (SleepBenchmarkConfig) this.config;
        SleepTool.sleepSpinning(FormatTool.parseTimeNs(config.sleep));
        return true;
    }

    @Override
    public RunnableWithError getWorkload() {
        return this::sleep;
    }

    @Override
    public String getOperationName() {
        return "sleep";
    }

    @Override
    public Class<? extends WlConfig> getConfigClass() {
        return SleepBenchmarkConfig.class;
    }
}
