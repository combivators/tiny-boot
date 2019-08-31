package net.tiny.config;

import java.io.InputStream;

public interface ContextHandler {

    interface Listener {
        void created(Object bean, Class<?> beanClass);
        void parsed(String type, String resource, int size);
        void cached(String name, Object value, boolean config);
    }

    enum Type {
        HOCON, //Human-Optimized Config Object Notation
        JSON,
        PROPERTIES,
        YAML
    }

    Listener getListener();
    void setListener(Listener listener);

    String getResource();
    void setResource(String resource);
    Configuration getConfiguration();

    void parse();
    void parse(boolean logging);
    void parse(InputStream in, Type type);
    void parse(InputStream in, Type type, boolean logging);

}
