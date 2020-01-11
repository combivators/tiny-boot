package net.tiny.logging;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

public class LevelableHandlerTest {

    /*
      java.util.logging.FileHandler Pattern Overview
     '/'  Local path name separator
     '%'  t system temporary directory
     '%h' user.home system property value
     '%g' Generation number identifying log rotation
     '%u' Unique number to resolve duplicates
     '%%' Convert to single percent sign%
    */
    @Test
    public void testSetup() throws Exception {

        Properties levels = new Properties();
        levels.setProperty("all", "ALL");
        levels.setProperty("net.tiny", "FINE");
        levels.setProperty("sun.net", "WARNING");
        levels.setProperty("org.tiny", "INFO");
        levels.setProperty("com.tiny", "OFF");

        Properties handlers = new Properties();
        //handlers.setProperty("output", "file");
        //handlers.setProperty("pattern", "%h/java%u.log");
        //handlers.setProperty("limit", "50000");
        //handlers.setProperty("count", "1");
        //handlers.setProperty("output", "console");
        //handlers.setProperty("level", "ALL");
        handlers.setProperty("level", "INFO");
        //handlers.setProperty("formatter", "java.util.logging.SimpleFormatter");
        //handlers.setProperty("formatter", "net.tiny.logging.RecordFormatter");
        LevelableHandler.setupLogger(levels, handlers);

        final Logger logger = Logger.getLogger(LoggingTest.class.getName());
        logger.finest("[FINEST] Invisible message");
        logger.finer("[FINER] Invisible message");
        logger.fine("[FINE] Visible message");
        logger.config("[CONFIG] Visible message");
        logger.info("[INFO] Visible message");
        logger.warning("[WARN] Visible message");
        logger.severe("[ERROR] Visible message");
        org.tiny.logging.DummyBean dummy = new org.tiny.logging.DummyBean();
        dummy.call();

        com.tiny.logging.DummyLogger off = new com.tiny.logging.DummyLogger();
        off.call();

        LogManager.getLogManager().reset();
    }


    @Test
    public void testSetupLocalFileHandler() throws Exception {
        Path logfile = Paths.get(System.getProperty("user.home") + System.getProperty("file.separator") + "logging-test0.log");
        if (Files.exists(logfile)) {
            Files.delete(logfile);
        }
        Properties levels = new Properties();
        levels.setProperty("net.tiny", "FINE");
        levels.setProperty("sun.net", "WARNING");
        levels.setProperty("org.tiny", "INFO");
        levels.setProperty("com.tiny", "OFF");

        Properties handlers = new Properties();
        handlers.setProperty("output", "file");
        handlers.setProperty("pattern", "%h/logging-test%u.log");
        handlers.setProperty("limit", "50000");
        handlers.setProperty("count", "1");
        handlers.setProperty("level", "ALL");
        handlers.setProperty("formatter", "net.tiny.logging.RecordFormatter");
        LevelableHandler.setupLogger(levels, handlers);

        final Logger logger = Logger.getLogger(LoggingTest.class.getName());
        logger.finest("[FINEST] Invisible message");
        logger.finer("[FINER] Invisible message");
        logger.fine("[FINE] Visible message");
        logger.config("[CONFIG] Visible message");
        logger.info("[INFO] Visible message");
        logger.warning("[WARN] Visible message");
        logger.severe("[ERROR] Visible message");
        org.tiny.logging.DummyBean dummy = new org.tiny.logging.DummyBean();
        dummy.call();

        com.tiny.logging.DummyLogger off = new com.tiny.logging.DummyLogger();
        off.call();

        LogManager.getLogManager().reset();
        if (Files.exists(logfile)) {
            Files.delete(logfile);
        }

    }
}
