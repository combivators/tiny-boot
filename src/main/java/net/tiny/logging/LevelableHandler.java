package net.tiny.logging;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/**
 * The only difference to the standard StreamHandler is
 * that a MAXLEVEL can be defined (which then is not published)
 *
 */
public class LevelableHandler extends StreamHandler {

    private final StreamHandler stderrHandler;

    private final Level line = Level.INFO;  // by default, put out everything

    /** Constructor forwarding */
    public LevelableHandler() {
        super(System.out, new RecordFormatter());
        stderrHandler = new StreamHandler(System.err, new RecordFormatter());
        stderrHandler.setLevel(Level.WARNING);
    }

    /**
     * The only method we really change to check whether the message
     * is smaller than maxlevel.
     * We also flush here to make sure that the message is shown immediately.
     */
    @Override
    public synchronized void publish(LogRecord record) {
        if (record.getLevel().intValue() <= line.intValue()) {
            // if we arrived here, do what we always do
            super.publish(record);
            super.flush();
        } else {
            // if the level is above level line
            stderrHandler.publish(record);
            stderrHandler.flush();
        }
    }

    /**
     * Configuration logging
     * 根据应用配置文件，加载动态生成的 ‘logging.properties’，并重新配置日志的设定
     * <code>
     * logging.handler.output = file | console | none
     * logging.handler.level = INFO
     * logging.handler.pattern = %h/logging-test%u.log
     * logging.handler.formatter = net.tiny.logging.RecordFormatter
     * logging.level.all = ALL
     * logging.level.ch.qos = WARN
     * logging.level.sun.net = WARN
     * logging.level.net.tiny = DEBUG
     * </code>
     *
     * @param levels
     * @param handlers
     */
    public static void setupLogger(Properties levels, Properties handlers) {
        final StringBuffer buffer = new StringBuffer();
        Set<String> targets = levels.stringPropertyNames();
        String parameter;
        for (String t : targets) {
            parameter = levels.getProperty(t).toUpperCase();
            if ("DEBUG".equals(parameter) || "TRACE".equals(parameter)) {
                parameter = "FINE";
            } else
            if ("WARN".equals(parameter)) {
                parameter = "WARNING";
            } else
            if ("ERROR".equals(parameter)) {
                parameter = "SEVERE";
            }
            if ("all".equalsIgnoreCase(t)) {
                buffer.append(String.format(".level = %s\r\n", parameter));
            } else {
                buffer.append(String.format("%s.level = %s\r\n", t, parameter));
            }
        }

        String handler = null;
        String formatter = null;
        String level = "INFO";
        String out = handlers.getProperty("output");
        if ("file".equalsIgnoreCase(out)) {
            handler = FileHandler.class.getName();
        } else if ("console".equalsIgnoreCase(out)) {
            handler = ConsoleHandler.class.getName();
        }

        if (null != handler) {
            // Using java logging handler
            buffer.append(String.format("handlers = %s\r\n", handler));
        }

        targets = handlers.stringPropertyNames();
        for (String t : targets) {
            if ("output".equals(t)) continue;
            parameter = handlers.getProperty(t);
            if ("formatter".equals(t)) {
                formatter = parameter;
            }
            if ("pattern".equals(t)) {
                parameter = parameter.replaceAll("\"", "").replaceAll("'", "");
            }
            if ("level".equals(t)) {
                level = parameter;
            }
            if (null != handler) {
                buffer.append(String.format("%s.%s = %s\r\n", handler, t, parameter));
            }
        }
        if (null == formatter && null != handler) {
            buffer.append(String.format("%s.formatter = %s\r\n", handler, RecordFormatter.class.getName()));
        }

        try {
            // Because can't load a custom log handler from 'logging.properties'
            // Setting LevelableHandler into root logger.
            LevelableHandler levelable = null;
            if (null == handler) {
                // Use a custom logger handler
                levelable = new LevelableHandler();
                levelable.setLevel(Level.parse(level));
            }

            // Configuration logging properties
            final LogManager logManager = LogManager.getLogManager();
            logManager.reset();
            ByteArrayInputStream is = new ByteArrayInputStream(buffer.toString().getBytes());
            logManager.readConfiguration(is);
            is.close();

            if (null != levelable) {
                final Logger logger = getRootLogger(logManager);
                logger.addHandler(levelable);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static Logger getRootLogger(LogManager manager) {
        Logger root = manager.getLogger(Logger.GLOBAL_LOGGER_NAME);
        if (null == root) {
            root = manager.getLogger("");
        }
        final Logger parent = root.getParent();
        if (null != parent) {
            root = parent;
        }
        return root;
    }
}