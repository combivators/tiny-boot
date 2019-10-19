package net.tiny.config;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class Mapper {

    private static final Logger LOGGER  = Logger.getLogger(Mapper.class.getName());
    Converter converter = new Converter();

    @SuppressWarnings("unchecked")
    public <T> T convert(final Map<String, ?> map, final Class<T> type) throws InstantiationException, IllegalAccessException {
        T target = type.newInstance();
        Set<String> names = map.keySet();
        for(String name : names) {
            try {
                Field field = Reflections.getDeclaredField(type, name);
                Object value = map.get(name);
                if (value instanceof Map) {
                    //Recursion call set member properties
                    value = convert((Map<String, ?>)value, field.getType());
                } else
                if(value instanceof Collection && Reflections.isCollectionField(field)) {
                    // Collection<?> type value can be converted
                    value = convert((Collection<Object>)value, Reflections.getFieldGenericType(field));
                } else {
                    if(Reflections.isNumberType(value.getClass()) && Reflections.isNumberType(field.getType())) {
                        value = converter.convertNumber((Number)value, field.getType());
                    } else {
                        if( Reflections.isJavaArrayType(field.getType()) &&
                            Reflections.isNumberType(field.getType().getComponentType()) &&
                            !field.getType().getComponentType().isPrimitive()) {
                            // double[] type convert to Double[] type
                            value = convertArray((Collection<Object>)value, Reflections.getFieldGenericType(field));
                        } else {
                            // Collection<String> or String[] type value can be converted
                            value = convert(value.toString(), field.getType());
                        }
                    }
                }
                setFieldValue(target, field, value);
            } catch (NoSuchFieldException e) {
                // Ignore
                LOGGER.warning(String.format("No such field '%s.%s'", type.getSimpleName(), name));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                // Ignore
                LOGGER.warning(String.format("Set field '%s.%s' error : %s ", type.getSimpleName(), name, e.getMessage()));

            }
        }
        return target;
    }

    @SuppressWarnings({ "unchecked"})
    public <T> T[] convertArray(final Collection<Object> values, final Class<T> type) throws InstantiationException, IllegalAccessException {
        T[] array = null;
        int i = 0;
        for(Object value : values) {
            if (array == null) {
                T t = (T)value;
                array =  (T[]) Array.newInstance(t.getClass(), values.size());
                array[i++] = t;
            } else {
                array[i++] = (T)value;
            }
        }
        return array;
    }



    @SuppressWarnings("unchecked")
    public <T> Collection<T> convert(final Collection<Object> values, final Class<T> type) throws InstantiationException, IllegalAccessException {
        Collection<T> collection = null;
        if(values instanceof Set) {
            collection = new LinkedHashSet<>();
        } else {
            collection = new ArrayList<>();
        }
        for(Object value : values) {
            T target = null;
            if(value instanceof Map) {
                target = convert((Map<String,?>)value, type);
            } else {
                target = convert(value.toString(), type);
            }
            collection.add(target);
        }
        return collection;
    }



    public <T> T convert(String value, Class<T> type) {
        if(value == null || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return converter.convert(value, type);
    }

    private static void setFieldValue(Object obj, Field field, Object value) throws IllegalArgumentException, IllegalAccessException {
        field.setAccessible(true);
        field.set(obj, value);
        field.setAccessible(false);
    }

}
