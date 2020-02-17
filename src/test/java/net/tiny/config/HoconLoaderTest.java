package net.tiny.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.logging.LogManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class HoconLoaderTest {

    static final String LS = System.getProperty("line.separator");

    @BeforeAll
    public static void beforeAll() throws Exception {
        LogManager.getLogManager().readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
    }

    @Test
    public void testLoadHocon() throws Exception {
        String hocon =
        "# HOCON Comment " + LS
        + "setting {" + LS
        + "  local = en-US" + LS
        + "}" + LS
        + LS
        + "app {" + LS
        + "  sample {" + LS
        + "    local = ${setting.local}" + LS
        + "    url = \"http://www.abc.com/\"" + LS
        + "    mem = \"abc = 123\"" + LS
        + "    cost = 1080" + LS
        + "    values = 1, 2, 3" + LS
        + "    array = \"a\", \"b\", \"c\"" + LS
        + "    date = 2016/09/16" + LS
        + "    time = 09:15" + LS
        + "    datetime = 2016/09/16 09:15" + LS
        + "    nested {" + LS
        + "       name = child" + LS
        + "       threshold = 1.4" + LS
        + "    }" + LS
        + "  }" + LS
        + "}" + LS
        + "// HOCON Comment " + LS
        + LS;
        Properties properties = HoconLoader.load(new StringReader(hocon), new PropertiesSupport.Monitor());
        properties.list(System.out);
        assertEquals(12, properties.size());
        assertEquals("en-US", properties.getProperty("setting.local"));
        assertEquals("${setting.local}", properties.getProperty("app.sample.local"));
        assertEquals("http://www.abc.com/", properties.getProperty("app.sample.url"));
        assertEquals("abc = 123", properties.getProperty("app.sample.mem"));
        assertEquals("1080", properties.getProperty("app.sample.cost"));
        assertEquals("1, 2, 3", properties.getProperty("app.sample.values"));
        assertEquals("a, b, c", properties.getProperty("app.sample.array"));
        assertEquals("09:15", properties.getProperty("app.sample.time"));
        assertEquals("2016/09/16", properties.getProperty("app.sample.date"));
        assertEquals("2016/09/16 09:15", properties.getProperty("app.sample.datetime"));
        assertEquals("child", properties.getProperty("app.sample.nested.name"));
        assertEquals("1.4", properties.getProperty("app.sample.nested.threshold"));
    }


    @Test
    public void testHoconIncludes() throws Exception {
        String hocon = new String(Files.readAllBytes(Paths.get("src/test/resources/config/includes.conf")));
        Properties properties = HoconLoader.load(new StringReader(hocon), new PropertiesSupport.Monitor());
        properties.list(System.out);

        assertEquals("value1", properties.getProperty("foo.bar.baz"));

//        assertEquals("payment", messages.getString("pay"));
//
//        Configuration conf = config.getConfiguration("web.ui.breadCrumbs");
//        assertNotNull(conf);
//        List<String> breadCrumbs = conf.getValueList("itemList1", String.class);
//        assertEquals(4, breadCrumbs.size());
//        assertEquals("item1", breadCrumbs.get(0));

    }
}
