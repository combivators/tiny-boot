package net.tiny.config;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PropertiesSupport implements Supplier<Properties> {

    public static interface Listener {
        void property(String method, String key, String value);
    }

    private final Properties properties;
    private Listener listener;

    public PropertiesSupport(Properties p, Listener l) {
        properties =  p;
        listener = l;
    }
    public PropertiesSupport(Listener l) {
        this(new Properties(), l);
    }
    public PropertiesSupport() {
        this(new Properties(), null);
    }

    public void put(String key, Object value) {
        properties.put(key, value);
        if (listener != null) {
            listener.property("put", key, String.valueOf(value));
        }
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        if (listener != null) {
            listener.property("setProperty", key, value);
        }
    }

    public void putAll(Map<?,?> all) {
        properties.putAll(all);
        if (listener != null) {
            listener.property("putAll", ""+ all.size(), null);
        }
    }

    public void load(Reader reader) throws IOException {
        try {
            properties.load(reader);
        } catch (IOException e) {
            throw e;
        } finally {
            if (listener != null) {
                listener.property("load", "reader" + reader.hashCode(), null);
            }
        }
    }

    @Override
    public Properties get() {
        return properties;
    }

    public Listener getListener() {
        return listener;
    }
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public static class Monitor implements Listener {
        private static Logger LOGGER = Logger.getLogger(Listener.class.getName());
        Level level = Level.INFO;
        @Override
        public void property(String method, String key, String value) {
            if (null != value) {
                LOGGER.log(level, String.format("[BOOT] Properties.%s('%s','%s')", method, key, value));
            } else {
                LOGGER.log(level, String.format("[BOOT] Properties.%s('%s')", method, key));
            }
        }
    }
}
