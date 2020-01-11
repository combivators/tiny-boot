package net.tiny.config;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Reflection Utility
 *
 */
public final class Reflections {

    public static final Predicate<Field> ALWAYS_TRUE = (field) -> true;
    private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

    public static final Predicate<Method> IS_GETTER_METHOD = (method) -> (method.getParameterCount() == 0 && method.getReturnType() != null);
    public static final Predicate<Method> IS_SETTER_METHOD = (method) -> (method.getParameterCount() == 1);

    /** Static field exclude filter */
    public static final Predicate<Field> IS_NOT_STATIC_FIELD = (field) -> !Modifier.isStatic(field.getModifiers());

    /** Final field exclude filter */
    public static final Predicate<Field> IS_NOT_FINAL_FIELD = (field) -> !Modifier.isFinal(field.getModifiers());

    /** Volatile field exclude filter */
    public static final Predicate<Field> IS_NOT_VOLATILE_FIELD = (field) -> !Modifier.isVolatile(field.getModifiers());

    /** Transient field exclude filter */
    public static final Predicate<Field> IS_NOT_TRANSIENT_FIELD = (field) -> !Modifier.isTransient(field.getModifiers());


    public static boolean hasMethods(Class<?> classType) {
        Method[] methods = classType.getDeclaredMethods();
        return (null != methods && methods.length > 0);
    }

    public static boolean isAbstractClass(Class<?> type) {
        return !type.isInterface() && Modifier.isAbstract(type.getModifiers());
    }

    public static List<Class<?>> getInterfaces(Class<?> classType) {
        return getInterfaces(classType, false, false, false);
    }
    public static List<Class<?>> getInterfaces(Class<?> classType,
            boolean inner,
            boolean constants,
            boolean serializable) {
        List<Class<?>> list = new ArrayList<Class<?>>();
        setInterfaces(classType, list, inner, constants, serializable);
        List<Class<?>> superClasses = getSuperClasses(classType);
        for(Class<?> c: superClasses) {
            setInterfaces(c, list, inner, constants, serializable);
        }
        return list;
    }

    private static void setInterfaces(Class<?> classType, List<Class<?>> list,
            boolean inner,
            boolean constants,
            boolean serializable) {
        Class<?>[] classes = classType.getInterfaces();
        for(Class<?> c: classes) {
            if (!list.contains(c)) {
                //TODO inner constants
                //Skip Serializable class
                if (!(!serializable && Serializable.class.equals(c))) {
                    list.add(c);
                }
            }
        }
    }

    public static List<Class<?>> getSuperClasses(Class<?> targetClass) {
        return getSuperClasses(targetClass, false);
    }

    public static List<Class<?>> getSuperClasses(Class<?> targetClass, boolean object) {
        List<Class<?>> list = getSuperClasses(targetClass, new ArrayList<Class<?>>());
        if (!object && list.size() > 0) {
            //Remove java.lang.Object
            list.remove(list.size()-1);
        }
        return list;
    }

    private static List<Class<?>> getSuperClasses(Class<?> targetClass, List<Class<?>> list) {
        Class<?> superClass = targetClass.getSuperclass();
        if(null != superClass) {
            list.add(superClass);
            list = getSuperClasses(superClass, list);
        }
        return list;
    }

    public static boolean isInnerClass(Class<?> classType) {
        return classType.getName().contains("$");
    }

    public static boolean hasMainMethod(Class<?> classType) {
        try {
            // Find public static 'main(String[] args)' method
            Method method = classType.getMethod("main", String[].class);
            return Modifier.isStatic(method.getModifiers());
        } catch (NoSuchMethodException ex) {
            return false;
        }
    }

    public static Field getDeclaredField(Class<?> targetClass, String name) throws NoSuchFieldException {
        try {
            return targetClass.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = targetClass.getSuperclass();
            if(null != superClass) {
                return getDeclaredField(superClass, name);
            } else {
                throw e;
            }
        }
    }

