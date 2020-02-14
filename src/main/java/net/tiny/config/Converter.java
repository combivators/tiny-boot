package net.tiny.config;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Converter implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    static final String LIST_REGEX = "[ ]*,[ ]*";

    @FunctionalInterface
    public static interface StringValueConverter<T> {
        T convert(String value);
    }

    public static final StringValueConverter<Boolean> BOOLEAN = value -> {
        if("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value)) {
            return true;
        } else {
            return false;
        }
    };

    static final StringValueConverter<String> STRING = String::valueOf;
    static final StringValueConverter<Integer> INTEGER = Integer::valueOf;
    static final StringValueConverter<Short> SHORT = Short::valueOf;
    static final StringValueConverter<Long> LONG = Long::valueOf;
    static final StringValueConverter<Float> FLOAT = Float::valueOf;
    static final StringValueConverter<Double> DOUBLE = Double::valueOf;
    static final StringValueConverter<BigInteger> BIG_INTEGER = BigInteger::new;
    static final StringValueConverter<BigDecimal> BIG_DECIMAL = BigDecimal::new;

    private static final String LOCAL_DATE_FORMATTER = "yyyy/MM/dd";


    static final StringValueConverter<Date> DATE = value -> {
        try {
            return  new SimpleDateFormat(LOCAL_DATE_FORMATTER).parse(value);
        } catch (ParseException ex) {
            throw new RuntimeException("Convert '" + value + "' error :" + ex.getMessage(), ex);
        }
    };

    static final StringValueConverter<LocalDate> LOCAL_DATE = value -> {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern(LOCAL_DATE_FORMATTER));
        } catch (DateTimeParseException e) {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        }
    };

    private static final String LOCAL_DATETIME_FORMATTER = "yyyy/MM/dd HH:mm:ss";


    static final StringValueConverter<LocalDateTime> LOCAL_DATETIME = value -> {
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(LOCAL_DATETIME_FORMATTER));
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    };

    static final StringValueConverter<LocalTime> LOCAL_TIME = LocalTime::parse;


    static final SimpleDateFormat GMT_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        GMT_FORMATTER.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    static final StringValueConverter<java.sql.Timestamp> TIMESTAMP = value -> {
        try {
            long t = Long.parseLong(value);
            return new java.sql.Timestamp(t);
        } catch (NumberFormatException e) {
            try {
                return new java.sql.Timestamp(GMT_FORMATTER.parse(value).getTime());
            } catch (ParseException e1) {
                return new java.sql.Timestamp(System.currentTimeMillis());
            }
        }
    };

    static final StringValueConverter<Level> LOGGER_LEVEL = Level::parse;
    static final StringValueConverter<File> FILE = value -> {
        return  new File(value);
    };
    static final StringValueConverter<Path> PATH = value -> {
        return  Paths.get(value);
    };

    private static final String SEPARATOR = "_";

    static final StringValueConverter<Locale> LOCALE = value -> {
        Locale locale = Locale.getDefault();
        String[] values = value.split(SEPARATOR);
        switch(values.length) {
        case 0:
        default :
            break;
        case 1:
            locale = new Locale(values[0], "", "");
            break;
        case 2:
            locale = new Locale(values[0], values[1], "");
            break;
        case 3:
            locale = new Locale(values[0], values[1], values[2]);
            break;
        }
        return locale;
    };

    static <E extends Enum<E>> StringValueConverter<E> toEnum(Class<E> type) {
        return v -> Enum.valueOf(type, v);
    }

    static final char[] LEFT_LIST_PREFIX  = new char[] {'{','(','[','<'};
    static final char[] RIGHT_LIST_SUFFIX = new char[] {'}',')',']','>'};
    static final int prefix(char c) {
        for (int i=0; i<LEFT_LIST_PREFIX.length; i++) {
            if (c == LEFT_LIST_PREFIX[i])
                return i;
        }
        return -1;
    }

    static final List<String> merageValues(String[] values, int index) {
        if(index == -1)
            return Arrays.asList(values);
        List<String> list = new ArrayList<>();
        char suffix = RIGHT_LIST_SUFFIX[index];
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<values.length; i++) {
            String v = values[i];
            if (v.charAt(v.length()-1) == suffix) { //matching last char
                sb.append(v);
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(v);
            }
        }
        return list;

    }

    static final StringValueConverter<List<String>> LIST = value -> {
        /*
        if (value.charAt(0) == '[' && value.charAt(value.length()-1) == ']') {
            value = value.substring(1, value.length()-1);
        }
        */
        String[] values = value.split(LIST_REGEX);
        int last = values.length - 1;
        for(int i=0; i<values.length; i++) {
            String v = values[i].trim();
            if(v.isEmpty()) continue;
            if(i==0 && v.charAt(0) == '[') {
                v = v.substring(1);
            }
            if(i==last && v.charAt(v.length()-1) == ']') {
                v = v.substring(0, v.length()-1);
            }
            if(v.charAt(0) == '"' && v.charAt(v.length()-1) == '"') {
                v = v.substring(1, v.length()-1);
            }
            values[i] = v;
        }
        int ln = -1;
        if (values.length > 2) {
            ln = prefix(values[0].charAt(0));
        }

        return merageValues(values, ln);
        //return Arrays.asList(values);
    };

    static final StringValueConverter<Set<String>> SET = value -> {
        return new LinkedHashSet<>(LIST.convert(value));
    };

    static final StringValueConverter<String[]> STRING_ARRAY = value -> {
        List<String> values = LIST.convert(value);
        return values.toArray(new String[values.size()]);
    };

    static final StringValueConverter<int[]> INTEGER_ARRAY = value -> {
        String[] array = STRING_ARRAY.convert(value);
        int[] values = new int[array.length];
        for(int i=0; i<values.length; i++) {
            values[i] = INTEGER.convert(cutDecimal(array[i]));
        }
        return values;
    };

    static final StringValueConverter<long[]> LONG_ARRAY = value -> {
        String[] array = STRING_ARRAY.convert(value);
        long[] values = new long[array.length];
        for(int i=0; i<values.length; i++) {
            values[i] = LONG.convert(cutDecimal(array[i]));
        }
        return values;
    };

    static final StringValueConverter<float[]> FLOAT_ARRAY = value -> {
        String[] array = STRING_ARRAY.convert(value);
        float[] values = new float[array.length];
        for(int i=0; i<values.length; i++) {
            values[i] = FLOAT.convert(array[i]);
        }
        return values;
    };

    static final StringValueConverter<double[]> DOUBLE_ARRAY = value -> {
        String[] array = STRING_ARRAY.convert(value);
        double[] values = new double[array.length];
        for(int i=0; i<values.length; i++) {
            values[i] = DOUBLE.convert(array[i]);
        }
        return values;
    };

    static final StringValueConverter<boolean[]> BOOLEAN_ARRAY = value -> {
        String[] array = STRING_ARRAY.convert(value);
        boolean[] values = new boolean[array.length];
        for(int i=0; i<values.length; i++) {
            values[i] = BOOLEAN.convert(array[i]);
        }
        return values;
    };

    private static String cutDecimal(String value) {
        int dot = value.indexOf('.');
        if (dot > 0) {
            return value.substring(0, dot);
        }
        return value;
    }

    static ConcurrentHashMap<Class<?>, StringValueConverter<?>> converters = new ConcurrentHashMap<>(40);
    static {
        converters.put(String.class, STRING);
        converters.put(String[].class, STRING_ARRAY);

        converters.put(boolean.class, BOOLEAN);
        converters.put(int.class, INTEGER);
        converters.put(short.class, SHORT);
        converters.put(long.class, LONG);
        converters.put(float.class, FLOAT);
        converters.put(double.class, DOUBLE);

        converters.put(Boolean.class, BOOLEAN);
        converters.put(Integer.class, INTEGER);
        converters.put(Short.class, SHORT);
        converters.put(Long.class, LONG);
        converters.put(Float.class, FLOAT);
        converters.put(Double.class, DOUBLE);

        converters.put(BigInteger.class, BIG_INTEGER);
        converters.put(BigDecimal.class, BIG_DECIMAL);
        converters.put(Date.class, DATE);
        converters.put(LocalDate.class, LOCAL_DATE);
        converters.put(LocalDateTime.class, LOCAL_DATETIME);
        converters.put(LocalTime.class, LOCAL_TIME);
        converters.put(java.sql.Timestamp.class, TIMESTAMP);

        converters.put(Level.class, LOGGER_LEVEL);
        converters.put(File.class, FILE);
        converters.put(Path.class, PATH);
        converters.put(List.class, LIST);
        converters.put(Set.class, SET);

        converters.put(int[].class, INTEGER_ARRAY);
        converters.put(long[].class, LONG_ARRAY);
        converters.put(float[].class, FLOAT_ARRAY);
        converters.put(double[].class, DOUBLE_ARRAY);
        converters.put(boolean[].class, BOOLEAN_ARRAY);
        converters.put(Integer[].class, INTEGER_ARRAY);
        converters.put(Long[].class, LONG_ARRAY);
        converters.put(Float[].class, FLOAT_ARRAY);
        converters.put(Double[].class, DOUBLE_ARRAY);
        converters.put(Boolean[].class, BOOLEAN_ARRAY);

        converters.put(Locale.class, LOCALE);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> T convert(String value, Class<T> classType) {

        if (classType.isEnum()) {
            return (T)classType.cast(Enum.valueOf((Class<? extends Enum>)classType, value));
        }
        if (classType.isArray() && classType.getComponentType().isEnum()) {
            Class<? extends Enum> enumType = (Class<? extends Enum>)classType.getComponentType();
            String[] array = STRING_ARRAY.convert(value);
            Enum[] enums = (Enum[])Array.newInstance(enumType, array.length);
            for(int i=0; i<array.length; i++) {
                enums[i] = enumType.cast(Enum.valueOf((Class<? extends Enum>)enumType, array[i]));
            }
            return (T)enums;
        }
        StringValueConverter<T> converter = (StringValueConverter<T>)converters.get(classType);
        if (null == converter && classType.isArray()) {
            Class<?> rawType = classType.getComponentType();
            StringValueConverter c = converters.get(rawType);
            if(null == c) {
                throw new RuntimeException("Not found " + rawType.getName() + " converter.");
            }
            String[] array = STRING_ARRAY.convert(value);
            Object[] values = (Object[])Array.newInstance(rawType, array.length);
            for(int i=0; i<array.length; i++) {
                values[i] = c.convert(array[i]);
            }
            return (T)values;
        }
        if(null == converter) {
            throw new RuntimeException("Not found " + classType.getName() + " converter.");
        }
        return converter.convert(value);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> convertList(String value, Class<T> classType) {
         StringValueConverter<T> converter = (StringValueConverter<T>)converters.get(classType);
        if(null == converter) {
            throw new RuntimeException("Not found " + classType.getName() + " converter.");
        }
        return LIST.convert(value).stream()
                .map(converter::convert)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T> Set<T> convertSet(String value, Class<T> classType) {
         StringValueConverter<T> converter = (StringValueConverter<T>)converters.get(classType);
        if(null == converter) {
            throw new RuntimeException("Not found " + classType.getName() + " converter.");
        }
        return LIST.convert(value).stream()
                .map(converter::convert)
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    public <T> T[] convertArray(String value, Class<T> classType) {
        List<T> list = convertList(value, classType);
        // Return a object array
        Object array = Array.newInstance(classType, list.size());
        for(int i=0; i<list.size(); i++) {
            Array.set(array, i, list.get(i));
        }
        return (T[])array;
    }

    @SuppressWarnings("unchecked")
    public <T> StringValueConverter<T> getConverter(Class<T> classType) {
        return (StringValueConverter<T>)converters.get(classType);
    }

    public Number convertNumber(Number value, Class<?> type) {
        if (value instanceof Double) {
            Double number = (Double)value;
            if(int.class.equals(type) || Integer.class.equals(type)) {
                return number.intValue();
            } else if(long.class.equals(type) || Long.class.equals(type)) {
                return number.longValue();
            } else if(float.class.equals(type) || Float.class.equals(type)) {
                return number.floatValue();
            } else if(double.class.equals(type) || Double.class.equals(type)) {
                return number;
            } else if(short.class.equals(type) || Short.class.equals(type)) {
                return number.shortValue();
            } else if(byte.class.equals(type) || Byte.class.equals(type)) {
                return number.byteValue();
            } else if(BigDecimal.class.equals(type)) {
                return new BigDecimal(number);
            } else if(BigInteger.class.equals(type)) {
                return new BigDecimal(number).unscaledValue();
            } else {
                return number;
            }
        } else {
            return value;
        }
    }
}
