
import static org.junit.Assert.assertTrue;

import java.text.DecimalFormat;

import org.benchmarks.tools.SleepTool;
import org.junit.Test;

public class ConfigTest {

    @Test
    public void test1() {
        DecimalFormat df = new DecimalFormat("#.##");
        System.err.println(df.format(999.9000099990002));
        System.err.println(df.format(999));
        System.err.println(df.format(999.0111));
        System.err.println(df.format(999.0001));
        System.err.println(df.format(99.0001));
        System.err.println(df.format(9.0001));
        System.err.println(df.format(.01));
        System.err.println(df.format(.001));
        System.err.println(df.format(.0001));
        System.err.println(df.format(.00001));
        System.err.println(df.format(.000001));
        System.err.println(String.format("%02.0f", .01));
        System.err.println(String.format("%02.0f", 1.01));
        System.err.println(String.format("%02.0f", 10.01));
        System.err.println(String.format("%02.0f", 100.01));
        assertTrue(true);
    }

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
