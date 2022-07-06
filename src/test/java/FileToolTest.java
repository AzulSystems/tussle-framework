import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import org.junit.Test;
import org.tussleframework.tools.FileTool;
import org.tussleframework.tools.LoggerTool;

public class FileToolTest {

    {
        LoggerTool.init("", "java.util.logging.ConsoleHandler");
    }

    private static void log(String format, Object... args) {
        LoggerTool.log(ProcToolTest.class.getSimpleName(), format, args);
    }

    static private Collection<String> list(String baseDir, String fileMatch) {
        log("list [%s] - [%s] ...", baseDir, fileMatch);
        File parentDir = new File(fileMatch).getParentFile();
        Collection<File> list = FileTool.listFiles(baseDir, Arrays.asList(fileMatch));
        log("BaseDir '%s', fileMatch: '%s', fileMatch parent dir: '%s', count %d", baseDir, fileMatch, parentDir, list.size());
        list.forEach(f -> log("found file '%s'", f));
        Stream<Object> s = list.stream().map(f -> f.getPath());
        return Arrays.asList(s.toArray(String[]::new));
    }

    @Test
    public void testListFilesExact1() {
        Collection<String> list = list(null, "test_data/proc_benchmark_test/test_run_script.sh");
        assertEquals(1, list.size());
        assertTrue(list.contains("test_data/proc_benchmark_test/test_run_script.sh"));
    }

    @Test
    public void testListFilesExact2() {
        Collection<String> list = list("test_data/proc_benchmark_test", "test_run_script.sh");
        assertEquals(1, list.size());
        assertTrue(list.contains("test_data/proc_benchmark_test/test_run_script.sh"));
    }

    @Test
    public void testListFilesStar() {
        Collection<String> list = list(null, "test_data/proc_benchmark_test/.*");
        assertTrue(list.size() > 1);
        assertTrue(list.contains("test_data/proc_benchmark_test/test_run_script.sh"));
    }

    @Test
    public void testListFilesHlog1() {
        Collection<String> list = list(null, "test_data/proc_benchmark_test/.*.hlog");
        assertTrue(list.size() > 1);
        list.forEach(elem -> assertTrue(elem.endsWith(".hlog")));
    }

    @Test
    public void testListFilesHlog2() {
        Collection<String> list = list("test_data", "proc_benchmark_test/.*.hlog");
        assertTrue(list.size() > 1);
        list.forEach(elem -> assertTrue(elem.endsWith(".hlog")));
    }

    @Test
    public void testListRoot() {
        Collection<String> list = list(null, ".*");
        assertTrue(list.size() > 10);
    }
}
