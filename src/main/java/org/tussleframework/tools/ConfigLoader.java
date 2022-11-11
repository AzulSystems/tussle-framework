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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;

import org.tussleframework.AbstractConfig;
import org.tussleframework.TussleException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

public class ConfigLoader {

    private ConfigLoader() {
    }

    static final IllegalArgumentException USAGE =
            new IllegalArgumentException("Expected parameters: -f yaml-file | -s yaml-string | -p prop1=value1 -p prop2=value2 ... | prop1=value1 prop2=value2 ...");

    public static void readFile(String configFile, StringBuilder sb) throws TussleException {
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            String line = br.readLine();
            while (line != null) {
                sb.append(line).append('\n');
                line = br.readLine();
            }
        } catch (Exception e) {
            throw new TussleException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T loadObject(String s, Class<?> klass, boolean skipMissingProperties) throws TussleException {
        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(skipMissingProperties);
        Yaml yaml = new Yaml(new Constructor(klass), representer);
        T config = yaml.load(new StringReader(s));
        if (config == null) {
            try {
                config = (T) klass.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                throw new TussleException(e);
            }
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

    public static <T> T loadObject(String[] args, Class<?> klass, boolean skipMissingProperties) throws TussleException {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        try {
            while (i < args.length) {
                if (args[i].equals("--")) {
                    break;
                } else if (args[i].isEmpty()) {
                    ///
                } else if (args[i].equals("--string") || args[i].equals("-s")) {
                    i++;
                    sb.append(args[i]).append('\n');
                } else if (args[i].equals("--file") || args[i].equals("-f")) {
                    i++;
                    readFile(args[i], sb);
                } else if (args[i].equals("--property") || args[i].equals("-p")) {
                    i++;
                    addProperty(sb, args[i]);
                } else if (args[i].indexOf('=') >= 1 && args[i].indexOf('-') != 0) {
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

    public static <T> T loadObject(String[] args, Class<?> klass) throws TussleException {
        return loadObject(args, klass, false);
    }

    public static <T extends AbstractConfig> T loadConfig(String[] args, boolean runMode, Class<? extends AbstractConfig> configClass, boolean skipMissingProperties) throws TussleException {
        T config = loadObject(args, configClass, skipMissingProperties);
        config.validate(runMode);
        return config;
    }

    public static <T extends AbstractConfig> T loadConfig(String[] args, boolean runMode, Class<? extends AbstractConfig> configClass) throws TussleException {
        return loadConfig(args, runMode, configClass, false);
    }
}
