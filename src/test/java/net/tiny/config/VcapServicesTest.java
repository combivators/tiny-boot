package net.tiny.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.tiny.config.VcapServices.VcapService;

public class VcapServicesTest {

    static String vcap_sample01 = "{\"user-provided\":[{" +
            "\"name\": \"ups-tiny\"," +
            "\"instance_name\": \"ups-tiny\"," +
            "\"binding_name\": null," +
            "\"credentials\": {" +
            "\"cf_username\": \"hoge\"," +
            "\"cf_password\": \"rv2XU8KDcevKy7Wm\"" +
            "}," +
            "\"syslog_drain_url\": \"\"," +
            "\"volume_mounts\": [" +
            "]," +
            "\"label\": \"user-provided\"," +
            "\"tags\": [" +
            "]" +
            "}]}";

    @BeforeAll
    public static void beforeAll() throws Exception {
        LogManager.getLogManager()
            .readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));

    }

    @SuppressWarnings({ "unchecked"})
    @Test
    public void testEnvironmentVcapServices() throws Exception {
        assertNull(System.getenv(VcapServices.ENV_VCAP_SERVICES));
//        updateEnv(VcapServices.ENV_VCAP_SERVICES, vcap_sample01);
//        String json = System.getenv(VcapServices.ENV_VCAP_SERVICES);
        String json = vcap_sample01;
        assertNotNull(json);
        Map<String, Object> map = JsonParser.unmarshal(json, Map.class);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals("user-provided", map.keySet().iterator().next());
    }


    @Test
    public void testValueOf() throws Exception {
        VcapServices vcaps = VcapServices.valueOf(vcap_sample01);
        assertEquals(1, vcaps.size());
        VcapServices.VcapService vcap = vcaps.getVcapService("ups-tiny");
        assertNotNull(vcap);
        assertEquals("ups-tiny", vcap.getName());
        assertEquals("user-provided", vcap.getLabel());
        assertNull(vcap.getPlan());
        assertNull(vcap.getProvider());
        assertEquals("hoge", vcap.getCredentials().getProperty("cf_username"));
        assertEquals("rv2XU8KDcevKy7Wm", vcap.getCredentials().getProperty("cf_password"));
        assertEquals("hoge", vcap.getCredential("cf_username"));
        assertEquals("rv2XU8KDcevKy7Wm", vcap.getCredential("cf_password"));
        assertEquals(0, vcap.getTags().length);

        List<VcapService> list = vcaps.geVcapServicesByType("user-provided");
        assertEquals(1, list.size());
    }

    @Test
    public void testVcapServices() throws Exception {
        final String json = loadJson("src/test/resources/cloudfoundry/vcap_example.json");
        VcapServices vcaps = VcapServices.valueOf(json);
        assertEquals(2, vcaps.size());
        List<VcapService> list = vcaps.geVcapServicesByType("mongodb-1.8");
        assertEquals(1, list.size());
        VcapServices.VcapService vcap = list.get(0);
        assertEquals("mongodb-5f772", vcap.getName());
        assertEquals("mongodb-1.8", vcap.getLabel());
        assertEquals("free", vcap.getPlan());
        assertEquals(3, vcap.getTags().length);
        assertEquals("mongodb", vcap.getTags()[0]);
        assertEquals("mongodb-1.6", vcap.getTags()[1]);
        assertEquals("nosql", vcap.getTags()[2]);
        assertEquals("127.0.0.1", vcap.getCredential("host"));
        assertEquals("25002.0", vcap.getCredential("port"));


        list = vcaps.geVcapServicesByType("postgresql-8.4");
        assertEquals(1, list.size());
        vcap = list.get(0);
    }

    @Test
    public void testResolvePropertyValue() throws Exception {
        System.getProperties().setProperty("custom", "xyz");
        System.getProperties().setProperty("abc.xyz", "Hello");
        assertEquals("xyz", VcapServices.resolvePropertyValue("${custom}"));
        assertEquals("ABC", VcapServices.resolvePropertyValue("${unknow, ABC}"));
        assertEquals("abc.xyz", VcapServices.resolvePropertyValue("abc.${custom}"));
        System.getProperties().remove("custom");
        System.getProperties().remove("abc.xyz");
    }

    @Test
    public void testResolveMutilPropertyValue() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("x.y", "c");
        properties.setProperty("a.b", "d");
        properties.setProperty("d.c", "Hello");
        assertEquals("d", VcapServices.resolvePropertyValue("${a.b}", properties));
        assertEquals("d is not c", VcapServices.resolvePropertyValue("${a.b} is not ${x.y}", properties));
        assertEquals("Hello", VcapServices.resolvePropertyValue("${${a.b}.${x.y}}", properties));
        assertEquals(3, properties.size());
    }


    @Test
    public void testNegatablePattern() throws Exception {
        VcapServices.NegatablePattern pattern = new VcapServices.NegatablePattern("/^postgres.*/");
        assertTrue(pattern.matches("postgresql-8.4"));
        assertFalse(pattern.matches("mongodb-1.8"));

        pattern = new VcapServices.NegatablePattern("!/^postgres.*/");
        assertFalse(pattern.matches("postgresql-8.4"));
        assertTrue(pattern.matches("mongodb-1.8"));
    }


    @Test
    public void tesIncludesVcapServices() throws Exception {
        final String json = loadJson("src/test/resources/cloudfoundry/vcap_ups_sample.json");
        VcapServices vcaps = VcapServices.valueOf(json);
        assertEquals(1, vcaps.size());

        ConfigurationHandler handler = new ConfigurationHandler();
        handler.setResource("src/test/resources/cloudfoundry/vcaps.properties");
        handler.parse();
        Configuration config = handler.getConfiguration();
        assertNotNull(config);
        vcaps.applyCredentials(config);
        assertEquals("hoge", config.getString("datasource.global.username"));
        assertEquals("rv2XU8KDcevKy7Wm", config.getString("datasource.global.password"));
    }

    @SuppressWarnings({ "unchecked" })
    static void updateEnv(String name, String val) throws ReflectiveOperationException {
        //On Windows failed
        ProcessBuilder pb = new ProcessBuilder("myCommand", "myArg1", "myArg2");
        Map<String, String> env = pb.environment();
        //Map<String, String> env = System.getenv();
        env.put(name, val);
//        Field field = env.getClass().getDeclaredField("m");
//        field.setAccessible(true);
//        ((Map<String, String>) field.get(env)).put(name, val);
    }

    static String loadJson(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }
}
