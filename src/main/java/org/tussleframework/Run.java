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

import java.util.Arrays;
import java.util.stream.IntStream;

import org.tussleframework.tools.LoggerTool;

public class Run {

    static final IllegalArgumentException USAGE = new IllegalArgumentException("Expected parameters:  benchmark-class-name [benchmark-args...]  [--runner runner-class-name [runner-args...]]");

    private Run() {
    }

    public static void main(String[] args) {
        run(args);
    }

    public static Class<?> findTussleClass(String className) throws ClassNotFoundException {
        ClassNotFoundException err = null;
        try {
            return ClassLoader.getSystemClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            err = e;
        }
        if (className.indexOf('.') < 0) {
            try {
                return ClassLoader.getSystemClassLoader().loadClass("org.tussleframework." + className);
            } catch (ClassNotFoundException e) {
                /// ignore
            }
            try {
                return ClassLoader.getSystemClassLoader().loadClass("org.tussleframework.runners." + className);
            } catch (ClassNotFoundException e) {
                /// ignore
            }
            try {
                return ClassLoader.getSystemClassLoader().loadClass("org.tussleframework.steprater." + className);
            } catch (ClassNotFoundException e) {
                /// ignore
            }
            try {
                return ClassLoader.getSystemClassLoader().loadClass("org.tussleframework.examples." + className);
            } catch (ClassNotFoundException e) {
                /// ignore
            }
        }
        throw err;
    }

    public static String[] benchmarkArgs(String[] args0) {
        int idxStop = IntStream.range(0, args0.length).filter(i -> args0[i].equals("--")).findFirst().orElse(args0.length);
        String[] args = Arrays.copyOfRange(args0, 0, idxStop);
        int idx = IntStream.range(0, args.length).filter(i -> args[i].equals("--runner")).findFirst().orElse(args.length);
        return Arrays.copyOfRange(args, 0, idx);
    }

    public static String[] runnerArgs(String[] args0, String[] def) {
        int idxStop = IntStream.range(0, args0.length).filter(i -> args0[i].equals("--")).findFirst().orElse(args0.length);
        String[] args = Arrays.copyOfRange(args0, 0, idxStop);
        int idx = IntStream.range(0, args.length).filter(i -> args[i].equals("--runner")).findFirst().orElse(-1);
        return idx >= 0 ? Arrays.copyOfRange(args, idx + 1, args.length) : def;
    }

    public static String[] runnerArgs(String[] args) {
        return runnerArgs(args, new String[0]);
    }

    /**
     * @param args - benchmark-class-name [benchmark-args...]  [--runner runner-class-name [runner-args...]]  [-- ignorable args]
     */
    public static void run(String[] args) {
        LoggerTool.init("benchmark");
        if (args.length == 0) {
            throw USAGE;
        }
        String[] benchmarkArgs = benchmarkArgs(args);
        benchmarkArgs = Arrays.copyOfRange(benchmarkArgs, 1, benchmarkArgs.length);
        String[] defaultRunner = { "BasicRunner" };
        String[] runnerArgs = runnerArgs(args, defaultRunner);
        String benchmarkClassName = args[0];
        String runnerClassName = runnerArgs[0];
        runnerArgs = Arrays.copyOfRange(runnerArgs, 1, runnerArgs.length);
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Benchmark> benchmarkClass = (Class<? extends Benchmark>) findTussleClass(benchmarkClassName);
            @SuppressWarnings("unchecked")
            Class<? extends Runner> runnerClass = (Class<? extends Runner>) findTussleClass(runnerClassName);
            run(benchmarkClass, benchmarkArgs, runnerClass, runnerArgs);
        } catch (Exception e) {
            LoggerTool.logException(e);
        }
    }

    /**
     * @param benchmark - benchmark instance
     * @param args - [benchmark-args...]  [--runner runner-class [runner-args...]]  [-- ignoreable-args]
     */
    public static void run(Benchmark benchmark, String[] args) {
        try {
            String[] defaultRunner = { "BasicRunner" };
            String[] runnerArgs = runnerArgs(args, defaultRunner);
            String runnerClassName = runnerArgs[0];
            runnerArgs = Arrays.copyOfRange(runnerArgs, 1, runnerArgs.length);
            @SuppressWarnings("unchecked")
            Class<? extends Runner> runnerClass = (Class<? extends Runner>) findTussleClass(runnerClassName);
            run(benchmark, benchmarkArgs(args), runnerClass.getConstructor().newInstance(), runnerArgs);
        } catch (Exception e) {
            LoggerTool.logException(e);
        }
    }

    /**
     * @param benchmarkClass - benchmark class
     * @param benchmarkArgs - benchmark args
     * @param runnerClass - runner class
     * @param runnerArgs - runner args
     */
    public static void run(Class<? extends Benchmark> benchmarkClass, String[] benchmarkArgs, Class<? extends Runner> runnerClass, String[] runnerArgs) {
        try {
            run(benchmarkClass.getConstructor().newInstance(), benchmarkArgs, runnerClass.getConstructor().newInstance(), runnerArgs);
        } catch (Exception e) {
            LoggerTool.logException(e);
        }
    }

    /**
     * @param benchmark - benchmark instance
     * @param benchmarkArgs - benchmark args
     * @param runner - runner instance
     * @param runnerArgs - runner args
     * 
     * @throws TussleException
     */
    public static void run(Benchmark benchmark, String[] benchmarkArgs, Runner runner, String[] runnerArgs) throws TussleException {
        runner.init(runnerArgs);
        benchmark.init(benchmarkArgs);
        runner.run(benchmark);
        benchmark.cleanup();
    }

    /**
     * 
     * @param benchmark - benchmark instance
     * @param runner - runner instance
     * @param args - [benchmark-args...]  [--runner [runner-args...]]  [-- ignoreable-args]
     * @throws TussleException
     */
    public static void run(Benchmark benchmark, Runner runner, String[] args) throws TussleException {
        run(benchmark, benchmarkArgs(args), runner, runnerArgs(args));
    }
}
