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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.tussleframework.tools.FormatTool;
import org.tussleframework.tools.LoggerTool;

public class FormatToolTest {

    {
        LoggerTool.init("", "java.util.logging.ConsoleHandler");
    }

    @Test
    public void testJoinArray() {
        Object c[] = { "A", "B" };
        assertEquals("AB", FormatTool.join("", c));
        assertEquals("A,B", FormatTool.join(",", c));
        assertEquals("A, B", FormatTool.join(", ", c));
        assertEquals("AB", FormatTool.join(null, c));
    }

    @Test
    public void testJoinCol() {
        List<String> c = Arrays.asList("A", "B");
        assertEquals("AB", FormatTool.join("", c));
        assertEquals("A,B", FormatTool.join(",", c));
        assertEquals("A, B", FormatTool.join(", ", c));
        assertEquals("AB", FormatTool.join(null, c));
    }

    @Test
    public void testJoinCols() {
        Object c1[] = { "A", "B" };
        List<String> c2 = Arrays.asList("X", "Y");
        assertEquals("ABXY", FormatTool.join("", c1, c2));
        assertEquals("X | Y | A | B | X | Y", FormatTool.join(" | ", c2, c1, c2));
        assertEquals("", FormatTool.join(null));
    }
    
    @Test
    public void testParseInt() {
        assertEquals(1000, FormatTool.parseInt("1k"));
        assertEquals(1024, FormatTool.parseInt("1kib"));
        assertEquals(1000000, FormatTool.parseInt("1M"));
        assertEquals(1024*1024, FormatTool.parseInt("1MiB"));
    }
    
    @Test
    public void testApply() {
        Properties vars = new Properties();
        vars.put("targetRate", "12345");
        Map<String, String> pairs = FormatTool.getSysMap(vars);
        pairs.put("warmupTime", "777");
        assertEquals("TEST", FormatTool.applyArg("TEST", pairs));
        assertEquals("TEST{", FormatTool.applyArg("TEST{", pairs));
        assertEquals("}TEST", FormatTool.applyArg("}TEST", pairs));
        assertEquals("TEST{args", FormatTool.applyArg("TEST{args", pairs));
        assertEquals("TEST{args}", FormatTool.applyArg("TEST{args}", pairs));
        assertEquals("TEST{args}END", FormatTool.applyArg("TEST{args}END", pairs));
        assertEquals("{args}END", FormatTool.applyArg("{args}END", pairs));
        assertEquals("TEST{targetRate", FormatTool.applyArg("TEST{targetRate", pairs));
        assertEquals("TEST12345", FormatTool.applyArg("TEST{targetRate}", pairs));
        assertEquals("TEST12345END", FormatTool.applyArg("TEST{targetRate}END", pairs));
        assertEquals("12345END", FormatTool.applyArg("{targetRate}END", pairs));
        assertEquals("12345", FormatTool.applyArg("{targetRate}", pairs));
        assertEquals("TEST_targetRate12345_warmupTime777", FormatTool.applyArg("TEST_targetRate{targetRate}_warmupTime{warmupTime}", pairs));
        assertEquals("TEST_targetRate12345_warmupTimewarmupTime}", FormatTool.applyArg("TEST_targetRate{targetRate}_warmupTimewarmupTime}", pairs));
        assertEquals(new File("").getAbsolutePath(), FormatTool.applyArg("{user.dir}", pairs));
    }
}
