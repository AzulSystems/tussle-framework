package org.tussleframework;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.tussleframework.tools.LoggerTool;

public class Run {

    static final IllegalArgumentException USAGE =
            new IllegalArgumentException("Expected parameters: benchmark-class-name [benchmark-args...] [--runner runner-class-name [runner-args...]]");

    Run() {}

    public static void main(String[] args) throws ClassNotFoundException {
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
            }   
            try {
                return ClassLoader.getSystemClassLoader().loadClass("org.tussleframework.steprater." + className);
            } catch (ClassNotFoundException e) {
            }
            try {
                return ClassLoader.getSystemClassLoader().loadClass("org.tussleframework.examples." + className);
            } catch (ClassNotFoundException e) {
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
     * @param args - benchmark-class-name [benchmark-args...] --runner runner-class-name [runner-args...] [-- ignoreable-args]
     */
    public static void run(String[] args) throws ClassNotFoundException {
        LoggerTool.init("benchmark");
        if (args.length == 0) {
            throw USAGE;
        }
        String[] benchmarkArgs = benchmarkArgs(args);
        benchmarkArgs = Arrays.copyOfRange(benchmarkArgs, 1, benchmarkArgs.length);
        String[] defRunner = { "BasicRunner" };
        String[] runnerArgs = runnerArgs(args, defRunner);
        String benchmarkClassName = args[0];
        String runnerClassName = runnerArgs[0];
        runnerArgs = Arrays.copyOfRange(runnerArgs, 1, runnerArgs.length);
        @SuppressWarnings("unchecked")
        Class<? extends Benchmark> benchmarkClass = (Class<? extends Benchmark>) findTussleClass(benchmarkClassName);
        @SuppressWarnings("unchecked")
        Class<? extends Runner> runnerClass = (Class<? extends Runner>) findTussleClass(runnerClassName);
        run(benchmarkClass, benchmarkArgs, runnerClass, runnerArgs);
    }

    public static void run(Benchmark benchmark, String[] args) {
        try {
            String[] defRunner = { "BasicRunner" };
            String[] runnerArgs = runnerArgs(args, defRunner);
            String runnerClassName = runnerArgs[0];
            runnerArgs = Arrays.copyOfRange(runnerArgs, 1, runnerArgs.length);
            @SuppressWarnings("unchecked")
            Class<? extends Runner> runnerClass = (Class<? extends Runner>) findTussleClass(runnerClassName);
            run(benchmark, benchmarkArgs(args), runnerClass.getConstructor().newInstance(), runnerArgs);
        } catch (Exception e) {
            LoggerTool.logException(e);
        }
    }

    public static void run(Class<? extends Benchmark> benchmarkClass, String[] benchmarkArgs, Class<? extends Runner> runnerClass, String[] runnerArgs) {
        try {
            run(benchmarkClass.getConstructor().newInstance(), benchmarkArgs, runnerClass.getConstructor().newInstance(), runnerArgs);
        } catch (Exception e) {
            LoggerTool.logException(e);
        }
    }

    public static void run(Benchmark benchmark, String[] benchmarkArgs, Runner runner, String[] runnerArgs) throws TussleException {
        runner.init(runnerArgs);
        benchmark.init(benchmarkArgs);
        runner.run(benchmark);
        benchmark.cleanup();
    }

    public static void run(Benchmark benchmark, Runner runner, String[] args) throws TussleException {
        runner.init(runnerArgs(args));
        benchmark.init(benchmarkArgs(args));
        runner.run(benchmark);
        benchmark.cleanup();
    }
}
