package net.tiny.config;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.tiny.logging.LevelableHandler;

public class Configuration implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    private static final String LIST_REGEX = "[ ]*,[ ]*";
    private static final String CLASS_KEY = "class";
    private static final String THIS_KEY = "${this}";

    /** Static method exclude filter */
    private static final Predicate<Method> IS_NOT_STATIC_METHOD = (method) -> !Modifier.isStatic(method.getModifiers());

    /** Public method include filter */
    private static final Predicate<Method> IS_PUBLIC_METHOD     = (method) -> Modifier.isPublic(method.getModifiers());

    /** Setter method include filter */
    private static final Predicate<Method> IS_SETTER_METHOD     = (method) -> method.getName().startsWith("set");

    private static Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    static abstract class VariablesReplacement {
        static final int    REFER_MAX = 10;
        static final String DOLLAR_BRACKET_START = "${";
        static final String DOLLAR_BRACKET_END = "}";

        public String replace(String value, boolean nameOnly) {
            StringBuffer buffer = new StringBuffer(value);
            int previousBegin = buffer.lastIndexOf(DOLLAR_BRACKET_START);
            int previousEnd = buffer.indexOf(DOLLAR_BRACKET_END, previousBegin);
            if(nameOnly && previousBegin == 0 && previousEnd == (buffer.length()-1)) {
                return buffer.substring(previousBegin + 2, previousEnd);
            }
            int count = 0;
            while (hasVariables(buffer.toString())) {
                previousBegin = buffer.lastIndexOf(DOLLAR_BRACKET_START);
                previousEnd = buffer.indexOf(DOLLAR_BRACKET_END, previousBegin);
                if(nameOnly && previousBegin == 0 && previousEnd == (buffer.length()-1)) {
                    return buffer.substring(previousBegin + 2, previousEnd);
                }
                String var = buffer.substring(previousBegin + 2, previousEnd);
                String replaced = replace(var);
                if (null != replaced) {
                    if(replaced.equals(DOLLAR_BRACKET_START + var + DOLLAR_BRACKET_END)) {
                        throw new RuntimeException(
                                String.format("The property value '%s' can not resue self name.", replaced));
                    }
                    buffer.replace(previousBegin, previousEnd + 1, replaced);
                    count++;
                    if(count > REFER_MAX) {
                        throw new RuntimeException(
                                String.format("Can not cycle reference '%s'.", replaced));
                    }
                } else {
                    return null;
                }
            }
            return buffer.toString();
        }

        public String replaceName(String value) {
            return replace(value, true);
        }

        public String replaceValue(String value) {
            return replace(value, false);
        }

        public static boolean hasVariables(String value) {
            if(value == null)
                return false;
            return value.contains(DOLLAR_BRACKET_START) && value.contains(DOLLAR_BRACKET_END);
        }

        abstract String replace(String var);
    }

    private final Converter converter;
    private final Properties properties;
    private final String parent;
    private final ContextHandler.Listener listener;

    public Configuration(Properties defaults, ContextHandler.Listener listener) {
        this(defaults, listener, false);
    }

    public Configuration(Properties defaults, ContextHandler.Listener listener, boolean logging) {
        this.parent = null;
        this.properties = defaults;
        this.listener = listener;
        this.converter = new Converter();
        if (logging) {
            // Initialization Java Logger Level
            final Configuration loggerConfig = getConfiguration("logging");
            if (null != loggerConfig) {
                setLoggingLevel(loggerConfig);
            }
        }
    }

    private Configuration(String parent, Properties defaults, Converter converter, ContextHandler.Listener listener) {
        this.parent = parent;
        this.properties = defaults;
        this.converter = converter;
        this.listener = listener;
    }

    public Set<String> getAllPropertyNames() {
        return this.properties.stringPropertyNames();
    }

    public Set<String> getPropertyNames(Predicate<String> predicate) {
        return getNameStream(predicate).collect(Collectors.toSet());
    }

    protected Stream<String> getNameStream(Predicate<String> predicate) {
        return this.properties.stringPropertyNames().stream().filter(predicate);
    }

    public boolean contains(String name) {
        return this.properties.containsKey(name);
    }

    public int size() {
        return this.properties.size();
    }

    private Stream<String> getNameStream() {
        return this.properties.stringPropertyNames().stream();
    }

    private String getProperty(String key) {
        return this.properties.getProperty(key);
    }

    private Object getObject(String key) {
        return this.properties.get(key);
    }

    private void setObject(String key, Object value) {
        if (listener != null) {
            if(value instanceof Configuration) {
                listener.cached(key, value, true);
            } else {
                listener.cached(key, value, false);
            }
        }
        this.properties.put(key, value);
    }

    public String getString(String key) {
        String value = getProperty(key);
        if(VariablesReplacement.hasVariables(value)) {
            return getReference(value);
        }
        return value;
    }

    protected <T> T getValue(Class<T> classType, String key) {
        return this.converter.convert(getString(key), classType);
    }

    public Integer getInteger(String key) {
        return getValue(Integer.class, key);
    }

    public Long getLong(String key) {
        return getValue(Long.class, key);
    }

    public Float getFloat(String key) {
        return getValue(Float.class, key);
    }

    public Double getDouble(String key) {
        return getValue(Double.class, key);
    }

    public Boolean getBoolean(String key) {
        return getValue(Boolean.class, key);
    }

    public Date getDate(String key) {
        return getValue(Date.class, key);
    }

    public LocalDate getLocalDate(String key) {
        return getValue(LocalDate.class, key);
    }

    public LocalTime getLocalTime(String key) {
        return getValue(LocalTime.class, key);
    }

    public LocalDateTime getLocalDateTime(String key) {
        return getValue(LocalDateTime.class, key);
    }

    public BigInteger getBigInteger(String key) {
        return getValue(BigInteger.class, key);
    }

    public BigDecimal getBigDecimal(String key) {
        return getValue(BigDecimal.class, key);
    }

    public Properties getProperties(String key) {
        String prefix = key + ".";
        // Generate a sub configuration by the key
        Properties prop = new Properties();
        int pos = prefix.length();
        getNameStream().filter(k -> k.startsWith(prefix))
                .forEach(name -> prop.setProperty(name.substring(pos), getString(name)));
        return prop;
    }

    public <T> T[] getValues(String key, Class<T> classType) {
        return this.converter.convertArray(getString(key), classType);
    }

    public <T> List<T> getValueList(String key, Class<T> classType) {
        return this.converter.convertList(getString(key), classType);
    }

    public <T> Set<T> getValueSet(String key, Class<T> classType) {
        return this.converter.convertSet(getString(key), classType);
    }

    public Configuration getConfiguration(String key) {
        return getConfiguration(key, String.class);
    }

    public Configuration getThis() {
        return this;
    }

    public <T> Configuration getConfiguration(String key, Class<T> beanClass) {
        String prefix = key + ".";
        if(contains(prefix)) {
            // Found from cache
            Object conf = getObject(prefix);
            if(conf instanceof Configuration) {
                return (Configuration)conf;
            }
        }
        // Generate a sub configuration by the key
        Configuration config = findConfiguration(prefix, beanClass);
        if(null != config) {
            // Cache config by the prefix key
            setObject(prefix, config);
        }
        return config;
    }

    private <T> Configuration findConfiguration(String key, Class<T> beanClass) {
        // Generate a sub configuration by the key
        Properties prop = new Properties();
        int pos = key.length();
        getNameStream().filter(k -> k.startsWith(key))
                .forEach(name -> pushValue(prop, pos, name, beanClass));
        if(!prop.isEmpty()) {
            return new Configuration(key, prop, this.converter, this.listener);
        }
        return null;
    }

    private <T> void pushValue(Properties prop, int pos, String name, Class<T> beanClass) {
        String key = name.substring(pos);
        String value = getProperty(name);
        prop.put(key, value);
    }

    Object getReference(String ref, Class<?> beanClass) {
        if (THIS_KEY.equals(ref)) {
            return getThis();
        } else if(VariablesReplacement.hasVariables(ref)) {
            ref = ref.substring(2, ref.length()-1);
        }
        return getAs(ref, beanClass);
    }

    Object getReference(String ref, Field field) {
        if (THIS_KEY.equals(ref)) {
            return getThis();
        }
        Class<?> beanClass = field.getType();
        boolean typed = Reflections.isJavaType(beanClass);
        if(typed) {
            return getReference(ref);
        }
        try {
            String value = new String(ref);
            // Cut head and tail of array string '[,,,]'
            if(value.charAt(0) == '[') {
                value = value.substring(1);
            }
            int len = value.length();
            if(value.charAt(len-1) == ']') {
                value = value.substring(0, len-1);
            }

            Collection<Object> beans;
            if(field.getType().equals(Set.class)) {
                beans = new HashSet<>();
            } else {
                beans = new ArrayList<>();
            }
            if (VariablesReplacement.hasVariables(value)) {
                String[] array = value.split(LIST_REGEX);
                VariablesReplacement replacement = new VariablesReplacement() {
                    @Override
                    String replace(String var) {
                        return properties.getProperty(var);
                    }
                };
                for(String var : array) {
                    String propertyName = replacement.replaceName(var);
                    if(contains(propertyName)) {
                        beans.add(getObject(propertyName));
                    } else {
                        // To find it
                        Object obj = getAs(propertyName, getMemberType(field));
                        if(null != obj) {
                            beans.add(obj);
                        } else {
                            LOGGER.log(Level.WARNING, String.format("Not found ${%s} (%2s)instance.", propertyName, beanClass.getSimpleName()));
                        }
                    }
                }
            }
            if(beans.isEmpty()) {
                return null;
            } else if(beanClass.isAssignableFrom(List.class) || beanClass.isAssignableFrom(Set.class)){
                return beans;
            } else {
                return Array.get(beans.toArray(), 0);
            }
        } catch(StackOverflowError ex) {
            throw new RuntimeException(
                    String.format("The property value '%1$s' can not resue self name.", ref));
        }
    }

    String getReference(String value) {
        try {
            return new VariablesReplacement() {
                @Override
                String replace(String var) {
                    return properties.getProperty(var);
                }
            }.replaceValue(value);
        } catch(StackOverflowError ex) {
            throw new RuntimeException(
                    String.format("The property value '%1$s' can not resue self name.", value));
        }
    }

    public <T> T getAs(Class<T> beanClass) {
        Config annotation = beanClass.getAnnotation(Config.class);
        if (annotation == null || annotation.value() == null) {
            throw new RuntimeException(
                    String.format("'%1$s' Class annotation '@%2$s' has not been granted, or not value has been set.",
                            beanClass.getName(), Config.class.getSimpleName()));
        }
        String key = annotation.value();
        return getAs(key, beanClass);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAs(String key, Class<T> beanClass) {
        if(contains(key)) {
            // Found from cache
            Object t = getObject(key);
            if(beanClass.isInstance(t)) {
                return (T)t;
            } else if ((t instanceof String) && VariablesReplacement.hasVariables(t.toString())){
                // Lookup reference object
                Field f = FieldBean.getField(beanClass);
                if (f != null)
                    return beanClass.cast(getReference(t.toString(), f)); // For list...
                else
                    return beanClass.cast(getReference(t.toString(), beanClass));
            }
        }
        T bean = null;
        Configuration config = getConfiguration(key, beanClass);
        if(null != config) {
            if(config.contains(CLASS_KEY)) {
                 bean = (T)getAsBean(config);
            } else {
                bean = getAsBean(beanClass, config);
            }
            // Cache bean by the value key
            if(null != bean) {
                setObject(key, bean);
            }
        }
        return bean;
    }

    private boolean hasPrefixKey(String key) {
        String prefix = key + ".";
        return getNameStream().anyMatch(k -> k.startsWith(prefix));
    }

    protected <T> T getAsBean(Class<T> beanClass, Configuration config) {
        try {
            T  bean;
            Class<?> implementClass = null;
            if(beanClass.isInterface() || Modifier.isAbstract( beanClass.getModifiers())) {
                // For implement class name
                String className = config.getString(CLASS_KEY);
                implementClass = Class.forName(className);
                bean = beanClass.cast(implementClass.newInstance());
            } else {
                bean = beanClass.newInstance();
                implementClass = beanClass;
            }
            bean = beanClass.cast( reweave(implementClass, bean, config));
            if (listener != null) {
                listener.created(bean, beanClass);
            }
            return bean;
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public <T> Object reweave(Class<T> beanClass, Object bean, Configuration config) {
        Stream<Field> fields = Reflections.getFieldStream(beanClass);
        Streams.Pair<Stream<Field>, Stream<Field>> pair = Streams.split(fields, field -> config.contains(field.getName()));
        // Set self field value.
        pair.getLeft()
            .forEach(field -> setFieldValue(bean, field, config));

        // Set self by setter method without fields.
        Stream<Method> methods = Reflections.getSetterStream(beanClass, IS_NOT_STATIC_METHOD.and(IS_PUBLIC_METHOD).and(IS_SETTER_METHOD));
        methods.forEach(method -> setPropertyValue(bean, method, config));

        // Set member class field value.
        pair.getRight()
            .filter(field -> config.hasPrefixKey(field.getName()))
            .forEach(field -> setFieldValue(bean, field, getAsBean(field.getType(), config.findConfiguration(field.getName() +".", field.getType()))));
        return bean;
    }

    protected Object getAsBean(Configuration config) {
        try {
            // For implement class name
            final String className = config.getString(CLASS_KEY);
            final Class<?> implementClass = Class.forName(className);
            final Object bean = implementClass.newInstance();
            Stream<Field> fields = Reflections.getFieldStream(implementClass);
            Streams.Pair<Stream<Field>, Stream<Field>> pair = Streams.split(fields, field -> config.contains(field.getName()));
            // Set self field value.
            pair.getLeft()
                .forEach(field -> setFieldValue(bean, field, config));

            // Set self by setter method without fields.
            Stream<Method> methods = Reflections.getSetterStream(implementClass, IS_NOT_STATIC_METHOD.and(IS_PUBLIC_METHOD).and(IS_SETTER_METHOD));
            methods.forEach(method -> setPropertyValue(bean, method, config));

            // Set member class field value.
            pair.getRight()
                .filter(field -> config.hasPrefixKey(field.getName()))
                .forEach(field -> setFieldValue(bean, field, getAsBean(field.getType(), config.findConfiguration(field.getName() +".", field.getType()))));
            if (listener != null) {
                listener.created(bean, implementClass);
            }
            return bean;
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void setFieldValue(Object bean, Field field, Configuration config)   {
        String data = config.getProperty(field.getName());
        Object value = null;
        if(VariablesReplacement.hasVariables(data)) {
            value = getReference(data, field);
        } else {
            value = this.converter.convert(data, field.getType());
        }
        if(null != value) {
            setFieldValue(bean, field, value);
        }
    }

    private void setFieldValue(Object bean, Field field, Object value)   {
        //First find setter method to set field value.
        Method setter =Reflections.getSetter(bean.getClass(), field);
        if (setter != null) {
            try {
                setter.invoke(bean, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                LOGGER.log(Level.SEVERE,
                        String.format("%s#%s('%s') failed - %s",
                                bean.getClass().getSimpleName(),
                                setter.getName(), String.valueOf(value), e.getMessage()), e);
                throw new RuntimeException(e.getMessage(), e);
            }
            return;
        }
        field.setAccessible(true);
        if(null != value) {
            try {
                field.set(bean, value);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                LOGGER.log(Level.SEVERE,
                        String.format("%s.%s = '%s' failed! %s",
                                bean.getClass().getSimpleName(),
                                field.getName(), String.valueOf(value), e.getMessage()), e);
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    private void setPropertyValue(Object bean, Method setter, Configuration config) {
        try {
            char c = Character.toLowerCase(setter.getName().charAt(3));
            String fieldName = new StringBuilder().append(c).append(setter.getName().substring(4)).toString();
            String value = config.getString(fieldName);
            if(null != value) {
                Object parameter = this.converter.convert(value, setter.getParameterTypes()[0]);
                setter.invoke(bean, parameter);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void setLoggingLevel(Configuration loggerConfig) {
        try {
            final Configuration levels = loggerConfig.getConfiguration("level");
            final Properties levelProp = (levels != null) ? levels.properties : new Properties();
            final Configuration handlers = loggerConfig.getConfiguration("handler");
            final Properties handlerProp = (handlers != null) ? handlers.properties : new Properties();
            LevelableHandler.setupLogger(levelProp, handlerProp);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, String.format("Java logger configuration error : %s.", e.getMessage()), e);
        }
    }

    protected void writeTo(PrintStream out) {
        out.println(String.format("Config#%1$d - %2$s",hashCode(), parent == null ? "" : parent));
        getNameStream().forEach(name -> out.println(String.format("#\t%1$s = %2$s", name, getString(name))));
    }

    public Set<String> findUnimplements() {
        Set<String> names = new HashSet<>();
        for (String name :  getAllPropertyNames()) {
            if (name.endsWith(".class")) {
                String key = name.substring(0, (name.length()-6));
                if (!contains(key)) { // Not found implemented value
                    names.add(key);
                }
            }
        }
        return names;
    }

    public void append(String key, String value) {
        this.properties.setProperty(key, value);
    }

    public void append(Properties appends) {
        this.properties.putAll(appends);
    }

    public Set<String> remains() {
        Set<String> names = new HashSet<>();
        Set<String> keys = findUnimplements();
        for (String key :  keys) {
            final String className = getProperty(key + ".class");
            try {
                getAs(key, Class.forName(className));
                names.add(key);
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING,
                        String.format("Remains '%s' failed. Not found '%s' class.", key, className), e);
            }
        }
        return names;
    }

    public void destroy() {
        properties.clear();
    }

    @Override
    protected void finalize() {
        destroy();
    }

    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        writeTo(out);
        out.close();
        return new String(baos.toByteArray());
    }

    static class FieldBean {
        private static Map<Class<?>, Field> fields;
        private List<Object> list;
        private Set<Object> values;
        private Object[] array;

        public List<Object> getList() {
            return list;
        }
        public Set<Object> getValues() {
            return values;
        }
        public Object[] getArray() {
            return array;
        }
        public static Field getField(Class<?> type) {
            if(null == fields) {
                try {
                    fields = new HashMap<>();
                    fields.put(List.class, FieldBean.class.getDeclaredField("list"));
                    fields.put(Set.class, FieldBean.class.getDeclaredField("values"));
                    fields.put(Object[].class, FieldBean.class.getDeclaredField("array"));
                } catch (NoSuchFieldException ex) {
                    throw new RuntimeException(ex.getMessage(), ex);
                }
            }
            return fields.get(type);
        }
    }
    static Class<?> getMemberType(Field field) {
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
}
