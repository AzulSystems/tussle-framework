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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class FormatTool {
    private FormatTool() {
    }

    private static final SimpleDateFormat BASIC_UTC_DATE_FORMAT;

    public static final long NS_IN_S = 1_000_000_000L;
    public static final long NS_IN_MS = 1_000_000L;
    public static final long NS_IN_US = 1_000L;
    public static final long MS_IN_S = 1_000L;

    static {
        BASIC_UTC_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        BASIC_UTC_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static long parseUTCDate(String s) throws ParseException {
        return BASIC_UTC_DATE_FORMAT.parse(s.substring("START:".length()).trim()).getTime();
    }

    public static String format(double d) {
        long l = (long) d;
        return l == d ? "" + l : "" + d;
    }

    public static String format(double n, boolean round) {
        return round ? roundFormat(n) : DF[DF.length - 1].format(n);
    }

    public static String roundFormat(double n, int digits) {
        return DF[digits].format(n);
    }

    public static String roundFormat(double n) {
        int digits;
        if (n == 0)
            return "0";
        double nAbs = n > 0 ? n : -n;
        if (nAbs >= 200)
            digits = 0;
        else if (nAbs >= 100)
            digits = 1;
        else if (nAbs >= 10)
            digits = 2;
        else if (nAbs >= 1)
            digits = 3;
        else if (nAbs >= .1)
            digits = 4;
        else if (nAbs >= .01)
            digits = 5;
        else if (nAbs >= .001)
            digits = 6;
        else if (nAbs >= .0001)
            digits = 7;
        else
            digits = 8;
        return DF[digits].format(n);
    }

    public static String roundFormatPercent(double percentOfHighBound) {
        if (percentOfHighBound < 10) {
            return "00" + roundFormat(percentOfHighBound);
        }
        if (percentOfHighBound < 100) {
            return "0" + roundFormat(percentOfHighBound);
        }
        return roundFormat(percentOfHighBound);
    }

    private static final DecimalFormat[] DF;

    static {
        DecimalFormatSymbols fs = DecimalFormatSymbols.getInstance(Locale.ENGLISH);
        DecimalFormat[] df = {
                new DecimalFormat("0", fs),
                new DecimalFormat("0", fs),
                new DecimalFormat("0", fs),
                new DecimalFormat("0", fs),
                new DecimalFormat("0", fs),
                new DecimalFormat("0", fs),
                new DecimalFormat("0", fs),
                new DecimalFormat("0", fs),
                new DecimalFormat("0", fs),
        };
        for (int i = 0; i < df.length - 1; i++) {
            df[i].setMaximumFractionDigits(i);
        }
        df[df.length - 1].setMaximumFractionDigits(20);
        DF = df;
    }

    /**
     * Value and result examples: 100 as is, 10k -> 1000, 1M -> 1000000
     * 
     * @param value Input string value
     * @return Parsed double value
     */
    public static double parseValue(String value) {
        value = value.toLowerCase();
        double m = 1.0;
        int end = 0;
        if (value.endsWith("kib")) {
            m = 1024.0;
            end = 3;
        } else if (value.endsWith("mib")) {
            m = 1024.0 * 1024.0;
            end = 3;
        } else if (value.endsWith("gib")) {
            m = 1024.0 * 1024.0 * 1024.0;
            end = 3;
        } else if (value.endsWith("k")) {
            m = 1000.0;
            end = 1;
        } else if (value.endsWith("m")) {
            m = 1000_000.0;
            end = 1;
        } else if (value.endsWith("g")) {
            m = 1000_000_000.0;
            end = 1;
        }
        value = value.substring(0, value.length() - end).trim();
        return Double.parseDouble(value) * m;
    }

    public static int parseInt(String value) {
        value = value.toLowerCase();
        int m = 1;
        int end = 0;
        if (value.endsWith("kib")) {
            m = 1024;
            end = 3;
        } else if (value.endsWith("mib")) {
            m = 1024 * 1024;
            end = 3;
        } else if (value.endsWith("gib")) {
            m = 1024 * 1024 * 1024;
            end = 3;
        } else if (value.endsWith("k")) {
            m = 1000;
            end = 1;
        } else if (value.endsWith("m")) {
            m = 1000_000;
            end = 1;
        } else if (value.endsWith("g")) {
            m = 1000_000_000;
            end = 1;
        }
        value = value.substring(0, value.length() - end).trim();
        return Integer.parseInt(value) * m;
    }

    /**
     * Value and result examples: 60 -> 60 seconds, 10m -> 600 seconds, 1h -> 3600 seconds, etc.
     * 
     * @param value Input string value
     * @return Time in seconds
     */
    public static int parseTimeLength(String value) {
        value = value.toLowerCase();
        int m = 1;
        int end = 0;
        if (value.endsWith("seconds")) {
            end = 7;
        } else if (value.endsWith("minutes")) {
            m = 60;
            end = 7;
        } else if (value.endsWith("min")) {
            m = 60;
            end = 3;
        } else if (value.endsWith("hrs")) {
            m = 3600;
            end = 3;
        } else if (value.endsWith("m")) {
            m = 60;
            end = 1;
        } else if (value.endsWith("h")) {
            m = 3600;
            end = 1;
        } else if (value.endsWith("s")) {
            end = 1;
        }
        value = value.substring(0, value.length() - end).trim();
        return Integer.parseInt(value) * m;
    }

    /**
     * 
     * @param s
     * @return Time in nanoseconds
     */
    public static long parseTimeNs(String s) {
        s = s.toLowerCase();
        long m = 1;
        int end = 0;
        if (s.endsWith("seconds")) {
            m = NS_IN_S;
            end = 7;
        } else if (s.endsWith("minutes")) {
            m = 60 * NS_IN_S;
            end = 7;
        } else if (s.endsWith("min")) {
            m = 60 * NS_IN_S;
            end = 3;
        } else if (s.endsWith("nanoseconds")) {
            end = 11;
        } else if (s.endsWith("nanos")) {
            end = 5;
        } else if (s.endsWith("milliseconds")) {
            m = NS_IN_MS;
            end = 12;
        } else if (s.endsWith("millis")) {
            m = NS_IN_MS;
            end = 6;
        } else if (s.endsWith("microseconds")) {
            m = NS_IN_US;
            end = 12;
        } else if (s.endsWith("micros")) {
            m = NS_IN_US;
            end = 6;
        } else if (s.endsWith("us")) {
            m = NS_IN_US;
            end = 2;
        } else if (s.endsWith("ns")) {
            end = 2;
        } else if (s.endsWith("ms")) {
            m = NS_IN_MS;
            end = 2;
        } else if (s.endsWith("s")) {
            m = NS_IN_S;
            end = 1;
        }
        s = s.substring(0, s.length() - end).trim();
        return Long.parseLong(s) * m;
    }

    /**
     * Joins collection into string buffer using specified separator. All array and collection objects will be joined recursively.  
     * 
     * @param sb
     * @param sep
     * @param c
     * @return
     */
    public static StringBuilder join(StringBuilder sb, String sep, Collection<?> c) {
        int i = 0;
        for (Object o : c) {
            if (o instanceof Collection) {
                join(sb, sep, (Collection<?>) o);
            } else if (o instanceof Object[]) {
                join(sb, sep, Arrays.asList((Object[]) o));
            } else {
                sb.append(o);
            }
            if (sep != null && sb.length() > 0 && i < c.size() - 1) {
                sb.append(sep);
            }
            i++;
        }
        return sb;
    }

    /**
     * Joins object array into string using specified separator
     * 
     * @param sep
     * @param c
     * @return
     */
    public static String join(String sep, Object... c) {
        return join(new StringBuilder(), sep, Arrays.asList(c)).toString();
    }

    /**
     * Joins collection into string using specified separator
     * 
     * @param sep
     * @param c
     * @return
     */
    public static String join(String sep, Collection<?> c) {
        return join(new StringBuilder(), sep, c).toString();
    }

    public static String withS(long count, String name) {
        if (count == 1 || count == -1) {
            return count + " " + name;
        } else {
            return count + " " + name + "s";
        }
    }

    /**
     * Extracts value by index from from string in format 'value0_value1_value2_...' 
     * 
     * @param p
     * @param idx
     * @param defval
     * @return
     */
    public static String getStringValue(String p, int idx, String defval) {
        String[] params = p.split("_");
        return idx < params.length ? params[idx] : defval;
    }

    public static class Param {
        String name;
        String value;
        Param(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * Splits string value in format 'key=name' into separate 'key' and 'value'
     * 
     * @param es
     * @return
     */
    public static Param splitParam(String es) {
        int pos = es.indexOf('=');
        return new Param(es.substring(0, pos), es.substring(pos + 1));
    }

    public static String paramName(String es) {
        int pos = es.indexOf('=');
        return es.substring(0, pos);
    }

    public static String paramValue(String es) {
        int pos = es.indexOf('=');
        return es.substring(pos + 1);
    }

    public static boolean matchFilters(String name, String[] include, String[] exclude) {
        boolean match = true;
        if (include != null && include.length > 0) {
            match = false;
            for (String inc : include) {
                Pattern regexp = Pattern.compile(inc);
                if (regexp.matcher(name).matches()) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                return match;
            }
        }
        if (exclude != null && exclude.length > 0) {
            for (String exc : exclude) {
                Pattern regexp = Pattern.compile(exc);
                if (regexp.matcher(name).matches()) {
                    match = false;
                    break;
                }
            }
        }
        return match;
    }

    /**
     * Replace parameters in the input string:
     *    "some str_{key1} ... {key2}" -> "some str_value1 ... value2"
     * 
     * @param input
     * @param params
     * @return
     */
    public static String applyArg(String input, Map<String, String> params) {
        String res = applyArgOnce(input, params);
        if (res != null && !res.equals(input)) {
            input = res;
            res = applyArgOnce(input, params);
            if (res != null && !res.equals(input)) {
                res = applyArgOnce(input, params);
            }
        }
        return res;
    }

    public static String applyArgOnce(String input, Map<String, String> params) {
        if (input == null || params == null || params.isEmpty()) {
            return input;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            int pos1 = input.indexOf('{', i);
            if (pos1 >= 0) {
                int pos2 = input.indexOf('}', pos1);
                if (pos2 > pos1) {
                    sb.append(input.substring(i, pos1));
                    String key = input.substring(pos1 + 1, pos2);
                    String value = key.isEmpty() ? null : params.get(key);
                    if (value != null) {
                        sb.append(value);
                    } else {
                        sb.append(input.substring(pos1, pos2 + 1));
                    }
                    i = pos2 + 1;
                } else {
                    sb.append(input.substring(i));
                    i = input.length();
                }
            } else {
                sb.append(input.substring(i));
                i = input.length();
            }
        }
        return sb.toString();
    }

    public static List<String> applyArgs(List<String> args, Map<String, String> params) {
        for (int i = 0; i < args.size(); i++) {
            args.set(i, applyArg(args.get(i), params));
        }
        return args;
    }

    public static Map<String, String> getSysMap(Properties vars) {
        return new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            @Override
            public String get(Object key) {
                String k = key.toString();
                if (super.containsKey(k)) {
                    return super.get(k);
                } else if (vars != null && vars.containsKey(k)) {
                    return vars.getProperty(k);
                } else {
                    return System.getProperty(k);
                }
            }
        };
    }
}
