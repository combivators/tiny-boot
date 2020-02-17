package net.tiny.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.Properties;
import java.util.logging.LogManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JsonLoaderTest {

    static final String LS = System.getProperty("line.separator");

    @BeforeAll
    public static void beforeAll() throws Exception {
        LogManager.getLogManager().readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
    }

    @Test
    public void testLoadJson() throws Exception {
        String json =
        "# Json Comment " + LS
        + "setting : {" + LS
        + "  local : en-US" + LS
        + "}" + LS
        + "," + LS
        + "app : {" + LS
        + "  sample : {" + LS
        + "    local : ${setting.local}," + LS
        + "    lang : ${app.sample.local}," + LS
        + "    langs : [${app.sample.local}, \"ja-JP\",\"zh-CN\"]," + LS
        + "    url : \"http://www.abc.com/\"," + LS
        + "    mem : \"abc = 123\"," + LS
        + "    cost : \"1080\"," + LS
        + "    values : \"1, 2, 3\"," + LS
        + "    array : \"a\", \"b\", \"c\"," + LS
        + "    date : \"2016/09/16\"," + LS
        + "    time : \"09:15\"," + LS
        + "    datetime : \"2016/09/16 09:15\"," + LS
        + "    nested : {" + LS
        + "       name : \"child\"," + LS
        + "       threshold : \"1.4\"" + LS
        + "    }" + LS
        + "  }" + LS
        + "}" + LS
        + "// Json Comment " + LS
        + LS;

        Properties properties = JsonLoader.load(new StringReader(json), new PropertiesSupport.Monitor());
        properties.list(System.out);
        assertEquals(14, properties.size());

        assertEquals("${setting.local}", properties.getProperty("app.sample.local"));
        assertEquals("${app.sample.local}", properties.getProperty("app.sample.lang"));
        assertEquals("[${app.sample.local},ja-JP,zh-CN]", properties.getProperty("app.sample.langs"));
        assertEquals("abc = 123", properties.getProperty("app.sample.mem"));
        assertEquals("1080", properties.getProperty("app.sample.cost"));
        assertEquals("1, 2, 3", properties.getProperty("app.sample.values"));
        assertEquals("a,b,c", properties.getProperty("app.sample.array"));
        assertEquals("09:15", properties.getProperty("app.sample.time"));
        assertEquals("2016/09/16", properties.getProperty("app.sample.date"));
        assertEquals("2016/09/16 09:15", properties.getProperty("app.sample.datetime"));
        assertEquals("child", properties.getProperty("app.sample.nested.name"));
        assertEquals("1.4", properties.getProperty("app.sample.nested.threshold"));
    }


}
