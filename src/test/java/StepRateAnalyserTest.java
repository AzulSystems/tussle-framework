import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.tussleframework.steprater.StepRaterAnalyser;

public class StepRateAnalyserTest {

    @Test
    public void testAnalyser() {
        String reportDir = "reportDir" + System.currentTimeMillis();
        try {
            Files.createDirectories(Paths.get(reportDir));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        String[] args = { 
                "-p", "resultsDir = " + reportDir,
                "-p", "reportDir = " + reportDir,
                "-p", "highBound = 100000",
                "-p", "sleConfig = [[50, 1, 10], [99, 10, 10], [99.9, 50, 60], [99.99, 200, 120], [100, 1000, 120]]"};
        try {
            StepRaterAnalyser.main(args);
            assertTrue("metrics.json should be created", Files.exists(Paths.get(reportDir, "metrics.json")));
            assertTrue("index.html should be created", Files.exists(Paths.get(reportDir, "index.html")));
        } catch (Throwable e) {
            e.printStackTrace();
            fail();
        }
    }
}
