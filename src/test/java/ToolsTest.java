
import static org.junit.Assert.assertTrue;

import org.benchmarks.tools.SleepTool;
import org.junit.Test;

public class ToolsTest {

    @Test
    public void testSleepPrecise1s() {
        System.err.println("S: " + System.nanoTime() + " - " + System.currentTimeMillis());
        for (int i = 1; i <= 1000000; i++) {
            long n = System.nanoTime();
            long m = System.currentTimeMillis();
            long s = System.nanoTime();
            SleepTool.sleepSpinning(1000);
            long t = System.nanoTime() - s;
            if (i % 1000 == 0)
                System.err.println(i + ": " + n + " - " + m + " - " + t);
        }
        System.err.println("E: " + System.nanoTime() + " - " + System.currentTimeMillis());
        assertTrue(true);
    }
    
    @Test
    public void testSleep1s() {
        System.err.println("S: " + System.nanoTime() + " - " + System.currentTimeMillis());
        for (int i = 1; i <= 10000; i++) {
            long n = System.nanoTime();
            long m = System.currentTimeMillis();
            long s = System.nanoTime();
            SleepTool.sleepSpinning(100000);
            long t = System.nanoTime() - s;
            if (i % 1000 == 0)
                System.err.println(i + ": " + n + " - " + m + " - " + t);
        }
        System.err.println("E: " + System.nanoTime() + " - " + System.currentTimeMillis());
        assertTrue(true);
    }

    @Test
    public void testSleep5s() {
        System.err.println("S: " + System.nanoTime() + " - " + System.currentTimeMillis());
        for (int i = 1; i <= 5; i++) {
            long n = System.nanoTime();
            long m = System.currentTimeMillis();
            long s = System.nanoTime();
            SleepTool.sleep(1000000000);
            long t = System.nanoTime() - s;
            System.err.println(i + ": " + n + " - " + m + " - " + t);
        }
        System.err.println("E: " + System.nanoTime() + " - " + System.currentTimeMillis());
        assertTrue(true);
    }
}
