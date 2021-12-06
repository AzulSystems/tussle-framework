package org.benchmarks.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import org.benchmarks.BenchmarkConfig;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

public class ConfigLoader {

    private ConfigLoader() {
    }

    static final IllegalArgumentException USAGE =
            new IllegalArgumentException("Expected parameters: -f yaml-file | -s yaml-string | -p prop1=value1 -p prop2=value2 ... | prop1=value1 prop2=value2 ...");

    public static void readFile(String configFile, StringBuilder sb) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            String line = br.readLine();
            while (line != null) {
                sb.append(line).append('\n');
                line = br.readLine();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T loadObject(String s, Class<?> klass, boolean skipMissingProperties) throws ReflectiveOperationException {
        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(skipMissingProperties);
        Yaml yaml = new Yaml(new Constructor(klass), representer);
        T config = yaml.load(new StringReader(s));
        if (config == null) {
            config = (T) klass.getConstructor().newInstance();
        }
        return config;
    }

    public static void addProperty(StringBuilder sb, String propArg) {
        int pos = propArg.indexOf('=');
        if (pos <= 0) {
            throw USAGE;
        }
        String v = propArg.substring(pos + 1);
        propArg = propArg.substring(0, pos);
        sb.append(propArg).append(": ").append(v).append('\n');
    }

    public static <T> T loadObject(String[] args, Class<?> klass, boolean skipMissingProperties) throws IOException, ReflectiveOperationException {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        try {
            while (i < args.length) {
                if (args[i].equals("--")) {
                    break;
                }
                if (args[i].equals("--string") || args[i].equals("-s")) {
                    i++;
                    sb.append(args[i]).append('\n');
                } else if (args[i].equals("--file") || args[i].equals("-f")) {
                    i++;
                    readFile(args[i], sb);
                } else if (args[i].equals("--property") || args[i].equals("-p")) {
                    i++;
                    addProperty(sb, args[i]);
                } else if (args[i].indexOf('=') > 0 && args[i].indexOf('-') != 0) {
                    addProperty(sb, args[i]);
                } else {
                    throw USAGE;
                }
                i++;
            }
        } catch (IndexOutOfBoundsException e) {
            throw USAGE;
        }
        return loadObject(sb.toString(), klass, skipMissingProperties);
    }

    public static <T> T loadObject(String[] args, Class<?> klass) throws IOException, ReflectiveOperationException {
        return loadObject(args, klass, false);
    }

    public static boolean isFileOrNonEmptyDir(File fileDir) {
        if (fileDir.exists()) {
            if (fileDir.isDirectory()) {
                String[] files = fileDir.list();
                return files != null && files.length > 0;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public static <T extends BenchmarkConfig> void createDirs(T config) {
        File histogramsDir = new File(config.histogramsDir);
        File reportDir = new File(config.reportDir);
        if (isFileOrNonEmptyDir(histogramsDir)) {
            throw new IllegalArgumentException(String.format("Non-empty histograms dir '%s' already exists", histogramsDir));
        }
        if (config.makeReport && isFileOrNonEmptyDir(reportDir)) {
            throw new IllegalArgumentException(String.format("Non-empty report dir '%s' already exists", reportDir));
        }
        if (!histogramsDir.exists() && !histogramsDir.mkdirs()) {
            throw new IllegalArgumentException(String.format("Failed to create histograms dir '%s'", histogramsDir));
        }
        if (config.makeReport && !reportDir.exists() && !reportDir.mkdirs()) {
            throw new IllegalArgumentException(String.format("Failed to create report dir '%s'", reportDir));
        }
    }

    public static <T extends BenchmarkConfig> T loadConfig(String[] args, boolean runMode, Class<? extends BenchmarkConfig> configClass, boolean skipMissingProperties) throws IOException, ReflectiveOperationException {
        T config = loadObject(args, configClass, skipMissingProperties);
        config.validate();
        if (runMode) {
            createDirs(config);
        }
        return config;
    }

    public static <T extends BenchmarkConfig> T load(String[] args, boolean runMode, Class<? extends BenchmarkConfig> configClass) throws IOException, ReflectiveOperationException {
        return loadConfig(args, runMode, configClass, false);
    }
}
