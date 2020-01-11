package net.tiny.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class ConfigurationHandlerTest {

    static final String LS = System.getProperty("line.separator");

    @Test
    public void testPrintHocon() throws Exception {
        String config =
        "# HOCON Comment " + LS
        + "setting {" + LS
        + "  local = en-US" + LS
        + "}" + LS
        + LS
        + "APP {" + LS
        + "  sample {" + LS
        + "    local = ${setting.local}" + LS
        + "    url = \"http://www.abc.com/\"" + LS
        + "    cost = 1080" + LS
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
        ConfigurationHandler.print(config, System.out);
    }


    @Test
    public void testPrintJson() throws Exception {
        String config =
        "# JSON Comment " + LS
        + "{" + LS
        + " APP {" + LS
        + "  sample {" + LS
        + "    local = ${setting.local}" + LS
        + "    url = \"http://www.abc.com/\"" + LS
        + "    cost = 1080" + LS
        + "    date = 2016/09/16" + LS
        + "    time = 09:15" + LS
        + "    datetime = 2016/09/16 09:15" + LS
        + "    nested {" + LS
        + "       name = child" + LS
        + "       threshold = 1.4" + LS
        + "    }" + LS
        + "  }" + LS
        + " }" + LS
        + "}" + LS
        + "// JSON Comment " + LS
        + LS;
        ConfigurationHandler.print(config, System.out);
    }

    @Test
    public void testParseHocon() throws Exception {
        String conf =
        "# HOCON Comment " + LS
        + "setting {" + LS
        + "  local = en-US" + LS
        + "}" + LS
        + LS
        + "app {" + LS
        + "  sample {" + LS
        + "    local = ${setting.local}" + LS
        + "    url = \"http://www.abc.com/\"" + LS
        + "    cost = 1080" + LS
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
        ByteArrayInputStream bais = new ByteArrayInputStream(conf.getBytes());
        ConfigurationHandler handler = new ConfigurationHandler();
        Properties prop = handler.load(bais, ContextHandler.Type.HOCON);
        prop.list(System.out);

    }

    @Test
    public void testParseJson() throws Exception {
        String conf =
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
        + "    cost : \"1080\"," + LS
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
        ByteArrayInputStream bais = new ByteArrayInputStream(conf.getBytes());
        ConfigurationHandler handler = new ConfigurationHandler();
        //Properties prop = handler.load(bais, ContextHandler.Type.JSON);
        //prop.list(System.out);

        handler.parse(bais, ContextHandler.Type.JSON);
        Configuration config = handler.getConfiguration();
        assertNotNull(config);
        System.out.println(config.toString());
        assertEquals("en-US", config.getString("app.sample.local"));
        assertEquals("en-US", config.getString("app.sample.lang"));
        assertEquals("[en-US,ja-JP,zh-CN]", config.getString("app.sample.langs"));
        List<String> langs = config.getValueList("app.sample.langs", String.class);
        assertEquals("en-US", langs.get(0));
    }

    @Test
    public void testParseYaml() throws Exception {
        String conf =
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
        ByteArrayInputStream bais = new ByteArrayInputStream(conf.getBytes());
        ConfigurationHandler handler = new ConfigurationHandler();
        //Properties prop = handler.load(bais, ContextHandler.Type.YAML);
        //prop.list(System.out);
        handler.parse(bais, ContextHandler.Type.YAML);
        Configuration config = handler.getConfiguration();
        assertNotNull(config);
        System.out.println(config.toString());
        assertEquals("en-US", config.getString("app.sample.local"));
    }

    @Test
    public void testParseVariables() throws Exception {
        String conf =
        "# Comment " + LS
        + "vcap:" + LS
        + "  services:" + LS
        + "    ups-admin:" + LS
        + "      credentials:" + LS
        + "        admin.api.server.auth.enable: true" + LS
        + "paas:" + LS
        + "  vcap:" + LS
        + "    alias: vcap.services.ups-admin.credentials" + LS
        + LS
        + "admin:" + LS
        + "  auth:" + LS
        + "    api:" + LS
        + "      enable: ${${paas.vcap.alias}.admin.api.server.auth.enable}" + LS
        + LS;
        ByteArrayInputStream bais = new ByteArrayInputStream(conf.getBytes());
        ConfigurationHandler handler = new ConfigurationHandler();
        Properties prop = handler.load(bais, ContextHandler.Type.YAML);
        assertEquals("${${paas.vcap.alias}.admin.api.server.auth.enable}", prop.getProperty("admin.auth.api.enable"));
        assertEquals("true", prop.getProperty("vcap.services.ups-admin.credentials.admin.api.server.auth.enable"));

        bais = new ByteArrayInputStream(conf.getBytes());
        handler = new ConfigurationHandler();
        handler.parse(bais, ContextHandler.Type.YAML);
        Configuration config = handler.getConfiguration();
        assertNotNull(config);
        System.out.println(config.toString());
        assertEquals("true", config.getString("admin.auth.api.enable"));
    }

    @Test
    public void testGenerateKey() throws Exception {
        final StringBuffer key = new StringBuffer("url");
        Deque<String> parents = new ArrayDeque<String>();
        parents.push("APP");
        parents.push("sample");

        parents.stream().forEach(p -> key.insert(0, ".").insert(0, p));
        assertEquals("APP.sample.url", key.toString());

        parents.push("nested");
        final StringBuffer name = new StringBuffer("name");
        parents.stream().forEach(p -> name.insert(0, ".").insert(0, p));
        assertEquals("APP.sample.nested.name", name.toString());

        parents = new ArrayDeque<String>();
        parents.push("web.ui");
        parents.push("sample");
        final StringBuffer pre = new StringBuffer("url");
        parents.stream().forEach(p -> pre.insert(0, ".").insert(0, p));
        assertEquals("web.ui.sample.url", pre.toString());
    }

    @Test
    public void testHoconIncludes() throws Exception {
        ConfigurationHandler handler = new ConfigurationHandler();
        handler.setResource("src/test/resources/config/includes.conf");
        handler.parse();
        Configuration config = handler.getConfiguration();
        assertNotNull(config);
        System.out.println(config.toString());
        assertEquals("value1", config.getString("foo.bar.baz"));
        Configuration messages = config.getConfiguration("foo.ui.messages");
        assertEquals("payment", messages.getString("pay"));

        Configuration conf = config.getConfiguration("web.ui.breadCrumbs");
        assertNotNull(conf);
        List<String> breadCrumbs = conf.getValueList("itemList1", String.class);
        assertEquals(4, breadCrumbs.size());
        assertEquals("item1", breadCrumbs.get(0));

    }

    @Test
    public void testHoconGetValue() throws Exception {
        ConfigurationHandler handler = new ConfigurationHandler();
        handler.setResource("src/test/resources/config/reference.conf");
        handler.parse();
        Configuration config = handler.getConfiguration();
        assertNotNull(config);

        assertEquals("http://www.abc.com/", config.getString("APP.sample.url"));
        assertEquals("1080", config.getString("APP.sample.cost"));
        assertEquals(1080, (int)config.getInteger("APP.sample.cost"));
        assertEquals(1080L, (long)config.getLong("APP.sample.cost"));
        Set<String> names = config.getAllPropertyNames();
        assertEquals(8, names.size());
        names = config.getPropertyNames(n -> n.contains("nested"));
        assertEquals(2, names.size());

        Configuration sub = config.getConfiguration("APP.sample.nested");
        assertNotNull(sub);
        assertEquals("child", sub.getString("name"));
        assertEquals(new Double(1.4d), sub.getDouble("threshold"));
        assertEquals(9, config.size());
        // Test cache
        Configuration other = config.getConfiguration("APP.sample.nested");
        assertEquals(sub, other);
        assertEquals(9, config.size());
    }

    @Test
    public void testHoconGetValueFromClasspath() throws Exception {
        ConfigurationHandler handler = new ConfigurationHandler();
        handler.setResource("config/reference.conf");
        handler.parse();
        Configuration config = handler.getConfiguration();
        assertNotNull(config);
        assertEquals("http://www.abc.com/", config.getString("APP.sample.url"));
    }

    @Test
    public void testJsonGetValue() throws Exception {
        ConfigurationHandler handler = new ConfigurationHandler();
        handler.setResource("src/test/resources/config/reference.json");
        handler.parse();
        Configuration config = handler.getConfiguration();
        assertNotNull(config);
        System.out.println(config.toString());

        assertEquals("http://www.abc.com/", config.getString("APP.sample.url"));
        assertEquals("1080", config.getString("APP.sample.cost"));
        assertEquals(1080, (int)config.getInteger("APP.sample.cost"));
        assertEquals(1080L, (long)config.getLong("APP.sample.cost"));
        Set<String> names = config.getAllPropertyNames();
        assertEquals(8, names.size());
        names = config.getPropertyNames(n -> n.contains("nested"));
        assertEquals(2, names.size());

        Configuration sub = config.getConfiguration("APP.sample.nested");
        assertNotNull(sub);
        assertEquals("child", sub.getString("name"));
        assertEquals(new Double(1.4d), sub.getDouble("threshold"));
        assertEquals(9, config.size());
        // Test cache
        Configuration other = config.getConfiguration("APP.sample.nested");
        assertEquals(sub, other);
        assertEquals(9, config.size());
    }

    @Test
    public void testYamlGetValue() throws Exception {
        ConfigurationHandler handler = new ConfigurationHandler();
        handler.setResource("src/test/resources/config/app-dev.yml");
        handler.parse();
        Configuration config = handler.getConfiguration();
        assertNotNull(config);
        System.out.println(config.toString());
        assertEquals("true", config.getString("admin.server-auth.admin-api.enable"));
    }

    @Test
    public void testLoggingPropertiesFromYaml() throws Exception {
        String yml =
        "#" + LS
        + "logging:" + LS
        + "  level:" + LS
        + "    net.tiny: FINE" + LS
        + "    com.sun.net: CONFIG" + LS
        + "    org.apache: OFF" + LS
        + LS;

        ByteArrayInputStream bais = new ByteArrayInputStream(yml.getBytes());
        ConfigurationHandler handler = new ConfigurationHandler();
        handler.parse(bais, ContextHandler.Type.YAML, true);
        Configuration config = handler.getConfiguration();
        assertNotNull(config);

        //assertEquals(3, config.size());
        Configuration loggerConfig = config.getConfiguration("logging.level");
        assertEquals(3, loggerConfig.size());

        Set<String> targets = loggerConfig.getAllPropertyNames();
        assertEquals(3, targets.size());
        assertTrue(targets.containsAll(Arrays.asList("net.tiny", "com.sun.net", "org.apache")));
    }


    @Test
    public void testPropertiesGetValue() throws Exception {
        ConfigurationHandler handler = new ConfigurationHandler();
        handler.setResource("src/test/resources/config/reference.properties");
        handler.parse();
        Configuration config = handler.getConfiguration();
        assertNotNull(config);
        System.out.println(config.toString());

        assertEquals("http://www.abc.com/", config.getString("APP.sample.url"));
        assertEquals("1080", config.getString("APP.sample.cost"));
        assertEquals(1080, (int)config.getInteger("APP.sample.cost"));
        assertEquals(1080L, (long)config.getLong("APP.sample.cost"));
        Set<String> names = config.getAllPropertyNames();
        assertEquals(8, names.size());
        names = config.getPropertyNames(n -> n.contains("nested"));
        assertEquals(2, names.size());

        Configuration sub = config.getConfiguration("APP.sample.nested");
        assertNotNull(sub);
        assertEquals("child", sub.getString("name"));
        assertEquals(new Double(1.4d), sub.getDouble("threshold"));
        assertEquals(9, config.size());
        // Test cache
        Configuration other = config.getConfiguration("APP.sample.nested");
        assertEquals(sub, other);
        assertEquals(9, config.size());
    }

    @Test
    public void testResource() throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        loader = getClass().getClassLoader();
        Enumeration<URL> urls = loader.getResources("config/app.properties");
        while(urls.hasMoreElements()) {
            System.out.println(urls.nextElement());
        }
        ConfigurationHandler handler = new ConfigurationHandler();
        handler.setResource("config/app.properties");
        handler.parse();
        Configuration configuration = handler.getConfiguration();
        assertNotNull(configuration);
        configuration.writeTo(System.out);
    }

}
