import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.junit.Test;
import org.tussleframework.TussleException;
import org.tussleframework.TussleTimeoutException;
import org.tussleframework.tools.LoggerTool;
import org.tussleframework.tools.ProcTool;

public class ProcToolTest {

    {
        LoggerTool.init("", "java.util.logging.ConsoleHandler");
    }

    private static void log(String format, Object... args) {
        LoggerTool.log(ProcToolTest.class.getSimpleName(), format, args);
    }

    @Test
    public void testSleep3sec() {
        try {
            long start = System.currentTimeMillis();
            String[] cmd = { "sleep", "3" };
            ProcTool.runProcess("sleep_3", null, Arrays.asList(cmd), null, 4, null);
            long finish = System.currentTimeMillis();
            log("time: %d ms", finish - start);
            assertTrue(finish - start > 3000 && finish - start < 4000);
        } catch (TussleException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testSleepTimeout() {
        long start = System.currentTimeMillis();
        try {
            String[] cmd = { "sleep", "30" };
            ProcTool.runProcess("sleep_30", null, Arrays.asList(cmd), null, 3, null);
            long finish = System.currentTimeMillis();
            log("time: %d ms", finish - start);
            fail();
        } catch (TussleTimeoutException e) {
            long finish = System.currentTimeMillis();
            log("time: %d ms (expected timeout)", finish - start);
            assertTrue(finish - start > 3000 && finish - start < 4000);
        } catch (TussleException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testUName() {
        try {
            String[] cmd = { "uname", "-a" };
            ProcTool.runProcess("uname", null, Arrays.asList(cmd), null, 3, new LoggerTool.LogOutputStream());
        } catch (TussleException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testBash() {
        try {
            String[] cmd = { "bash", "-c", "for (( i = 1; i <= 3; i++ )); do echo TEST$i; sleep 1; done" };
            ProcTool.runProcess("bash_for_loop", null, Arrays.asList(cmd), null, 5, new LoggerTool.LogOutputStream());
        } catch (TussleException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testEnv() {
        try {
            StringBuilder sb = new StringBuilder();
            String[] cmd = { "bash", "-c", "env" };
            String envVar = "SOME=_______________1_2_3_some_Z__";
            long start = System.currentTimeMillis();
            ProcTool.runProcess("env", null, Arrays.asList(cmd), Arrays.asList(envVar), 2, new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    sb.append((char)b);
                }
            });
            long finish = System.currentTimeMillis();
            log("time: %d ms", finish - start);
            log("ENV:\n%s", sb);
            //assertTrue(sb.indexOf(envVar) >= 0);
        } catch (TussleException e) {
            e.printStackTrace();
            fail();
        }
    }
}

