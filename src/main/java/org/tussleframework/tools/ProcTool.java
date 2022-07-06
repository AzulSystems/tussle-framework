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

package org.tussleframework.tools;

import static org.tussleframework.tools.FormatTool.paramName;
import static org.tussleframework.tools.FormatTool.paramValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.tussleframework.TussleException;
import org.tussleframework.TussleTimeoutException;

class ProcState {
    volatile boolean stopped;
    String name;
    ProcState(String name) {
        this.name = name;
    }
}

@FunctionalInterface
interface ProcCB {
    void onStart(Process proc, ProcState state) throws TussleException;
}

public class ProcTool {

    public static class CmdVars {
        public String delay = "10m";
        public String dir;
        public String[] env = {}; // list of ENV_VAR=value pairs
        public String[] cmd = {};

        public CmdVars setDelay(int delay) {
            this.delay = String.valueOf(delay);
            return this;
        }

        public boolean isEmpty() {
            return cmd == null || cmd.length == 0 || cmd[0].isEmpty();
        }
    }

    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("tussle.debug"));

    private static void log(String format, Object... args) {
        LoggerTool.log(ProcTool.class.getSimpleName(), format, args);
    }

    private ProcTool() {
    }

    public static void saveStream(InputStream is, OutputStream os, ProcState state) {
        byte[] buff = new byte[1024];
        int n;
        try {
            while ((n = is.read(buff)) != -1 && !state.stopped) {
                os.write(buff, 0, n);
            }
        } catch (IOException e) {
            log("Stream of '%s': %s", state.name, e.getMessage());
        }
    }

    public static Thread startStream(InputStream is, OutputStream os, ProcState state) {
        Thread thread = new Thread(() -> saveStream(is, os, state));
        thread.start();
        return thread;
    }

    public static Thread startStream(InputStream is, String file, ProcState state) throws IOException {
        try (FileOutputStream os = new FileOutputStream(file)) {
            return startStream(is, os, state);
        }
    }

    public static void startProcess(String name, String procDir, Collection<String> args, int expectedRunTime) throws TussleException {
        runProcess(name, procDir, args, null, expectedRunTime, null, null);
    }

    public static void runProcess(String name, String procDir, Collection<String> args, Collection<String> env, int expectedRunTime) throws TussleException {
        runProcess(name, procDir, args, env, expectedRunTime, null);
    }

    public static void runProcess(String name, String procDir, String[] args, String[] env, int expectedRunTime, OutputStream os) throws TussleException {
        runProcess(name, procDir, Arrays.asList(args), Arrays.asList(env), expectedRunTime, os, os);
    }

    public static void runProcess(String name, CmdVars vars, OutputStream os) throws TussleException {
        runProcess(name, vars.dir, Arrays.asList(vars.cmd), Arrays.asList(vars.env), FormatTool.parseTimeLength(vars.delay), os);
    }

    public static void runProcess(String name, String procDir, Collection<String> args, Collection<String> env, int expectedRunTime, OutputStream os) throws TussleException {
        runProcess(name, procDir, args, env, expectedRunTime, os, os);
    }

    public static void runProcess(String name, String procDir, Collection<String> args, Collection<String> env, int expectedRunTime, OutputStream stdout, OutputStream stderr) throws TussleException {
        runProcessCB(name, procDir, args, env, expectedRunTime, (proc, state) -> {
            if (stdout != null) {
                startStream(proc.getInputStream(), stdout, state);
                startStream(proc.getErrorStream(), stderr != null ? stderr : stdout, state);
            } else {
                try {
                    startStream(proc.getInputStream(), name + "-stdout.log", state);
                    startStream(proc.getErrorStream(), name + "-stderr.log", state);
                } catch (IOException e) {
                    throw new TussleException(e);
                }
            }
        });
    }

    /**
     * Process run tool
     * 
     * @param name            - name of a process
     * @param procDir         - process working dir
     * @param args            - command line args
     * @param env             - list of optional environment variables in the format [VAR1=value1,VAR2=value2,...]
     * @param expectedRunTime - expected process run time after which it is killed forcibly
     * 
     * @throws TussleException - process run error or timeout
     */
    public static void runProcessCB(String name, String procDir, Collection<String> args, Collection<String> env, int expectedRunTime, ProcCB procCB) throws TussleException {
        log("Starting process '%s' [%s] (dir: %s), expected exec time %s seconds", name, FormatTool.join(" ", args), procDir != null ? procDir : ".", expectedRunTime);
        Process proc = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(new ArrayList<>(args));
            if (procDir != null) {
                File dir = new File(procDir);
                dir.mkdirs();
                pb.directory(dir);
            }
            if (env != null && !env.isEmpty()) {
                Map<String, String> e = pb.environment();
                env.forEach(es -> e.put(paramName(es), paramValue(es)));
                if (DEBUG) {
                    e.forEach((key, value) -> log(" env var %s = %s", key, value));
                }
            }
            ProcState state = new ProcState(name);
            proc = pb.start();
            if (procCB != null) {
                procCB.onStart(proc, state);
            }
            if (proc.waitFor(expectedRunTime, TimeUnit.SECONDS)) {
                state.stopped = true;
                int exitCode = proc.exitValue();
                log("Process finished with exitCode %d", exitCode);
                if (exitCode != 0) {
                    throw new TussleException("Process exited with error code " + exitCode);                    
                }
            } else {
                state.stopped = true;
                log("Process has not finished, destroying...");
                proc.destroy();
                throw new TussleTimeoutException("Process has not finished during expected time " + expectedRunTime + " seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TussleException(e);
        } catch (IOException e) {
            throw new TussleException(e);
        } finally {
            if (proc != null) {
                try {
                    proc.destroy();
                } catch (Exception e) {
                    ///
                }
            }
        }
    }
}
