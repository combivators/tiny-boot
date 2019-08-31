package net.tiny.config;

import static java.util.Spliterator.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamsTest {

    @Test
    public void testIndexedValue() throws Exception {
        Streams.IndexedValue<String> target = new Streams.IndexedValue<>(999, "test");
        assertEquals(999L, target.getIndex());
        assertEquals("test", target.getValue());

        Streams.IndexedValue<String> target1 = new Streams.IndexedValue<>(1, "test");
        Streams.IndexedValue<String> target2 = new Streams.IndexedValue<>(1, "test");
        Streams.IndexedValue<String> target3 = new Streams.IndexedValue<>(2, "test");
        Streams.IndexedValue<String> target4 = new Streams.IndexedValue<>(1, "any");
        assertTrue(target1.hashCode() == target2.hashCode());
        assertFalse(target1.hashCode() == target3.hashCode());
        assertFalse(target1.hashCode() == target4.hashCode());

        assertTrue(target1.equals(target2));
        assertFalse(target1.equals(target3));
        assertFalse(target1.equals(target4));
    }

    @Test
    public void testIndexed() throws Exception {
        List<Streams.IndexedValue<String>> expected =
                Arrays.asList(
                        new Streams.IndexedValue<>(0, "foo"),
                        new Streams.IndexedValue<>(1, "bar"),
                        new Streams.IndexedValue<>(2, "test"));
        Stream<Streams.IndexedValue<String>> stream = Streams.indexed(Stream.of("foo", "bar", "test"));
        assertEquals(expected, stream.collect(Collectors.toList()));
    }

    @Test
    public void testZip() throws Exception {
        Stream<Integer> left  = Stream.empty();
        Stream<Integer> right = Stream.empty();
        List<Object> expected = Collections.emptyList();
        assertEquals(expected, Streams.zip(left, right).collect(Collectors.toList()));

        left  = Stream.of(1, 2, 3);
        right = Stream.of(4, 5, 6);
        expected = Arrays.asList(Streams.Pair.of(1, 4), Streams.Pair.of(2, 5), Streams.Pair.of(3, 6));
        assertEquals(expected, Streams.zip(left, right).collect(Collectors.toList()));

        left  = Stream.of(1, 2, 3);
        right = Stream.of(10, 11, 12, 13);
        expected = Arrays.asList(Streams.Pair.of(1, 10), Streams.Pair.of(2, 11), Streams.Pair.of(3, 12));
        assertEquals(expected, Streams.zip(left, right).collect(Collectors.toList()));

        left  = Stream.of(1, 2, 3, 4, 5);
        right = Stream.of(10, 11);
        expected = Arrays.asList(Streams.Pair.of(1, 10), Streams.Pair.of(2, 11));
        assertEquals(expected, Streams.zip(left, right).collect(Collectors.toList()));

        left  = Stream.of(1, 2, 3);
        right =  Stream.empty();
        expected = Collections.emptyList();
        assertEquals(expected, Streams.zip(left, right).collect(Collectors.toList()));

        left  = Stream.empty();
        right =  Stream.of(4, 5, 6);
        expected = Collections.emptyList();
        assertEquals(expected, Streams.zip(left, right).collect(Collectors.toList()));
     }

    @Test
    public void testZipper() throws Exception {
        Spliterator<?> left  = Stream.empty().spliterator();
        Spliterator<?> right = Stream.empty().spliterator();
        Streams.Zipper<Object, Object, Object> target =
                new Streams.Zipper<>(left, right, (l, r) -> l);
        assertEquals(0, target.estimateSize());

        left  = Stream.of("foo", "bar", "test").spliterator();
        right = Stream.of("foo").spliterator();
        target = new Streams.Zipper<>(left, right, (l, r) -> l);
        assertEquals(1, target.estimateSize());

        left  = Stream.of("foo").spliterator();
        right = Stream.of("foo", "bar", "test").spliterator();
        target = new Streams.Zipper<>(left, right, (l, r) -> l);
        assertEquals(1, target.estimateSize());

        target = new Streams.Zipper<>(Stream.empty().spliterator(),
                        Stream.empty().spliterator(), (l, r) -> l);
        assertNull(target.trySplit());


        left =
                Spliterators.spliterator(Collections.emptyList(), CONCURRENT | DISTINCT | SORTED);
        right =
                Spliterators.spliterator(Collections.emptyList(), CONCURRENT | SORTED | ORDERED);
        Streams.Zipper<Object, Object, Streams.Pair<Object, Object>> zipper = new Streams.Zipper<>(left, right, (l, r) -> Streams.Pair.of(l, r));
        assertEquals(0, zipper.characteristics() & DISTINCT);
        assertEquals(0, zipper.characteristics() & SORTED);
        assertNotEquals(0, zipper.characteristics() & CONCURRENT);
        assertEquals(0, zipper.characteristics() & ORDERED);
        assertEquals(0, zipper.characteristics() & IMMUTABLE);
    }


    @Test
    public void testFit() throws Exception {
        Stream<Integer> in = Stream.empty();
        Predicate<Integer> predicate = predicate(i -> true);
        List<Integer> expected = Collections.emptyList();
        assertEquals(expected, Streams.fit(in, predicate).collect(Collectors.toList()));

        in = Stream.of(1, 2, 3, 4, 5);
        predicate = predicate(i -> i <= 5);
        expected = Arrays.asList(1, 2, 3, 4, 5);
        assertEquals(expected, Streams.fit(in, predicate).collect(Collectors.toList()));

        in = Stream.of(1, 2, 3, 4, 5);
        predicate = predicate(i -> i <= 4);
        expected = Arrays.asList(1, 2, 3, 4);
        assertEquals(expected, Streams.fit(in, predicate).collect(Collectors.toList()));

        in = Stream.of(1, 2, 3, 4, 5);
        predicate = predicate(i -> i <= 1);
        expected = Arrays.asList(1);
        assertEquals(expected, Streams.fit(in, predicate).collect(Collectors.toList()));

        in = Stream.of(1, 2, 3, 4, 5);
        predicate = predicate(i -> i < 1);
        expected = Collections.emptyList();
        assertEquals(expected, Streams.fit(in, predicate).collect(Collectors.toList()));
    }

    @Test
    public void testPredicatedSplitterConsumer() throws Exception {
        Stream<Integer> in = Stream.of(1, 2, 3, 4, 5);
        Predicate<Integer> predicate = predicate(i -> i <= 3);

        List<Integer> left = new ArrayList<>();
        List<Integer> right = new ArrayList<>();
        in.forEach(new Streams.PredicatedSplitterConsumer<>(predicate,
                n -> left.add(n),
                n -> right.add(n)
                ));
        List<Integer> expected = Arrays.asList(1, 2, 3);
        assertEquals(expected, left);
        expected = Arrays.asList(4, 5);
        assertEquals(expected, right);
    }

    @Test
    public void testSplit() throws Exception {
        Stream<Integer> in = Stream.of(1, 2, 3, 4, 5);
        Predicate<Integer> predicate = predicate(i -> i <= 3);
        Streams.Pair<Stream<Integer>, Stream<Integer>> pair = Streams.split(in, predicate);
        List<Integer> expected = Arrays.asList(1, 2, 3);
        assertEquals(expected, pair.getLeft().collect(Collectors.toList()));
        expected = Arrays.asList(4, 5);
        assertEquals(expected, pair.getRight().collect(Collectors.toList()));
    }

    private static Predicate<Integer> predicate(Predicate<Integer> p) {
        return p;
    }

    /** Static field exclude filter */
    private static final Predicate<Field> IS_NOT_STATIC_FIELD = (field) -> !Modifier.isStatic(field.getModifiers());

    /** Final field exclude filter */
    private static final Predicate<Field> IS_NOT_FINAL_FIELD = (field) -> !Modifier.isFinal(field.getModifiers());

    /** Static method exclude filter */
    private static final Predicate<Method> IS_NOT_STATIC_METHOD = (method) -> !Modifier.isStatic(method.getModifiers());

    /** Public method include filter */
    private static final Predicate<Method> IS_PUBLIC_METHOD     = (method) -> Modifier.isPublic(method.getModifiers());

    /** Setter method include filter */
    private static final Predicate<Method> IS_SETTER_METHOD     = (method) -> method.getName().startsWith("set");

    /** Setter method include filter */
    private static final Predicate<Method> IS_GETTER_METHOD     = (method) -> (method.getName().startsWith("get") || method.getName().startsWith("is") || method.getName().startsWith("has"));

    @Test
    public void testGetFieldSetterStream() throws Exception {
        Stream<Field> fields = Reflections.getFieldStream(TestBean.class, IS_NOT_STATIC_FIELD.and(IS_NOT_FINAL_FIELD));
        fields.forEach(s -> System.out.println(s));
        System.out.println();
        Stream<Method> mehods = Reflections.getSetterStream(TestBean.class, IS_NOT_STATIC_METHOD.and(IS_PUBLIC_METHOD).and(IS_SETTER_METHOD), IS_NOT_STATIC_FIELD.and(IS_NOT_FINAL_FIELD));
        mehods.forEach(s -> System.out.println(s));
    }

    static class TestSupperBean {
        private static String version;
        private String name;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }

        public static void setVersion(String ver) {
            version = ver;
        }
    }
    static class TestBean extends TestSupperBean {
        private String value;
        public String getValue() {
            return value;
        }
        public void setValue(String value) {
            this.value = value;
        }

        public void setData(String data) {
        }
    }
}
