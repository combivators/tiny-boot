package net.tiny.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;


public class JsonParserListTest {

    @Test
    public void testMarshalList() {

        List<TestBean> consumers = new ArrayList<>();
        TestBean c1 = new TestBean();
        c1.setChannels(Arrays.asList("ch1", "ch2"));
        c1.setEndpoint("http://localhost:8080/api/v1/tc1/do");

        TestBean c2 = new TestBean();
        c2.setChannels(Arrays.asList("ch2"));
        c2.setEndpoint("http://localhost:8080/api/v1/tc2/do");

        TestBean c3 = new TestBean();
        c3.setChannels(Arrays.asList("ch3"));
        c3.setEndpoint("LocalConsumer");

        consumers.add(c1);
        consumers.add(c2);
        consumers.add(c3);
        assertEquals(3, consumers.size());

        String json = JsonParser.marshal(consumers);
        System.out.println(json);
        assertEquals(195, json.length());
        assertEquals("[{\"endpoint\":\"http://localhost:8080/api/v1/tc1/do\",\"channels\":[\"ch1\",\"ch2\"]},{\"endpoint\":\"http://localhost:8080/api/v1/tc2/do\",\"channels\":[\"ch2\"]},{\"endpoint\":\"LocalConsumer\",\"channels\":[\"ch3\"]}]",
                json);

    }

    @Test
    public void testMapperConvertList() {
        String json = "[{\"endpoint\":\"http://localhost:8080/api/v1/tc1/do\",\"channels\":[\"ch1\",\"ch2\"]},{\"endpoint\":\"http://localhost:8080/api/v1/tc2/do\",\"channels\":[\"ch2\"]},{\"endpoint\":\"LocalConsumer\",\"channels\":[\"ch3\"]}]";
        Mapper mapper = new Mapper();
        List<?> list = mapper.convert(json, List.class);
        assertEquals(3, list.size());
    }

    @Test
    public void testUnmarshalList() {
        String json = "[{\"endpoint\":\"http://localhost:8080/api/v1/tc1/do\",\"channels\":[\"ch1\",\"ch2\"]},{\"endpoint\":\"http://localhost:8080/api/v1/tc2/do\",\"channels\":[\"ch2\"]},{\"endpoint\":\"LocalConsumer\",\"channels\":[\"ch3\"]}]";
        List<?> list = JsonParser.unmarshal(json, List.class);
        assertEquals(3, list.size());

        //GenericType type = new GenericType<List<TestBean>>(){};
        List<TestBean> beans = JsonParser.unmarshals(json, TestBean.class);
        System.out.println();
        for (Object o : beans) {
            System.out.println(o.getClass().getName());
        }
    }

    static class TestBean {

        private String endpoint;
        private List<String> channels;
        private transient String observer;

        public List<String> getChannels() {
            return channels;
        }

        public void setChannels(List<String> channels) {
            this.channels = channels;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getObserver() {
            return observer;
        }

        public void setObserver(String o) {
            observer = o;
        }
    }
}
