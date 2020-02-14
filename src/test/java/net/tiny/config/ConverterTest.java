package net.tiny.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

public class ConverterTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testConvert() throws Exception {
        Converter converter = new Converter();
        assertEquals("12A$", converter.convert("12A$", String.class));
        assertTrue(converter.convert("true", boolean.class));
        assertFalse(converter.convert("false", boolean.class));
        assertTrue(converter.convert("Yes", boolean.class));
        assertFalse(converter.convert("No", boolean.class));
        assertTrue(converter.convert("1", boolean.class));
        assertFalse(converter.convert("0", boolean.class));
        assertEquals(1234, (int)converter.convert("1234", int.class));
        assertEquals(1234567890L, (long)converter.convert("1234567890", long.class));
        assertEquals(123456.789f, (float)converter.convert("123456.7890", float.class));
        assertEquals(123456.789d, (double)converter.convert("123456.7890", double.class));

        List<String> list = converter.convertList("123,abc , 789, xyz", String.class);
        assertEquals("123", list.get(0));
        assertEquals("abc", list.get(1));
        assertEquals("789", list.get(2));
        assertEquals("xyz", list.get(3));

        list = (List<String>)converter.convert("123,abc , 789, xyz", List.class);
        assertEquals("123", list.get(0));
        assertEquals("abc", list.get(1));
        assertEquals("789", list.get(2));
        assertEquals("xyz", list.get(3));


        String[] array = converter.convert("123,abc , 789, xyz", String[].class);
        assertEquals("123", array[0]);
        assertEquals("abc", array[1]);
        assertEquals("789", array[2]);
        assertEquals("xyz", array[3]);

        int[] values = converter.convert("123,456 , 789, 0", int[].class);
        assertEquals(123, values[0]);
        assertEquals(456, values[1]);
        assertEquals(789, values[2]);
        assertEquals(0, values[3]);

        array = converter.convertArray("123,abc , 789, xyz", String.class);
        assertEquals("123", array[0]);
        assertEquals("abc", array[1]);
        assertEquals("789", array[2]);
        assertEquals("xyz", array[3]);

        Set<String> set = converter.convert("123,abc , 789, xyz", Set.class);
        assertTrue(set.contains("123"));
        assertTrue(set.contains("abc"));
        assertTrue(set.contains("789"));
        assertTrue(set.contains("xyz"));

        set = converter.convertSet("123,abc , 789, xyz", String.class);
        assertTrue(set.contains("123"));
        assertTrue(set.contains("abc"));
        assertTrue(set.contains("789"));
        assertTrue(set.contains("xyz"));


        list = converter.convertList("[\"123\",\"abc\" , 789, \"xyz \"]", String.class);
        assertEquals("123", list.get(0));
        assertEquals("abc", list.get(1));
        assertEquals("789", list.get(2));
        assertEquals("xyz ", list.get(3));

        java.sql.Timestamp timestamp = converter.convert("1513346400000", java.sql.Timestamp.class);
        assertNotNull(timestamp);

        timestamp = converter.convert("2019-03-14T02:20:28.941Z", java.sql.Timestamp.class);
        assertNotNull(timestamp);

        DummyType type = converter.convert("ONE", DummyType.class);
        assertEquals(DummyType.ONE, type);

    }

    @Test
    public void testConvertEnumArray() throws Exception {
        Converter converter = new Converter();
        DummyType[] types = converter.convert("\"ONE\", \"TWO\"", DummyType[].class);
        assertEquals(DummyType.ONE, types[0]);
        assertEquals(DummyType.TWO, types[1]);
    }

    public static enum DummyType {
        UNKNOW,
        ONE,
        TWO,
        THREE
    }

    @Test
    public void testConvertertList() {
        String value = "[{\"endpoint\":\"http://localhost:8080/api/v1/tc1/do\",\"channels\":[\"ch1\",\"ch2\"]},{\"endpoint\":\"http://localhost:8080/api/v1/tc2/do\",\"channels\":[\"ch2\"]},{\"endpoint\":\"LocalConsumer\",\"channels\":[\"ch3\"]}]";
        List<String> values = Converter.LIST.convert(value);
        for (String s : values) {
            System.out.println(s);
        }
        assertEquals(3, values.size());

        Converter converter = new Converter();
        List<?> list = converter.convert(value, List.class);
        assertEquals(3, list.size());
        //List<Map> maps = converter.convertList(value, Map.class);
        //assertEquals(3, maps.size());
    }

    @Test
    public void testSingleList() {
        String value = "mail";
        List<String> values = Converter.LIST.convert(value);
        assertEquals(1, values.size());
        assertEquals("mail", values.get(0));

        value = "[mail]";
        values = Converter.LIST.convert(value);
        assertEquals(1, values.size());
        assertEquals("mail", values.get(0));

        value = "[\"mail\"]";
        values = Converter.LIST.convert(value);
        assertEquals(1, values.size());
        assertEquals("mail", values.get(0));

        value = "one,two";
        values = Converter.LIST.convert(value);
        assertEquals(2, values.size());
        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));

        value = "[one,two]";
        values = Converter.LIST.convert(value);
        assertEquals(2, values.size());
        assertEquals("one", values.get(0));
        assertEquals("two", values.get(1));

        value = "[\"one\",\"two \"]";
        values = Converter.LIST.convert(value);
        assertEquals(2, values.size());
        assertEquals("one", values.get(0));
        assertEquals("two ", values.get(1));
    }
}