    public static Class<?> getFieldGenericType(Field field) {
        Class<?> classType = null;
        Type type = field.getGenericType();
        if(type instanceof Class) {
             classType = field.getType();
        } else if(type instanceof ParameterizedType) {
            //Is List(Set) type
            ParameterizedType listType = (ParameterizedType) field.getGenericType();
            Type rawType = listType.getActualTypeArguments()[0];
            if(rawType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType)rawType;
                classType = (Class<?>) parameterizedType.getRawType();
            } else {
                classType = (Class<?>) listType.getActualTypeArguments()[0];
            }
        }
        return classType;
    }

    public static Field[] getAllFields(Class<?> targetClass) {
        return getFieldStream(targetClass, ALWAYS_TRUE).toArray(Field[]::new);
    }

    public static Stream<Field> getFieldStream(Class<?> targetClass) {
        return getFieldStream(targetClass, IS_NOT_STATIC_FIELD.and(IS_NOT_FINAL_FIELD).and(IS_NOT_TRANSIENT_FIELD));
    }

    public static Stream<Field> getFieldStream(Class<?> targetClass, Predicate<Field> filter) {
        Stream<Field> thisFields = Arrays.stream(targetClass.getDeclaredFields()).filter(filter);
        Class<?> superClass = targetClass.getSuperclass();
        if (superClass == null || superClass.equals(Object.class)) {
            return thisFields;
        }
        return Stream.concat(thisFields, getFieldStream(superClass, filter));
    }


    public static Optional<Method> getGetter(Class<?> targetClass, String name) {
        return getSetterStream(targetClass, IS_GETTER_METHOD,
                IS_NOT_STATIC_FIELD.and(IS_NOT_FINAL_FIELD).and(IS_NOT_TRANSIENT_FIELD)
                    .and(f -> f.getName().equals(name)))
                .findAny();
    }

    public static Method getSetter(Class<?> targetClass, Field field) {
        return getSetterStream(targetClass, IS_SETTER_METHOD,
                IS_NOT_STATIC_FIELD.and(IS_NOT_FINAL_FIELD).and(IS_NOT_TRANSIENT_FIELD)
                    .and(f -> f.equals(field)))
                .findAny()
                .orElse(null);
    }

    public static Optional<Method> getSetter(Class<?> targetClass, String name) {
        return getSetterStream(targetClass, IS_SETTER_METHOD,
                IS_NOT_STATIC_FIELD.and(IS_NOT_FINAL_FIELD).and(IS_NOT_TRANSIENT_FIELD)
                    .and(f -> f.getName().equals(name)))
                .findAny();
    }

    public static Stream<Method> getSetterStream(Class<?> targetClass) {
        return getSetterStream(targetClass, IS_SETTER_METHOD, true);
    }

    public static Stream<Method> getSetterStream(Class<?> targetClass, Predicate<Method> filter) {
        return getSetterStream(targetClass, filter, false);
    }

    public static Stream<Method> getSetterStream(Class<?> targetClass, Predicate<Method> setter, boolean all) {
        if(all) {
            return getSetterStream(targetClass, setter, null);
        } else {
            return getSetterStream(targetClass, setter, IS_NOT_STATIC_FIELD.and(IS_NOT_FINAL_FIELD).and(IS_NOT_TRANSIENT_FIELD));
        }
    }

    public static Stream<Method> getSetterStream(Class<?> targetClass, Predicate<Method> setter, Predicate<Field> filter) {
        Predicate<Method> addon = null;
        if(null != filter) {
            Stream<Field> thisFields = getFieldStream(targetClass, filter);
            List<String> names = new ArrayList<>();
            thisFields.forEach(field -> {
                names.add(String.format("set%C%s",  field.getName().charAt(0), field.getName().substring(1)));
                names.add(field.getName());
            });
            addon = (method) -> names.contains(method.getName());
            if (setter != null) {
                setter = setter.and(addon);
            }
        }
        if (setter == null) {
            setter = IS_SETTER_METHOD;
        }

        Stream<Method> thisMethods = Arrays.stream(targetClass.getDeclaredMethods()).filter(setter);
        Class<?> superClass = targetClass.getSuperclass();
        if (superClass == null || superClass.equals(Object.class)) {
            return thisMethods;
        }
        return Stream.concat(thisMethods, getSetterStream(superClass, setter, filter));
    }

    public static Annotation[] expandAnnotation(Annotation annotation) {
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            if ("value".equals(method.getName())) {
                return getAnnotationArray(annotation, method);
            }
        }
        return EMPTY_ANNOTATION_ARRAY;
    }

    public static boolean isNumberType(Class<?> type) {
        if(type.isAssignableFrom(byte.class)
            || type.isAssignableFrom(int.class)
            || type.isAssignableFrom(short.class)
            || type.isAssignableFrom(long.class)
            || type.isAssignableFrom(float.class)
            || type.isAssignableFrom(double.class)
            || type.isAssignableFrom(Byte.class)
            || type.isAssignableFrom(Integer.class)
            || type.isAssignableFrom(Short.class)
            || type.isAssignableFrom(Long.class)
            || type.isAssignableFrom(Float.class)
            || type.isAssignableFrom(Double.class)
            || type.isAssignableFrom(BigDecimal.class)
            || type.isAssignableFrom(BigInteger.class)
            ) {
            return true;
        }
        return false;
    }

    public static boolean isDateType(Class<?> type) {
        if(type.isAssignableFrom(LocalDate.class)
            || type.isAssignableFrom(LocalTime.class)
            || type.isAssignableFrom(LocalDateTime.class)
            || type.isAssignableFrom(Date.class)
            || type.isAssignableFrom(Timestamp.class)
            ) {
             return true;
        }
        return false;
    }

    public static boolean isJavaType(Class<?> type) {
        if(type.isAssignableFrom(String.class)
          || isNumberType(type)
          || type.isAssignableFrom(char.class)
          || type.isAssignableFrom(boolean.class)
          || type.isAssignableFrom(Character.class)
          || type.isAssignableFrom(Boolean.class)
          || type.isEnum()
          || isDateType(type)
          ) {
           return true;
       }
       return false;
    }

    public static boolean isJavaArrayType(Class<?> type) {
        if(type.isArray()
          || type.isAssignableFrom(String[].class)
          || type.isAssignableFrom(char[].class)
          || type.isAssignableFrom(int[].class)
          || type.isAssignableFrom(short[].class)
          || type.isAssignableFrom(long[].class)
          || type.isAssignableFrom(float[].class)
          || type.isAssignableFrom(double[].class)
          || type.isAssignableFrom(boolean[].class)
          || type.isAssignableFrom(Character[].class)
          || type.isAssignableFrom(Integer[].class)
          || type.isAssignableFrom(Short[].class)
          || type.isAssignableFrom(Long[].class)
          || type.isAssignableFrom(Float[].class)
          || type.isAssignableFrom(Double[].class)
          || type.isAssignableFrom(Boolean[].class)
          || type.isAssignableFrom(BigDecimal[].class)
          || type.isAssignableFrom(BigInteger[].class)
          || type.isAssignableFrom(LocalDate[].class)
          || type.isAssignableFrom(LocalTime[].class)
          || type.isAssignableFrom(LocalDateTime[].class)
          || type.isAssignableFrom(Date[].class)
          || type.isAssignableFrom(Timestamp[].class)
          ) {
           return true;
       }
       return false;
    }

    public static boolean isCollectionType(Class<?> type) {
        if(isAssignable(type, Collection.class)) {
            return true;
        }
        return false;
    }

    public static boolean isAssignable(Class<?> targetType, Class<?> interfaceType) {
        if(null == targetType || !interfaceType.isInterface()) {
            return false;
        }

        for(Class<?> type : getInterfaces(targetType, true, true, true)) {
            if(interfaceType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCollectionField(Field field) {
        return isCollectionType(field.getType()) && !isJavaType(getFieldGenericType(field)) ;
    }

    private static Annotation[] getAnnotationArray(Annotation annotation, Method method) {
        if (!isRepeatableAnnotation(method.getReturnType(), annotation.annotationType())) {
            return EMPTY_ANNOTATION_ARRAY;
        }
        try {
            return (Annotation[])method.invoke(annotation);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
             throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static boolean isRepeatableAnnotation(Class<?> targetClass, Class<? extends Annotation> annotationType) {
        if (!targetClass.isArray()) {
            return false;
        }
        Class<?> type = targetClass.getComponentType();
        if (!type.isAnnotation()) {
            return false;
        }
        Repeatable repeatable = type.getDeclaredAnnotation(Repeatable.class);
        if (repeatable == null) {
            return false;
        }
        return repeatable.value().equals(annotationType);
    }

}
