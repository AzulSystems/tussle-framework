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

import java.util.Random;

import org.tussleframework.RunnableWithError;
import org.tussleframework.TussleException;
import org.tussleframework.WlBenchmark;
import org.tussleframework.WlConfig;
import org.tussleframework.tools.FormatTool;

public class StringBenchmark extends WlBenchmark {

    public long sumLenghs;
    protected Random random = new Random();

    public StringBenchmark() {
    }

    public StringBenchmark(String[] args) throws TussleException {
        init(args);
    }

    public boolean string() {
        StringBenchmarkConfig config = (StringBenchmarkConfig) this.config;
        int len = FormatTool.parseInt(config.len);
        int lenMax = FormatTool.parseInt(config.lenMax);
        if (lenMax > len) {
            len = random.nextInt(lenMax - len) + len;
        }
        char[] azul = "Azul".toCharArray();
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < len; i++) {
            s.append(azul[random.nextInt(azul.length)]);
        }
        sumLenghs += s.length();
        return true;
    }

    @Override
    public RunnableWithError getWorkload() {
        return this::string;
    }

    @Override
    public String getOperationName() {
        return "string";
    }
    
    @Override
    public void cleanup() {
        super.cleanup();
        log("StringBenchmark sumLenghs = %d", sumLenghs);
    }

    @Override
    public Class<? extends WlConfig> getConfigClass() {
        return StringBenchmarkConfig.class;
    }
}
