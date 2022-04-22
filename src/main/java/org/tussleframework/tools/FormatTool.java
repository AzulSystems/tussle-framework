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
import java.util.Collection;
import java.util.Locale;
import java.util.TimeZone;

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
     * Value examples: 100 as is, 10k -> 1000, 1M -> 1000000
     * 
     * @param s Input string value
     * @return Parsed double value
     */
    public static double parseValue(String s) {
        s = s.toLowerCase();
        double m = 1.0;
        int end = 0;
        if (s.endsWith("kib")) {
            m = 1024.0;
            end = 3;
        } else if (s.endsWith("mib")) {
            m = 1024.0 * 1024.0;
            end = 3;
        } else if (s.endsWith("gib")) {
            m = 1024.0 * 1024.0 * 1024.0;
            end = 3;
        } else if (s.endsWith("k")) {
            m = 1000.0;
            end = 1;
        } else if (s.endsWith("m")) {
            m = 1000_000.0;
            end = 1;
        } else if (s.endsWith("g")) {
            m = 1000_000_000.0;
            end = 1;
        }
        s = s.substring(0, s.length() - end).trim();
        return Double.parseDouble(s) * m;
    }

    public static int parseInt(String s) {
        s = s.toLowerCase();
        int m = 1;
        int end = 0;
        if (s.endsWith("kib")) {
            m = 102;
            end = 3;
        } else if (s.endsWith("mib")) {
            m = 1024 * 1024;
            end = 3;
        } else if (s.endsWith("gib")) {
            m = 1024 * 1024 * 1024;
            end = 3;
        } else if (s.endsWith("k")) {
            m = 1000;
            end = 1;
        } else if (s.endsWith("m")) {
            m = 1000_000;
            end = 1;
        } else if (s.endsWith("g")) {
            m = 1000_000_000;
            end = 1;
        }
        s = s.substring(0, s.length() - end).trim();
        return Integer.parseInt(s) * m;
    }

    /**
     * Value examples: 60 -> 60 seconds, 10m -> 600 seconds, 1h -> 3600 seconds, etc.
     * 
     * @param s Input string value
     * @return Time in seconds
     */
    public static int parseTimeLength(String s) {
        s = s.toLowerCase();
        int m = 1;
        int end = 0;
        if (s.endsWith("seconds")) {
            end = 7;
        } else if (s.endsWith("minutes")) {
            m = 60;
            end = 7;
        } else if (s.endsWith("min")) {
            m = 60;
            end = 3;
        } else if (s.endsWith("hrs")) {
            m = 3600;
            end = 3;
        } else if (s.endsWith("m")) {
            m = 60;
            end = 1;
        } else if (s.endsWith("h")) {
            m = 3600;
            end = 1;
        } else if (s.endsWith("s")) {
            end = 1;
        }
        s = s.substring(0, s.length() - end).trim();
        return Integer.parseInt(s) * m;
    }

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

    public static String join(Collection<?> c, String sep) {
        StringBuilder sb = new StringBuilder();
        c.forEach(o -> {
            sb.append(o);
            if (sep != null && sb.length() > 0) {
                sb.append(sep);
            }
        });
        return sb.toString();
    }

    public static String withS(long count, String name) {
        if (count == 1 || count == -1) {
            return count + " " + name;
        } else {
            return count + " " + name + "s";
        }
    }

    public static String getParam(String p, int idx, String defval) {
        String[] params = p.split("_");
        return idx < params.length ? params[idx] : defval;
    }
}
