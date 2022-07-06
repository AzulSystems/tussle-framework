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

import static org.tussleframework.tools.FormatTool.applyArg;
import static org.tussleframework.tools.FormatTool.applyArgs;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.tussleframework.tools.FormatTool;
import org.tussleframework.tools.ProcTool.CmdVars;

/**
 * Basic benchmark configuration
 */
public class ProcConfig extends BenchmarkConfig {
    public String logPrefix;
    public String logSuffix;
    public String runResult;
    public CmdVars run = new CmdVars();
    public CmdVars init = new CmdVars();
    public CmdVars reset = new CmdVars();
    public CmdVars cleanup = new CmdVars();
    public String[] resultFiles = { ".*.hlog" };
    public Properties vars = new Properties();

    public ProcConfig() {
        name = "proc";
    }

    @Override
    public void validate(boolean runMode) {
        if (run == null || run.isEmpty()) {
            throw new IllegalArgumentException("Missing or empty required parameter: run command");
        }
        if (resultFiles != null) {
            Map<String, String> params = getArgs(new RunArgs());
            params.put("runDir", "mock_dir");
            for (String pattern : FormatTool.applyArgs(Arrays.asList(resultFiles.clone()), params)) {
                Pattern.compile(pattern);
            }
        }
    }

    public Map<String, String> getArgs(RunArgs runArgs) {
        Map<String, String> pairs = FormatTool.getSysMap(vars);
        pairs.put("targetRate", FormatTool.format(runArgs.targetRate));
        pairs.put("warmupTime", String.valueOf(runArgs.warmupTime));
        pairs.put("runTime", String.valueOf(runArgs.runTime));
        pairs.put("runStep", String.valueOf(runArgs.runStep));
        return pairs;
    }

    public static final String[] EMPTY = {};

    public CmdVars makeCmd(CmdVars c, Map<String, String> params) {
        CmdVars ret = new CmdVars();
        ret.dir = applyArg(c.dir, params);
        params.put("runDir", ret.dir);
        ret.env = applyArgs(Arrays.asList(c.env.clone()), params).toArray(EMPTY);
        ret.cmd = applyArgs(Arrays.asList(c.cmd.clone()), params).toArray(EMPTY);
        return ret;
    }
}
