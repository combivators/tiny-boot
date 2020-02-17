package net.tiny.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class YamlLoaderTest {

    static final String LS = System.getProperty("line.separator");

    @BeforeAll
    public static void beforeAll() throws Exception {
        LogManager.getLogManager().readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
    }

    @Test
    public void testVariablePattern() throws Exception {
        assertTrue(Pattern.matches(YamlLoader.VAR_REGEX, "${a}"));
        assertTrue(Pattern.matches(YamlLoader.VAR_REGEX, "${a.b.c}"));
        assertTrue(Pattern.matches(YamlLoader.VAR_REGEX, "${a1.b2.c3}"));
        assertTrue(Pattern.matches(YamlLoader.VAR_REGEX, "${a}${b.c}"));
        assertTrue(Pattern.matches(YamlLoader.VAR_REGEX, "${a.${b.c}}"));

        assertFalse(Pattern.matches(YamlLoader.VAR_REGEX, "${b. c}"));
        assertFalse(Pattern.matches(YamlLoader.VAR_REGEX, "${b+c}"));
        assertFalse(Pattern.matches(YamlLoader.VAR_REGEX, "{b.c}"));
    }

    @Test
    public void testParseYaml() throws Exception {
        final String yaml =
        "# Comment " + LS
        + "setting:" + LS
        + "  local : en-US" + LS
        + LS
        + "app:" + LS
        + "  sample: " + LS
        + "    local : ${setting.local}" + LS
        + "    url : \"http://www.abc.com/\"" + LS
        + "    cost: 1080  # Date" + LS
        + "    time : 09:15" + LS
        + "    datetime : 2016/09/16 09:15" + LS
        + "    nested:" + LS
        + "      name : child" + LS
        + "      threshold : 1.4" + LS
        + "    date : 2016/09/16" + LS
        + LS;


        Properties properties = YamlLoader.load(new StringReader(yaml), new PropertiesSupport.Monitor());
        properties.list(System.out);
        assertEquals(9, properties.size());
        assertEquals("\"http://www.abc.com/\"", properties.getProperty("app.sample.url"));
        assertEquals("1080", properties.getProperty("app.sample.cost"));
        assertEquals("en-US", properties.getProperty("setting.local"));
        assertEquals("${setting.local}", properties.getProperty("app.sample.local"));
    }


    @Test
    public void testList() throws Exception {
        final String yaml =
          "setting:" + LS
        + "  local: " + LS
        + "    - en-US" + LS
        + "    - ja-JP" + LS
        + "  time: 09:15" + LS
        + LS
        + "app:" + LS
        + "  sample: " + LS
        + "    cost:" + LS
        + "      - 1080  # Date" + LS
        + "      - 1090" + LS
        + "main:" + LS
        + "  - ${online}" + LS
        + "  - ${batch}" + LS
        + LS;

        Properties properties = YamlLoader.load(new StringReader(yaml), new PropertiesSupport.Monitor());
        properties.list(System.out);
        assertEquals(4, properties.size());

        assertEquals("\"1080\",\"1090\"", properties.getProperty("app.sample.cost"));
        assertEquals("09:15", properties.getProperty("setting.time"));
        assertEquals("\"en-US\",\"ja-JP\"", properties.getProperty("setting.local"));
    }

    @Test
    public void testHyphenList() throws Exception {
        final String yaml =
          "main:" + LS
        + "  - en-US" + LS
        + "  - ja-JP" + LS
        + "local: " + LS
        + "  time: 09:15" + LS
        + LS;

        Properties properties = YamlLoader.load(new StringReader(yaml), new PropertiesSupport.Monitor());
        assertEquals(2, properties.size());

        assertEquals("09:15", properties.getProperty("local.time"));
        assertEquals("\"en-US\",\"ja-JP\"", properties.getProperty("main"));
    }

    @Test
    public void testLastHyphenList() throws Exception {
        final String yaml =
          "main:" + LS
        + "  - en-US" + LS
        + "  - ja-JP" + LS
        + LS;

        Properties properties = YamlLoader.load(new StringReader(yaml), new PropertiesSupport.Monitor());
        assertEquals(1, properties.size());

        assertEquals("\"en-US\",\"ja-JP\"", properties.getProperty("main"));
    }



    @Test
    public void testOneElement() throws Exception {
        final String yaml =
          "main:" + LS
        + "  - en-US" + LS
        + "hoge:" + LS
        + "  n:" + LS
        + "    a: 1" + LS
        + "local: " + LS
        + "  time:" + LS
        + "    - 09:15" + LS
        + "fuga:" + LS
        + "  n:" + LS
        + "    a: 2" + LS
        + LS;

        Properties properties = YamlLoader.load(new StringReader(yaml), new PropertiesSupport.Monitor());
        assertEquals(4, properties.size());

        assertEquals("\"en-US\"", properties.getProperty("main"));
        assertEquals("\"09:15\"", properties.getProperty("local.time"));
        assertEquals("1", properties.getProperty("hoge.n.a"));
        assertEquals("2", properties.getProperty("fuga.n.a"));
    }

    @Test
    public void testHyphenListReference() throws Exception {
        String yaml =
                "app:" + LS
              + "  m:" + LS
              + "    a: 1" + LS
              + "    b: 2" + LS
              + "    c: 3" + LS
              + "  d: " + LS
              + "    - ${app.m.a}" + LS
              + "    - ${app.m.b}" + LS
              + "    - ${app.m.c}" + LS
              + "  e:" + LS
              + "    f: 4" + LS
              + LS;

        Properties properties = YamlLoader.load(new StringReader(yaml), new PropertiesSupport.Monitor());
        assertEquals(5, properties.size());
        assertEquals("${app.m.a},${app.m.b},${app.m.c}", properties.getProperty("app.d"));
    }

    @Test
    public void testDeepList() throws Exception {
        final String yaml =
          "setting:" + LS
        + "  a:" + LS
        + "    b:" + LS
        + "      local: " + LS
        + "        - en-US" + LS
        + "        - ja-JP" + LS
        + "  c:" + LS
        + "    d:" + LS
        + "      local: " + LS
        + "        - en-US" + LS
        + "        - ja-JP" + LS
        + "main:" + LS
        + "  - ${setting.a.b.local}" + LS
        + "  - ${setting.c.d.local}" + LS
        + LS;

        Properties properties = YamlLoader.load(new StringReader(yaml), new PropertiesSupport.Monitor());
        properties.list(System.out);
        assertEquals(3, properties.size());

        assertEquals("${setting.a.b.local},${setting.c.d.local}", properties.getProperty("main"));
        assertEquals("\"en-US\",\"ja-JP\"", properties.getProperty("setting.a.b.local"));
        assertEquals("\"en-US\",\"ja-JP\"", properties.getProperty("setting.c.d.local"));
    }
}
