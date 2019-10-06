package net.tiny.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

public class ConfigurationTest {
    static final String LS = System.getProperty("line.separator");

    @Test
    public void tesRreplaceValue() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("x.y", "XYZ");
        properties.setProperty("a.b", "d");
        properties.setProperty("d.c", "${x.y}");
        String value = "${a.b} is ${d.c}";

        Configuration.VariablesReplacement replacement = new Configuration.VariablesReplacement() {
            @Override
            String replace(String var) {
                return properties.getProperty(var);
            }
        };
        String ret = replacement.replaceValue(value);
        assertEquals("d is XYZ", ret);
        value = "${${a.b}.c}";
        ret = replacement.replaceValue(value);
        assertEquals("XYZ", ret);
    }

    @Test
    public void tesRreplaceNameOnly() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("x.y", "XYZ");
        properties.setProperty("a.b", "d");
        properties.setProperty("d.c", "${x.y}");

        Configuration.VariablesReplacement replacement = new Configuration.VariablesReplacement() {
            @Override
            String replace(String var) {
                return properties.getProperty(var);
            }
        };

        String value = "${a.b}";
        String ret = replacement.replaceName(value);
        assertEquals("a.b", ret);

        value = "${a.b}.${d.c}";
        ret = replacement.replaceName(value);
        assertEquals("d.XYZ", ret);

        value = "${${a.b}.c}";
        ret = replacement.replaceName(value);
        assertEquals("d.c", ret);
    }

    @Test
    public void testGetReferenceString() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("a.b.c1", "ABC1");
        properties.setProperty("x.y.z1", "XYZ1");
        Configuration config = new Configuration(properties, null);

        String value = config.getReference("${a.b.c1}, n, m, ${x.y.z1}");
        System.out.println(value);
        assertEquals("ABC1, n, m, XYZ1", value);
    }

    static class Dummy {
        private DummyBean dummyBean;
        private List<DummyBean> beans;

        public DummyBean getDummyBean() {
            return dummyBean;
        }

        public void setDummyBean(DummyBean dummyBean) {
            this.dummyBean = dummyBean;
        }

        public List<DummyBean> getBeans() {
            return beans;
        }

        public void setBeans(List<DummyBean> beans) {
            this.beans = beans;
        }
    }

    @Test
    public void testGetReferenceObject() throws Exception {
        Field field = Dummy.class.getDeclaredField("dummyBean");
        Properties properties = new Properties();
        Configuration config = new Configuration(properties, null);
        Object value = config.getReference("${a.b.c1}, ${x.y.z1}", field);
        assertNull(value);

        DummyBean bean = new DummyBean();
        properties.put("a.b.c1", bean);
        config = new Configuration(properties, null);
        value = config.getReference("${a.b.c1}", field);
        assertTrue(value instanceof DummyBean);
        assertEquals(bean, value);
    }

    @Test
    public void testGetReferenceList() throws Exception {
        Field field = Dummy.class.getDeclaredField("beans");
        Properties properties = new Properties();

        DummyBean b1 = new DummyBean();
        properties.put("a.b.c1", b1);
        DummyBean b2 = new DummyBean();
        properties.put("x.y.z1", b2);
        assertNotEquals(b1, b2);

        Configuration config = new Configuration(properties, null);
        Object value = config.getReference("${a.b.c1}, ${x.y.z1}", field);
        assertNotNull(value);
        assertTrue(value instanceof List);
        List<?> beans = (List<?>) value;
        assertEquals(2, beans.size());
        assertEquals(b1, beans.get(0));
        assertEquals(b2, beans.get(1));

        value = config.getReference("[${a.b.c1}, ${x.y.z1}]", field);
        assertNotNull(value);
        assertTrue(value instanceof List);
        beans = (List<?>) value;
        assertEquals(2, beans.size());
        assertEquals(b1, beans.get(0));
        assertEquals(b2, beans.get(1));
    }

    @Test
    public void testGetReference() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("c1", "root");
        properties.setProperty("c2", "user");
        properties.setProperty("c3", "local");
        Configuration config = new Configuration(properties, null);
        String value = config.getReference("[${c1},${c2},${c3}]");
        assertEquals("[root,user,local]", value);
    }

    @Test
    public void testGetValue() throws Exception {
        String prop =
                "# Comment" + LS
                + "APP.sample.url=http://www.abc.com/" + LS
                + "APP.sample.cost = 1080" + LS
                + "APP.sample.date = 2016/09/16" + LS
                + "APP.sample.time = 09:15" + LS
                + "APP.sample.datetime = 2016/09/16 09:15" + LS
                + "APP.sample.nested.name = child" + LS
                + "APP.sample.nested.threshold = 1.4" + LS
                + LS;

        ByteArrayInputStream bais = new ByteArrayInputStream(prop.getBytes());
        Properties properties = new Properties();
        properties.load(bais);
        bais.close();

        Configuration config = new Configuration(properties, null);
        assertEquals("http://www.abc.com/", config.getString("APP.sample.url"));
        assertEquals("1080", config.getString("APP.sample.cost"));
        assertEquals(1080, (int) config.getInteger("APP.sample.cost"));
        assertEquals(1080L, (long) config.getLong("APP.sample.cost"));
        Set<String> names = config.getAllPropertyNames();
        assertEquals(7, names.size());
        names = config.getPropertyNames(n -> n.contains("nested"));
        assertEquals(2, names.size());

        Configuration sub = config.getConfiguration("APP.sample.nested");
        assertNotNull(sub);
        assertEquals("child", sub.getString("name"));
        assertEquals(new Double(1.4d), sub.getDouble("threshold"));
        assertEquals(8, config.size());
        // Test cache
        Configuration other = config.getConfiguration("APP.sample.nested");
        assertEquals(sub, other);
        assertEquals(8, config.size());
    }

    @Test
    public void testGetPropertyValue() throws Exception {
        String prop =
                "# Comment" + LS
                + "app.sample.url=http://www.abc.com/" + LS
                + "app.sample.cost = 1080" + LS
                + "app.sample.date=2016/09/16" + LS
                + "app.sample.site = ${app.sample.url}" + LS
                + "app.sample.web = ${app.sample.site}" + LS
                + "app.sample.value = ${app.sample.cost}" + LS
                + "app.sample.day = ${app.sample.date}" + LS
                + "app.sample.reuse = ${app.sample.reuse}" + LS
                + LS;

        ByteArrayInputStream bais = new ByteArrayInputStream(prop.getBytes());
        Properties properties = new Properties();
        properties.load(bais);
        bais.close();

        Configuration config = new Configuration(properties, new ConfigMonitor());
        assertEquals("http://www.abc.com/", config.getString("app.sample.url"));
        assertEquals("1080", config.getString("app.sample.cost"));
        assertEquals("2016-09-16", config.getLocalDate("app.sample.date").toString());

        assertEquals("http://www.abc.com/", config.getString("app.sample.site"));
        assertEquals("http://www.abc.com/", config.getString("app.sample.web"));
        assertEquals(1080, (int) config.getInteger("app.sample.value"));
        assertEquals("2016-09-16", config.getLocalDate("app.sample.day").toString());
    }

    @Test
    public void testCycleReference() throws Exception {
        String prop =
                "# Comment" + LS
                + "app.sample.url=http://www.abc.com/" + LS
                + "app.sample.cost = 1080" + LS
                + "app.sample.date=2016/09/16" + LS
                + "app.sample.reuse = ${app.sample.reuse}" + LS
                + LS;
        ByteArrayInputStream bais = new ByteArrayInputStream(prop.getBytes());
        Properties properties = new Properties();
        properties.load(bais);
        bais.close();
        Configuration config = new Configuration(properties, null);
        try {
            config.getString("app.sample.reuse");
            fail();
        } catch (Exception ex) {
            assertTrue(ex instanceof RuntimeException);
            assertEquals("The property value '${app.sample.reuse}' can not resue self name.", ex.getMessage());
        }
    }

    @Test
    public void testSidewaysCycleReference() throws Exception {
        String prop =
                "# Comment" + LS
                + "app.sample.url=http://www.abc.com/" + LS
                + "app.sample.cost = 1080" + LS
                + "app.sample.date=2016/09/16" + LS
                + "app.sample.a = ${app.sample.b}" + LS
                + "app.sample.b = ${app.sample.a}" + LS
                + LS;
        ByteArrayInputStream bais = new ByteArrayInputStream(prop.getBytes());
        Properties properties = new Properties();
        properties.load(bais);
        bais.close();
        Configuration config = new Configuration(properties, new ConfigMonitor());
        try {
            String ret = config.getString("app.sample.b");
            fail(ret);
        } catch (Exception ex) {
            assertTrue(ex instanceof RuntimeException);
            assertEquals("Can not cycle reference '${app.sample.b}'.", ex.getMessage());
        }
    }


    @Test
    public void testManyReference() throws Exception {
        String prop =
                "# Comment" + LS
                + "app:" + LS
                + "  m:" + LS
                + "    a: 1" + LS
                + "    b: 2" + LS
                + "    c: 3" + LS
                + "    d: 4" + LS
                + "    e: 5" + LS
                + "    f: 6" + LS
                + "    g: 7" + LS
                + "    h: 8" + LS
                + "    i: 9" + LS
                + "    j: 10" + LS
                + "    k: 11" + LS
                + "    l: 12" + LS
                + "  g: ${app.m.a}, ${app.m.b}, ${app.m.c}, ${app.m.d}, ${app.m.e}, ${app.m.f}, ${app.m.g}, "
                + "${app.m.h}, ${app.m.i}, ${app.m.j}, ${app.m.k}, ${app.m.l}" + LS
                + LS;
        ByteArrayInputStream bais = new ByteArrayInputStream(prop.getBytes());
        ConfigurationHandler handler = new ConfigurationHandler();
        handler.parse(bais, ContextHandler.Type.YAML);
        Configuration config = handler.getConfiguration();
        assertNotNull(config);
        List<?> list = config.getAs("app.g", List.class);
        assertNotNull(list);
        assertEquals(12, list.size());
    }

    @Test
    public void testGetAsBean() throws Exception {
        String prop =
                "#" + LS
                + "app.sample.url=http://www.abc.com/" + LS
                + "app.sample.cost = 1080" + LS
                + "app.sample.date=2016/09/16" + LS
                + "app.sample.nested.name = child" + LS
                + "app.sample.nested.threshold = 1.4" + LS
                + "app.sample.target = ${this}" + LS
                + LS;

        ByteArrayInputStream bais = new ByteArrayInputStream(prop.getBytes());
        Properties properties = new Properties();
        properties.load(bais);
        bais.close();

        Configuration config = new Configuration(properties, new ConfigMonitor());
        Configuration sub = config.getConfiguration("app.sample.nested");
        Nested nested = config.getAsBean(Nested.class, sub);
        assertNotNull(nested);
        assertEquals("child", nested.getName());
        assertEquals(new BigDecimal("1.4"), nested.getThreshold());
        Properties subProp = config.getProperties("app.sample.nested");
        assertEquals("child", subProp.getProperty("name"));
        assertEquals("1.4", subProp.getProperty("threshold"));

        SampleConfig sample = config.getAs(SampleConfig.class);
        assertNotNull(sample);
        assertEquals("http://www.abc.com/", sample.getUrl());
        assertEquals("2016-09-16", sample.getDate().toString());
        assertEquals(1080, (int) sample.getCost());
        assertNull(sample.getTime());
        nested = sample.getNested();
        assertNotNull(nested);
        assertEquals("child", nested.getName());
        assertEquals(new BigDecimal("1.4"), nested.getThreshold());
        assertEquals(config, sample.getTarget());

        // Test cache
        SampleConfig other = config.getAs(SampleConfig.class);
        assertEquals(sample, other);
    }

    @Test
    public void testGetReferenceBeanList() throws Exception {
        String prop =
                "#" + LS
                + "main = ${one}, ${two}" + LS
                + "one.class = " + One.class.getName() + LS
                + "one.name = One1" + LS
                + "two.class = " + Two.class.getName() + LS
                + "two.name = Two2" + LS + LS;

        ByteArrayInputStream bais = new ByteArrayInputStream(prop.getBytes());
        Properties properties = new Properties();
        properties.load(bais);
        bais.close();

        Configuration config = new Configuration(properties, new ConfigMonitor());
        Configuration conf = config.getConfiguration("one");
        Object one = config.getAsBean(conf);
        System.out.println(one.getClass().getName());
        assertTrue(One.class.equals(one.getClass()));
        /*
         * One one = config.getAs("one", One.class); assertNotNull(one); Two two =
         * config.getAs("two", Two.class); assertNotNull(two);
         */
        List<?> list = config.getAs("main", List.class);
        assertNotNull(list);
        assertEquals(2, list.size());
        assertTrue(One.class.equals(list.get(0).getClass()));
        assertTrue(Two.class.equals(list.get(1).getClass()));
        System.out.println(list.get(0).getClass().getName());
        System.out.println(list.get(1).getClass().getName());
    }

    @Test
    public void testGetReferenceOneBean() throws Exception {
        String prop = "#" + LS + "main = ${one}" + LS + "one.class = " + One.class.getName() + LS + "one.name = One1"
                + LS + "two.class = " + Two.class.getName() + LS + "two.name = Two2" + LS + LS;

        ByteArrayInputStream bais = new ByteArrayInputStream(prop.getBytes());
        Properties properties = new Properties();
        properties.load(bais);
        bais.close();

        Configuration config = new Configuration(properties, new ConfigMonitor());
        One one = config.getAs("one", One.class);
        assertNotNull(one);
        Two two = config.getAs("two", Two.class);
        assertNotNull(two);
        List<?> list = config.getAs("main", List.class);
        assertNotNull(list);
        assertEquals(1, list.size());
    }

    public static abstract class AbstractConfig {
        private String url;
        private Integer cost;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Integer getCost() {
            return cost;
        }

        public void setCost(Integer cost) {
            this.cost = cost;
        }
    }

    @Config("app.sample")
    public static class SampleConfig extends AbstractConfig {

        private LocalDate date;
        private LocalTime time;
        private List<String> array;
        private Nested nested;
        private Object target;

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public LocalTime getTime() {
            return time;
        }

        public void setTime(LocalTime time) {
            this.time = time;
        }

        public List<String> getArray() {
            return array;
        }

        public void setArray(List<String> array) {
            this.array = array;
        }

        public Nested getNested() {
            return nested;
        }

        public void setNested(Nested nested) {
            this.nested = nested;
        }

        public Object getTarget() {
            return target;
        }

        public void setTarget(Object target) {
            this.target = target;
        }
    }

    public static class Nested implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private BigDecimal threshold;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getThreshold() {
            return threshold;
        }

        public void setThreshold(BigDecimal threshold) {
            this.threshold = threshold;
        }
    }

    public static class DummyBean implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public static class One {
        private String name;

        public String getName() {
            return name;
        }
    }

    public static class Two {
        private String name;

        public String getName() {
            return name;
        }
    }

    @Config("") // TODO
    public static class App {
        private String name;
        private long value;

        public App() {
            System.out.println(String.format("# [Instance] App#%1$d", hashCode()));
        }

        public String getName() {
            return name;
        }

        public long getValue() {
            return value;
        }

    }

    @Test
    public void testGetMemberType() throws Exception {
        Field field = TestType.class.getDeclaredField("valueString");
        assertNotNull(field);
        Class<?> type = Configuration.getMemberType(field);
        assertEquals(String.class, type);
        field = TestType.class.getDeclaredField("listString");
        type = Configuration.getMemberType(field);
        assertEquals(String.class, type);

        field = TestType.class.getDeclaredField("valueCustom");
        type = Configuration.getMemberType(field);
        assertEquals(CustomType.class, type);
        field = TestType.class.getDeclaredField("listCustom");
        type = Configuration.getMemberType(field);
        assertEquals(CustomType.class, type);

        field = TestType.class.getDeclaredField("valueInterface");
        type = Configuration.getMemberType(field);
        assertEquals(CustomIF.class, type);
        field = TestType.class.getDeclaredField("listInterface");
        type = Configuration.getMemberType(field);
        assertEquals(CustomIF.class, type);

        field = TestType.class.getDeclaredField("filters");
        type = Configuration.getMemberType(field);
        assertEquals(AbstractType.Filter.class, type);

        field = TestType.class.getDeclaredField("sets");
        type = Configuration.getMemberType(field);
        assertEquals(AbstractType.class, type);

    }

    public static class CustomType {
    }

    public static interface CustomIF {
    }

    public static abstract class AbstractType {
        public static abstract class Filter<T> {
            public abstract T doFilter(T request);
        }
    }

    public static class TestType<T> {
        String valueString;
        List<String> listString;
        CustomType valueCustom;
        List<CustomType> listCustom;
        CustomIF valueInterface;
        List<CustomIF> listInterface;
        AbstractType valueAbstract;
        List<AbstractType> listAbstract;

        List<AbstractType.Filter<T>> filters;
        Set<AbstractType> sets = Collections.synchronizedSortedSet(new TreeSet<>());
    }

    @Test
    public void testCollectionBean() throws Exception {
        String conf =
                "APP {" + LS
                + "    name = \"Sample\"" + LS
                + "    array = abc, 123, xyz" + LS
                + "    list = [${c1},${c2},${c3}]" + LS
                + "    values = [123,456,789]" + LS
                + "    contents = [${c1},${c2},${c3}]" + LS
                + "}" + LS
                + "c1 = \"root\"" + LS
                + "c2 = \"user\"" + LS
                + "c3 = \"local\"" + LS
                + LS;

        ByteArrayInputStream bais = new ByteArrayInputStream(conf.getBytes());
        ConfigurationHandler handler = new ConfigurationHandler();
        handler.parse(bais, ContextHandler.Type.HOCON);
        Configuration config = handler.getConfiguration();
        assertNotNull(config);
        CollectionBean bean = config.getAs("APP", CollectionBean.class);
        assertNotNull(bean);
        assertEquals("Sample", bean.getName());
        assertEquals(3, bean.getArray().length);
        assertEquals(3, bean.getList().size());
        assertEquals(3, bean.getValues().size());
        assertEquals(3, bean.getContents().size());
        assertEquals("root", config.getString("c1"));
        assertEquals("user", config.getString("c2"));
        assertEquals("local", config.getString("c3"));
    }

    public static class CollectionBean {
        private String name;
        private String[] array = new String[0];
        private List<String> list = new ArrayList<>();
        private Set<Long> values = Collections.synchronizedSortedSet(new TreeSet<>());
        private Set<String> contents = Collections.synchronizedSortedSet(new TreeSet<>());

        public String getName() {
            return name;
        }

        public String[] getArray() {
            return array;
        }

        public List<String> getList() {
            return list;
        }

        public Set<Long> getValues() {
            return values;
        }

        public Set<String> getContents() {
            return contents;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setArray(String[] array) {
            this.array = array;
        }

        public void setList(List<String> list) {
            this.list = list;
        }

        public void setValues(Set<Long> values) {
            this.values = values;
        }

        public void setContents(Set<String> contents) {
            this.contents = contents;
        }

        public void setNumbers(List<Double> numbers) {
        }
    }

    static class ConfigMonitor implements ContextHandler.Listener {

        @Override
        public void created(Object bean, Class<?> beanClass) {
            System.out.println(String.format("[CONFIG] An object '%s'#%d was created.", beanClass.getSimpleName(), bean.hashCode()));
        }
        @Override
        public void parsed(String type, String resource, int size) {
            System.out.println(String.format("[CONFIG] %s '%s'(%d) was parsed.", type, String.valueOf(resource), size));
        }
        @Override
        public void cached(String name, Object value, boolean config) {
            if (config)
                System.out.println(String.format("[BOOT] Cached Configuration#%d'", value.hashCode()));
            else
                System.out.println(String.format("[BOOT] Cached %s = %s", name, value.toString()));
        }
    }
}
