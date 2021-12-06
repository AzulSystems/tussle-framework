package org.benchmarks.tools;
import java.io.PrintStream;
import java.util.Properties;

public class Informator {

    public static void main(String[] args) {
        printAll(System.err);
    }

    static final String[] JVM_PROP = {
            "java.vm.name",
            "java.vm.vendor",
            "java.vm.version",
            "java.vendor.url",
            "java.vm.specification.version",
            "java.version.date",
    };

    public static void printAll(PrintStream out) {
        out.println("All system properties:");
        print(System.getProperties(), out);
        out.println("");
        out.println("");
        out.println("JVM properties:");
        print(getJvmInfo(), out);
        out.println("");
        out.println("OS properties:");
        print(getOsInfo(), out);
        out.println("");
        out.println("HW properties:");
        print(getHwInfo(), out);
        out.println("");
    }

    public static String makeKey(String key) {
        if (key.startsWith("os.")) {
            key = key.substring(3);
        } else if (key.startsWith("java.")) {
            key = key.substring(5);
            if (key.startsWith("vm.")) {
                key = key.substring(3);
            }
        }
        String[] ss = key.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ss.length; i++) {
            String s = ss[i];
            if (i > 0) {
                s = s.substring(0, 1).toUpperCase() + s.substring(1);
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public static Properties getJvmInfo() {
        Properties p = new Properties();
        for (String key : JVM_PROP) {
            p.put(makeKey(key), System.getProperty(key, "None"));
        }
        return p;
    }

    static final String[] OS_PROP = {
            "os.name",
            "os.version",
            "os.arch",
    };

    public static Properties getOsInfo() {
        Properties p = new Properties();
        for (String key : OS_PROP) {
            p.put(makeKey(key), System.getProperty(key, "None"));
        }
        return p;
    }

    public static Properties getHwInfo() {
        Properties p = new Properties();
        // TODO
        p.put("name", "Server");
        return p;
    }

    public static void print(Properties p, PrintStream out) {
        for (Object key : p.keySet()) {
            out.println(key + ": " + p.getProperty(key.toString(), "None"));
        }
    }
}
