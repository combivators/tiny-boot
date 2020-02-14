package net.tiny.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class JsonParserTest {

    //static final String LS = "\r\n";
    static final String LS = System.getProperty("line.separator");


    // https://json.org/example
    @Test
    public void testMap() throws Exception {
        String[] jsons = {
                "{}",
                "{\"foo\": \"bar\"}",
                "{\"foo\": [\"1\", \"2\"]}",
                "{\"foo\": 100}",
                "{\"foo\": \"bar\", \"baz\": \"qux\"}",
                "{\"hoge\" : {\"foo\": \"bar\", \"baz\": \"qux\"}}",
                "{\"foo\": [\"1\", \"2\"], \"baz\": {\"foo\": [true, \"bar\"], \"baz\": \"qux\"}}"
            };

        //assertNotNull(JsonParser.unmarshal(jsons[6], Map.class));
        for(String json : jsons) {
            Map<?,?> map = JsonParser.unmarshal(json, Map.class);
            assertNotNull(map);
            System.out.println(JsonParser.toString(map));
            System.out.println("------------------");
        }
    }

    @Test
    public void testWithLFValue() throws Exception {
        String value = "bar\nhoge";
        assertEquals("bar\\nhoge", value.replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r"));
        String json = "{\"foo\":\"bar\\nhoge\"}";
        Map<?,?> map = JsonParser.unmarshal(json, Map.class);
        assertNotNull(map);
        assertEquals("bar\nhoge", map.get("foo"));
        System.out.println(JsonParser.marshal(map));
        //System.out.println(JsonParser.toString(map));
        assertEquals(json, JsonParser.marshal(map));
        System.out.println(JsonParser.toString(map));


        json = "{\"foo\":[\"1\\n2\",\"a\\nb\"]}";
        map = JsonParser.unmarshal(json, Map.class);
        List<?> list = (List<?>)map.get("foo");
        assertEquals(2, list.size());
        assertEquals("1\n2", list.get(0));
        assertEquals("a\nb", list.get(1));
        System.out.println(JsonParser.marshal(map));
        assertEquals(json, JsonParser.marshal(map));
    }

    @Test
    public void testParserJson() throws Exception {
        Reader reader = new FileReader(new File("src/test/resources/json/example.json"));
        Map<?,?> map = JsonParser.unmarshal(reader, Map.class);
        reader.close();
        assertNotNull(map);
        System.out.println(JsonParser.toString(map));
    }

    @Test
    public void testParserDeepJson() throws Exception {
        Reader reader = new FileReader(new File("src/test/resources/json/sample_policy.json"));
        Map<?,?> map = JsonParser.unmarshal(reader, Map.class);
        assertNotNull(map);
        reader.close();
        System.out.println(JsonParser.toString(map));
    }

    @Test
    public void testParserMapList() throws Exception {
        String json = "{\"success\": [\"TG2301_Stoke Holy Cross\", \r\n" +
                "             \"TF7439_Thornham Corner\", \r\n" +
                "             \"TL8583_Thetford, North\"]}";
        Map<?,?> map = JsonParser.unmarshal(json, Map.class);
        assertEquals(1, map.size());
        assertTrue(map.get("success") instanceof List);
        List<?> list = (List<?>)map.get("success");
        assertEquals(3, list.size());
        assertEquals("TG2301_Stoke Holy Cross", list.get(0));
        assertEquals("TF7439_Thornham Corner", list.get(1));
        assertEquals("TL8583_Thetford, North", list.get(2));
    }


    @Test
    public void testParserMapMap() throws Exception {
        String json = "{\"success\": {\"TG2301\": \"Stoke Holy Cross\", \r\n" +
                "             \"TF7439\": \"Thornham Corner\", \r\n" +
                "             \"TL8583\": \"Thetford, North\"}}";
        Map<?,?> map = JsonParser.unmarshal(json, Map.class);
        assertEquals(1, map.size());
        assertTrue(map.get("success") instanceof Map);
        Map<?,?> sub = (Map<?,?>)map.get("success");
        assertEquals(3, sub.size());
        assertEquals("Stoke Holy Cross", sub.get("TG2301"));
        assertEquals("Thornham Corner", sub.get("TF7439"));
        assertEquals("Thetford, North", sub.get("TL8583"));
    }

    @Test
    public void testUnmarshalObject() throws Exception {
        String json =
          "{" + LS
        + "  \"local\" : \"en-US\"," + LS
        + "  \"lang\" : \"en-US\"," + LS
        + "  \"list\" : [\"en-US\", \"ja-JP\",\"zh-CN\"]," + LS
        + "  \"url\" : \"http://www.abc.com/\"," + LS
        + "  \"cost\" : 1080," + LS
        + "  \"date\" : \"2016/09/16\"," + LS
        + "  \"time\" : \"09:15\"," + LS
        + "  \"nested\" : {" + LS
        + "     \"name\" : \"child\"," + LS
        + "     \"threshold\" : \"1.4\"" + LS
        + "  }," + LS
        + "  \"datetime\" : \"2016/09/16 09:15:45\"" + LS
        + "}" + LS
        + LS;

        assertNotNull(JsonParser.unmarshal(json, Map.class));

        SampleConfig sample = JsonParser.unmarshal(json, SampleConfig.class);
        assertEquals("http://www.abc.com/", sample.getUrl());
        assertEquals("2016-09-16", sample.getDate().toString());
        assertEquals(1080, (int)sample.getCost());
        assertEquals("09:15", sample.getTime().toString());
        assertEquals(3, sample.getList().size());
        Nested nested = sample.getNested();
        assertNotNull(nested);
        assertEquals("child", nested.getName());
        assertEquals(new BigDecimal("1.4"), nested.getThreshold());
    }

    @Test
    public void testParseCustomJson() throws Exception {
        String conf =
          "{" + LS
        + "  local : \"en-US\"," + LS
        + "  lang : \"en-US\"," + LS
        + "  list : [\"en-US\", \"ja-JP\",\"zh-CN\"]," + LS
        + "  url : \"http://www.abc.com/\"," + LS
        + "  cost : \"1080\"," + LS
        + "  date : \"2016/09/16\"," + LS
        + "  time : \"09:15\"," + LS
        + "  datetime : \"2016/09/16 09:15:45\"," + LS
        + "  nested : {" + LS
        + "       name : \"child\"," + LS
        + "     threshold : \"1.4\"" + LS
        + "  }" + LS
        + "}" + LS
        + LS;

        SampleConfig sample = JsonParser.unmarshal(conf, SampleConfig.class);
        assertNotNull(sample);
        assertEquals("http://www.abc.com/", sample.getUrl());
        assertEquals("2016-09-16", sample.getDate().toString());
        assertEquals(1080, (int)sample.getCost());
        assertEquals("09:15", sample.getTime().toString());
        Nested nested = sample.getNested();
        assertNotNull(nested);
        assertEquals("child", nested.getName());
        assertEquals(new BigDecimal("1.4"), nested.getThreshold());

        System.out.println();
        String json = JsonParser.marshal(sample);
        System.out.println(json);
    }

    @Test
    public void testMarshalJavaJson() throws Exception {

        assertEquals("{\"ABCDE\"}", JsonParser.marshal("ABCDE"));
        assertEquals("{1234.5678}", JsonParser.marshal(1234.5678d));
        assertEquals("{\"false\"}", JsonParser.marshal(Boolean.FALSE));
        assertEquals("{\"TWO\"}", JsonParser.marshal(TestType.TWO));
        System.out.print(JsonParser.marshal(LocalDate.now()));
        System.out.print(JsonParser.marshal(new Timestamp(System.currentTimeMillis())));

        System.out.print(JsonParser.marshal(new String[] {"ABCDE", "1234"}));
        System.out.print(JsonParser.marshal(new double[] {12.34d, 56.78d}));
        System.out.print(JsonParser.marshal(new boolean[] {true, false}));
        System.out.print(JsonParser.marshal(new Timestamp[] {new Timestamp(1513346400000L), new Timestamp(1513348500000L)}));
        System.out.print(JsonParser.marshal(new TestType[] {TestType.ONE, TestType.THREE}));

        List<String> list = new ArrayList<>();
        list.add("ABCDE");
        list.add("1234");
        list.add("xyz");
        System.out.print(JsonParser.marshal(list));
    }

    @Test
    public void testMarshalJavaTransientedJson() throws Exception {
        TransientedBean vb = new TransientedBean();
        vb.setExpression("a+b-c");
        String json = JsonParser.marshal(vb);
        System.out.print(json);
        assertTrue(json.contains("expression"));
        assertFalse(json.contains("_exp"));

    }


    @Test
    public void testMarshalMapJson() throws Exception {
        Map<String, String> one = new HashMap<>();
        one.put("name", "ABCDE");
        one.put("value", "1234");
        one.put("date", "2015/01/25");
        String json = JsonParser.marshal(one, false);
        assertTrue(json.contains("\n"));
        System.out.println(json);

        Map<String, String> two = new HashMap<>();
        two.put("name", "XYZ");
        two.put("value", "9876");
        two.put("date", "2000/12/25");

        Map<String, Map<String, String>> main = new HashMap<>();
        main.put("one", one);
        main.put("two", two);
        json = JsonParser.marshal(main, false);
        assertTrue(json.contains("\n"));
        System.out.print(json);
    }

    @Test
    public void testMarshalOnelineMapJson() throws Exception {
        Map<String, String> one = new HashMap<>();
        one.put("name", "ABCDE");
        one.put("value", "1234");
        one.put("date", "2015/01/25");
        String json = JsonParser.marshal(one, true);
        assertFalse(json.contains("\r"));
        assertFalse(json.contains("\n"));
        System.out.println(json);

        Map<String, String> two = new HashMap<>();
        two.put("name", "XYZ");
        two.put("value", "9876");
        two.put("date", "2000/12/25");

        Map<String, Map<String, String>> main = new HashMap<>();
        main.put("one", one);
        main.put("two", two);
        json = JsonParser.marshal(one, true);
        assertFalse(json.contains("\r"));
        assertFalse(json.contains("\n"));
        System.out.print(json);
    }


    @Test
    public void testMarshalMapListJson() throws Exception {
        //{q=[svg], t=[min]}
        List<String> one = new ArrayList<>();
        one.add("svg");
        List<String> two = new ArrayList<>();
        two.add("min");

        Map<String, List<String>> main = new HashMap<>();
        main.put("q", one);
        main.put("t", two);

        String json = JsonParser.marshal(main);
        System.out.print(json);
        Map<?,?> map = JsonParser.unmarshal(json, Map.class);
        assertEquals(2, map.size());
        assertTrue(map.get("q") instanceof List);
        List<?> l = (List<?>)map.get("q");
        assertEquals(1, l.size());
    }

    @Test
    public void testMarshalList() throws Exception {
        String data = "[{\"name\" : \"one\"},{\"name\" : \"two\"},{\"name\" : \"three\"}]";
        List<Lasted> values = JsonParser.unmarshals(new StringReader(data), Lasted.class);
        assertEquals(3, values.size());
        assertEquals("one", values.get(0).getName());

        Lasted one = new Lasted();
        one.setName("one");
        Lasted two = new Lasted();
        two.setName("two");
        Lasted three = new Lasted();
        three.setName("three");
        List<Lasted> list = Arrays.asList(one, two, three);
        String json = JsonParser.marshal(list);

        System.out.println("---------------");
        System.out.println(json);
        System.out.println("---------------");

        values = JsonParser.unmarshals(new StringReader(json), Lasted.class);
        assertEquals(3, values.size());
        assertEquals("one", values.get(0).getName());
    }

    @Test
    public void testMarshalCollection() throws Exception {
        CollectionBean bean = new CollectionBean();
        Set<String> list = new HashSet<>();
        list.add("ABC");
        list.add("123");
        list.add("xyz");
        bean.setNames(list);
        bean.setArray(new String[] {"en-US", "ja-JP","zh-CN"});

        Nested n1 = new Nested();
        n1.setName("Hoge");
        n1.setThreshold(BigDecimal.ZERO);
        Nested n2 = new Nested();
        n2.setName("Fuga");
        n2.setThreshold(BigDecimal.ONE);
        Nested n3 = new Nested();
        n3.setName("Jone");
        n3.setThreshold(BigDecimal.TEN);
        bean.setNesteds(Arrays.asList(n1, n2, n3));
        String json = JsonParser.marshal(bean);
        System.out.println(json);
    }

    @Test
    public void testUnmarshalJson() throws Exception {
        String json =
          "{" + LS
        + "  list : [\"en-US\", \"ja-JP\",\"zh-CN\"]," + LS
        + "  url : \"http://www.abc.com/\"," + LS
        + "  cost : \"1080\"," + LS
        + "  date : \"2016/09/16\"," + LS
        + "  time : \"09:15\"," + LS
        + "  nested : {" + LS
        + "     lasted : {" + LS
        + "        name : \"The last\"," + LS
        + "     }," + LS
        + "     name : \"child\"," + LS
        + "     threshold : \"1.4\"" + LS
        + "  }," + LS
        + "  datetime : \"2016/09/16 09:15:45\"" + LS
        + "}" + LS
        + LS;

        SampleConfig sample = JsonParser.unmarshal(json, SampleConfig.class);
        assertNotNull(sample);
        assertEquals("http://www.abc.com/", sample.getUrl());
        assertEquals("2016-09-16", sample.getDate().toString());
        assertEquals(1080, (int)sample.getCost());
        assertEquals("09:15", sample.getTime().toString());
        assertEquals(3, sample.getList().size());
        Nested nested = sample.getNested();
        assertNotNull(nested);
        Lasted lasted = nested.getLasted();
        assertNotNull(lasted);
        assertEquals("The last", lasted.getName());
        assertEquals("child", nested.getName());
    }

    @Test
    public void testUnmarshalJavaJson() throws Exception {
        assertEquals("ABCDE", JsonParser.unmarshal("{\"ABCDE\"}", String.class));
        assertEquals("ABCDE", JsonParser.unmarshal("{\"ABCDE\"}\r\n\r\n", String.class));
        assertTrue(JsonParser.unmarshal("{\"True\"}", boolean.class));
        assertTrue(JsonParser.unmarshal("{\"Yes\"}", Boolean.class));
        assertTrue(JsonParser.unmarshal("{\"1\"}", boolean.class));
        assertFalse(JsonParser.unmarshal("{\"False\"}", boolean.class));
        assertFalse(JsonParser.unmarshal("{\"No\"}", Boolean.class));
        assertFalse(JsonParser.unmarshal("{\"0\"}", boolean.class));
        assertTrue(1234.5678d == JsonParser.unmarshal("{1234.5678}", double.class));
        assertEquals(TestType.TWO, JsonParser.unmarshal("{\"TWO\"}", TestType.class));

        String[] array = JsonParser.unmarshal("{[\"ABCDE\",\"1234\"]}",String[].class);
        assertNotNull(array);
        assertEquals(2, array.length);
        assertEquals("ABCDE", array[0]);
        assertEquals("1234", array[1]);
        long[] values = JsonParser.unmarshal("{[1234, 9876]}\r\n",long[].class);
        assertEquals(2, values.length);
        assertEquals(1234L, values[0]);
        assertEquals(9876L, values[1]);

        boolean[] flags = JsonParser.unmarshal("{[1, True, false, Yes, no, 0]}\r\n",boolean[].class);
        assertEquals(6, flags.length);
        assertTrue( flags[0]);
        assertTrue( flags[1]);
        assertFalse( flags[2]);
        assertTrue( flags[3]);
        assertFalse( flags[4]);
        assertFalse( flags[5]);

        TestType[] types = JsonParser.unmarshal("{[\"ONE\", \"THREE\"]}",TestType[].class);
        assertEquals(2, types.length);
        assertEquals(TestType.ONE, types[0]);
        assertEquals(TestType.THREE, types[1]);

        Timestamp[] timestamps = JsonParser.unmarshal("{[1513346400000, 1513348500000]}\r\n", Timestamp[].class);
        assertEquals(2, timestamps.length);

        List<?> list = JsonParser.unmarshal("{[\"ONE\", \"TWO\"]}", List.class);
        assertEquals(2, list.size());
        assertEquals("ONE", list.get(0));
        assertEquals("TWO", list.get(1));

        Set<?> sets = JsonParser.unmarshal("{[\"ONE\", \"TWO\"]}", Set.class);
        assertEquals(2, sets.size());
        Object[] m = sets.toArray(new Object[sets.size()]);
        assertEquals("ONE", m[0]);
        assertEquals("TWO", m[1]);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnmarshalMapJson() throws Exception {
        String json =
          "{" + LS
        + "  local : \"en-US\"," + LS
        + "  lang : \"en-US\"," + LS
        + "  list : [\"en-US\", \"ja-JP\",\"zh-CN\"]," + LS
        + "  url : \"http://www.abc.com/\"," + LS
        + "  name : \"hoge\"," + LS
        + "  date : \"2016/09/16\"," + LS
        + "  nested : {" + LS
        + "     name : \"child\"," + LS
        + "     threshold : \"1.4\"" + LS
        + "  }," + LS
        + "  value : 1234" + LS
        + "}" + LS;

        Map<String, Object> map = JsonParser.unmarshal(json, Map.class);
        assertNotNull(map);
        assertEquals(8, map.size());
        assertEquals(Double.valueOf("1234"), map.get("value"));
        assertEquals("2016/09/16", map.get("date"));
        Object value = map.get("nested");
        assertNotNull(value);
        assertTrue(value instanceof Map);
        Map<String, Object> nested = (Map<String, Object>)value;
        assertEquals("child", nested.get("name"));
        assertEquals("1.4", nested.get("threshold"));

        value = map.get("list");
        assertNotNull(value);
        assertTrue(value instanceof List);
        List<Object> list = (List<Object>)value;
        assertEquals(3, list.size());
        assertEquals("en-US", list.get(0));
    }

    @Test
    public void testUnmarshalCollection() throws Exception {
        String json =
                "{" + LS
              + "  names : [\"ABC\",\"123\",\"xyz\"]," + LS
              + "  array : [\"en-US\",\"ja-JP\",\"zh-CN\"]," + LS
              + "  nesteds : [" + LS
              + "    {" + LS
              + "     name : \"Hoge\"," + LS
              + "     threshold : \"14\"" + LS
              + "    }," + LS
              + "    {" + LS
              + "     name : \"Fuga\"," + LS
              + "     threshold : \"12\"" + LS
              + "    }," + LS
              + "    {" + LS
              + "     name : \"Jone\"," + LS
              + "     threshold : \"10\"" + LS
              + "    }" + LS
              + "  ]" + LS
              + "}" + LS;
        CollectionBean bean = JsonParser.unmarshal(json, CollectionBean.class);
        assertNotNull(bean);
        assertNotNull(bean.getNames());
        assertNotNull(bean.getArray());
        assertNotNull(bean.getNesteds());
        assertEquals(3, bean.getNames().size());
        assertEquals(3, bean.getArray().length);
        assertEquals(3, bean.getNesteds().size());
        assertEquals("Hoge", bean.getNesteds().get(0).getName());
        assertEquals("10", bean.getNesteds().get(2).getThreshold().toString());
    }

/*
    @Test
    public void testSliptJsons() throws Exception {
        String json =
                "[" + LS
              + "    {" + LS
              + "     name : \"Hoge\"," + LS
              + "     threshold : \"14\"," + LS
              + "     contents : {" + LS
              + "        number : \"1234\"," + LS
              + "        detail : \"abc\"" + LS
              + "     }" + LS
              + "    }," + LS
              + "    {" + LS
              + "     name : \"Fuga\"," + LS
              + "     threshold : \"12\"" + LS
              + "     contents : [" + LS
              + "       \"1234\"," + LS
              + "       \"abc\"" + LS
              + "     ]" + LS
              + "    }," + LS
              + "    {" + LS
              + "     name : \"Jone's\"," + LS
              + "     threshold : \"10\"" + LS
              + "    }" + LS
              + "  ]" + LS;

        List<String> jsons = JsonParser.splitJson(json);
        assertEquals(3, jsons.size());
        for(String parts : jsons) {
            System.out.println(parts);
            System.out.println("---------------------");
        }
    }

    @Test
    public void testNextBracket() throws Exception {
        String json =
                "[" + LS
              + "    {" + LS
              + "     name : \"Hoge\"," + LS
              + "     threshold : \"14\"," + LS
              + "     contents : {" + LS
              + "        number : \"1234\"," + LS
              + "        detail : \"abc\"" + LS
              + "     }" + LS
              + "    }," + LS
              + "    {" + LS
              + "     name : \"Fuga\"," + LS
              + "     threshold : \"12\"" + LS
              + "     contents : [" + LS
              + "       \"1234\"," + LS
              + "       \"abc\"" + LS
              + "     ]" + LS
              + "    }," + LS
              + "    {" + LS
              + "     name : \"Jone's\"," + LS
              + "     threshold : \"10\"" + LS
              + "    }" + LS
              + "]" + LS;

        StringReader reader = new StringReader(json);
        StreamTokenizer tokenizer = JsonParser.createStreamTokenizer(reader);
        int token = 0;
        StringBuilder buffer = new StringBuilder();
        while ((token = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
            if(token == StreamTokenizer.TT_EOL) {
                buffer.append("[");
                break;
            }
        }
        String block = JsonParser.nextBracket(tokenizer);
        System.out.println(block);

        //JsonParser.splitJson(buffer.append(block).toString());
    }

    @Test
    public void testParseListJsons() throws Exception {
        String json =
                "[" + LS
              + "    {" + LS
              + "     name : \"Hoge\"," + LS
              + "     threshold : \"14\"" + LS
              + "    }," + LS
              + "    {" + LS
              + "     name : \"Fuga\"," + LS
              + "     threshold : \"12\"" + LS
              + "    }," + LS
              + "    {" + LS
              + "     name : \"Jone's\"," + LS
              + "     threshold : \"10\"" + LS
              + "    }" + LS
              + "  ]" + LS;
        List list = new ArrayList();
        JsonParser.parseJsons(json, list, Nested.class);
        assertEquals(3, list.size());
        Nested nested = (Nested)list.get(0);
        assertEquals("Hoge", nested.getName());
        assertEquals("14", nested.getThreshold().toString());

        nested = (Nested)list.get(2);
        assertEquals("Jone's", nested.getName());
        assertEquals("10", nested.getThreshold().toString());
    }

*/

    public static abstract class AbstractConfig implements Serializable {
        private static final long serialVersionUID = 1L;
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

    public static class Nested implements Serializable {
        private static final long serialVersionUID = 1L;
        private Lasted lasted;
        private String name;
        private BigDecimal threshold;

        public Lasted getLasted() {
            return lasted;
        }
        public void setLasted(Lasted lasted) {
            this.lasted = lasted;
        }
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

    public static class Lasted implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }

    public static class SampleConfig extends AbstractConfig {
        private static final long serialVersionUID = 1L;
        private LocalDate date;
        private LocalTime time;
        private LocalDateTime datetime;
        private List<String> list;
        private Nested nested;

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
        public LocalDateTime getDatetime() {
            return datetime;
        }
        public void setDatetime(LocalDateTime datetime) {
            this.datetime = datetime;
        }
        public List<String> getList() {
            return list;
        }
        public void setList(List<String> list) {
            this.list = list;
        }
        public Nested getNested() {
            return nested;
        }
        public void setNested(Nested nested) {
            this.nested = nested;
        }
    }

    public static class CollectionBean {
        private Set<String> names;
        private String[] array;
        private List<Nested> nesteds;

        public Set<String> getNames() {
            return names;
        }
        public void setNames(Set<String> names) {
            this.names = names;
        }

        public String[] getArray() {
            return array;
        }
        public void setArray(String[] array) {
            this.array = array;
        }

        public List<Nested> getNesteds() {
            return nesteds;
        }
        public void setNesteds(List<Nested> nesteds) {
            this.nesteds = nesteds;
        }
    }

    public static class TransientedBean {
        String expression;
        transient String _exp;
        static String EXP = "Hello";

        public void setExpression(String exp) {
            expression = exp;
            _exp = "(" + exp + ")";
        }
    }

    public static enum TestType {
        UNKNOW,
        ONE,
        TWO,
        THREE
    }

}
