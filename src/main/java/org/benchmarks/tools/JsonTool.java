package org.benchmarks.tools;

import java.io.InputStream;
import java.io.PrintStream;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

public class JsonTool {
    private JsonTool() {
    }

    /**
     * Print in json format 
     * 
     */
    public static void printJson(Object obj, PrintStream out) throws Exception {
        ObjectWriter ow = new ObjectMapper()
                .setSerializationInclusion(Include.NON_NULL)
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                .writer().withDefaultPrettyPrinter();
        ow.writeValue(out, obj);
    }

    /**
     * Read object from json file (stream)
     */
    public static <T> T readJson(InputStream in, Class<T> klass) throws Exception {
        ObjectReader objectReader = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .reader();
        return objectReader.readValue(in, klass);
    }

}
