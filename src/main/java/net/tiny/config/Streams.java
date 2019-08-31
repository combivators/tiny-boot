package net.tiny.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * {@link Stream} Utility
 *
 * @since 1.0.0
 */
public final class Streams {

    /**
     * A pair consisting of two elements.
     *
     * @param <L> the left element type
     * @param <R> the right element type
     *
     */
    public static class Pair<L, R> implements Map.Entry<L, R>, Comparable<Pair<L, R>>, Serializable {
        /** Serialization version */
        private static final long serialVersionUID = 1L;

        /** Left object */
        public final L left;
        /** Right object */
        public final R right;

        public Pair(final L left, final R right) {
            super();
            this.left = left;
            this.right = right;
        }

        public L getLeft() {
            return left;
        }

        public R getRight() {
            return right;
        }

        @Override
        public L getKey() {
            return getLeft();
        }

        @Override
        public R getValue() {
            return getRight();
        }

        @Override
        public R setValue(R value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int compareTo(Pair<L, R> other) {
            int c = getLeft().hashCode() - other.getLeft().hashCode();
            if(c == 0 ) {
                c = getRight().hashCode() - other.getRight().hashCode();
            }
            return c;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Map.Entry<?, ?>) {
                final Map.Entry<?, ?> other = (Map.Entry<?, ?>) obj;
                return getKey().equals(other.getKey())
                        && getValue().equals(other.getValue());
            }
            return false;
        }

        @Override
        public int hashCode() {
            // see Map.Entry API specification
            return (getKey() == null ? 0 : getKey().hashCode()) ^
                    (getValue() == null ? 0 : getValue().hashCode());
        }

        @Override
        public String toString() {
            return new StringBuilder().append('(').append(getLeft()).append(',').append(getRight()).append(')').toString();
        }

        public static <L, R> Pair<L, R> of(final L left, final R right) {
            return new Pair<L, R>(left, right);
        }
    }

    /**
     * A value with index.
     *
     * @param <T>  the value type
     */
    public static class IndexedValue<T> {

        private final long index;

        private final T value;

        public IndexedValue(long index, T value) {
            this.index = index;
            this.value = value;
        }

        public long getIndex() {
            return index;
        }

        public T getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(index, value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            IndexedValue<?> other = (IndexedValue<?>)obj;
            return (index == other.index && value.equals(other.value));
        }

        @Override
        public String toString() {
            return "IndexedValue [index=" + index + ", value=" + value + "]";
        }
    }

    /**
     * Two {@link Spliterator} zipper {@link Spliterator}。
     *
     * <p>
     * If different Spliterator size, It fits shorter one.
     * <p>
     *
     * @param <L> Left element type
     * @param <R> Right element type
     * @param <O> Output type
     */
    public static class Zipper<L, R, O> implements Spliterator<O> {

        private final Spliterator<? extends L> left;
        private final Spliterator<? extends R> right;
        private final BiFunction<L, R, O> combiner;

        Zipper(Spliterator<? extends L> left, Spliterator<? extends R> right,
                BiFunction<L, R, O> combiner) {
            this.left = left;
            this.right = right;
            this.combiner = combiner;
        }

        @Override
        public boolean tryAdvance(Consumer<? super O> action) {
            boolean[] hasNext = new boolean[2];
            hasNext[0] = left.tryAdvance(l -> hasNext[1] = right.tryAdvance(r -> action.accept(combiner.apply(l, r))));
            return hasNext[0] && hasNext[1];
        }

        @Override
        public Spliterator<O> trySplit() {
            return null; // parallel processing is not executed, don't divided
        }

        @Override
        public long estimateSize() {
            return Math.min(left.estimateSize(), right.estimateSize());
        }

        @Override
        public int characteristics() {
            // Distinct from logical AND of left and right Stream, Except Sorted
            return left.characteristics() & right.characteristics() & ~(DISTINCT | SORTED);
        }
    }

    /**
     * The spliterator that meet the specified conditions.
     *
     * @param <T> Element type
     */
    static class TakeWhileSpliterator<T> implements Spliterator<T> {

        /** Target Spliterator */
        private final Spliterator<? extends T> spliterator;
        /** Condition */
        private final Predicate<? super T> predicate;

        private boolean isSatisfied = true;

        TakeWhileSpliterator(Spliterator<? extends T> spliterator,
                Predicate<? super T> predicate) {
            this.spliterator = spliterator;
            this.predicate = predicate;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            return spliterator.tryAdvance(t -> {
                isSatisfied = predicate.test(t);
                if (isSatisfied) {
                    action.accept(t);
                }
            });
        }

        @Override
        public Spliterator<T> trySplit() {
            return null; // parallel processing is not executed, don't divided
        }

        @Override
        public long estimateSize() {
            return isSatisfied ? spliterator.estimateSize() : 0;
        }

        @Override
        public int characteristics() {
            return spliterator.characteristics() & ~SIZED;
        }

        @Override
        public Comparator<? super T> getComparator() {
            return null;
        }
    }

    public static class PredicatedSplitterConsumer<T> implements Consumer<T> {

        private Predicate<T> predicate;
        private Consumer<T> positiveConsumer;
        private Consumer<T> negativeConsumer;

        public PredicatedSplitterConsumer(Predicate<T> predicate,
                Consumer<T> positive, Consumer<T> negative) {
            this.predicate = predicate;
            this.positiveConsumer = positive;
            this.negativeConsumer = negative;
        }

        @Override
        public void accept(T t) {
            if(this.predicate.test(t)) {
                this.positiveConsumer.accept(t);
            } else {
                this.negativeConsumer.accept(t);
            }
        }
    }

    /**
     * Convert an stream to the indexed stream
     *
     * @param <T> Stream Type
     * @param stream Target Stream
     * @return Indexed Stream
     */
    public static <T> Stream<IndexedValue<T>> indexed(Stream<? extends T> stream) {
        Stream<Long> indexStream = LongStream.iterate(0, i -> i + 1).mapToObj(Long::valueOf);
        return StreamSupport.stream(
                new Zipper<>(indexStream.spliterator(), stream.spliterator(), IndexedValue::new), false);
    }

    /**
     * Zip two stream to one stream
     *
     * @param <L> Left stream type
     * @param <R> Right stream type
     * @param left Left stream
     * @param right Right stream
     * @return Zipped stream
     */
    public static <L, R> Stream<Pair<L, R>> zip(Stream<? extends L> left,  Stream<? extends R> right) {
        return StreamSupport.stream(
                new Zipper<>(left.spliterator(), right.spliterator(), Pair::of), false);
    }

    /**
     * Take stream of elements that meet the specified conditions.
     *
     * @param <T> Stream type
     * @param stream target stream
     * @param predicate Condition
     * @return Stream The stream has been token
     */
    public static <T> Stream<T> fit(Stream<? extends T> stream,  Predicate<? super T> predicate) {
        return StreamSupport.stream(
                new TakeWhileSpliterator<T>(stream.spliterator(), predicate), false);
    }

    public static <T> Pair<Stream<T>, Stream<T>> split(Stream<T> stream,  Predicate<T> predicate) {
        List<T> left = new ArrayList<>();
        List<T> right = new ArrayList<>();
        stream.forEach(new PredicatedSplitterConsumer<>(predicate,
                n -> left.add(n),
                n -> right.add(n)
                ));
        return new Pair<>(left.stream(), right.stream());
    }

    public static <T> void split(Stream<T> stream,  Predicate<T> predicate, Consumer<T> positive, Consumer<T> negative) {
        stream.forEach(new PredicatedSplitterConsumer<>(predicate,
                positive, negative));
    }
}
