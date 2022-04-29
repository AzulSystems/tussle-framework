import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.tussleframework.tools.FormatTool;

public class FormatToolTest {

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
}
