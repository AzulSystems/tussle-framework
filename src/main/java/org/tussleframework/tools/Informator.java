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
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Properties;

public class Informator implements Tool {

    public static void main(String[] args) {
        printAll(System.out);
    }

    static final String[] JVM_PROP = {
            "java.vm.name",
            "java.vm.vendor",
            "java.vm.version",
            "java.vendor.url",
            "java.specification.version",
            "java.vm.specification.version",
            "java.version.date",
            "java.runtime.name",
            "sun.management.compiler",
            "jdk.debug",
    };

    public static void print(Properties p, PrintStream out) {
        for (Object key : p.keySet()) {
            out.println(key + ": " + p.getProperty(key.toString(), "None"));
        }
    }

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

    public static String capitalize(String s) {
        if ("os".equals(s)) {
            return "OS";
        }
        if ("jvm".equals(s)) {
            return "JVM";
        }
        if ("jdk".equals(s)) {
            return "JDK";
        }
        return s != null && s.length() > 0 ? s.substring(0, 1).toUpperCase() + s.substring(1) : s;
    }

    public static String makeKey(String key) {
        if (key.startsWith("os.")) {
            key = key.substring("os.".length());
        } else if (key.startsWith("java.")) {
            key = key.substring("java.".length());
            if (key.startsWith("vm.")) {
                key = key.substring("vm.".length());
            }
        } else if (key.startsWith("sun.management.")) {
            key = key.substring("sun.management.".length());
        }
        String[] ss = key.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ss.length; i++) {
            String s = ss[i];
            s = capitalize(s);
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public static String getUser() {
        return System.getProperty("user.name");
    }

    public static Properties getJvmInfo() {
        Properties p = new Properties();
        for (String key : JVM_PROP) {
            p.put(makeKey(key), System.getProperty(key, ""));
        }
        try {
            List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
            p.put(makeKey("jvm.args"), FormatTool.join(" ", jvmArgs));
            long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
            p.put(makeKey("jvm.start.time"), FormatTool.formatIsoDatetime(startTime));
        } catch (Exception e) {
            /// ignore
        }
        return p;
    }

    public static String getJvmName() {
        return System.getProperty("java.vm.name");
    }

    static final String[] OS_PROP = {
            "os.name",
            "os.arch",
            "os.version",
            "user.name",
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
        p.put(makeKey("available.processors"), "" + Runtime.getRuntime().availableProcessors());
        p.put(makeKey("free.memory"), FormatTool.formatBytes(Runtime.getRuntime().freeMemory()));
        p.put(makeKey("max.memory"), FormatTool.formatBytes(Runtime.getRuntime().maxMemory()));
        p.put(makeKey("total.memory"), FormatTool.formatBytes(Runtime.getRuntime().totalMemory()));
        return p;
    }
}
