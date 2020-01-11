package net.tiny.logging;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;


public class LoggingTest {

    @Test
    public void testLoggingFormat() throws Exception {
        //Bridge the output of java.util.logging.Logger
//        org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger();
//        org.slf4j.bridge.SLF4JBridgeHandler.install();
//        LOGGER.info(String.format("[JUNIT5] %s-%d SLF4J Bridge the output of JUL", LoggingTest.class.getSimpleName(), hashCode()));
        LogManager.getLogManager().readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
        Logger logger = Logger.getLogger(LoggingTest.class.getName());
        logger.finest("[FINEST] Message");
        logger.finer("[FINER] Message");
        logger.fine("[FINE] Message");
        logger.config("[CONFIG] Message");
        logger.info("[INFO] Message");
        logger.log(Level.INFO, "[INFO] {0} is {1} in English", new Object[]{"Hoge", "Fuga"});

        logger.warning("[WARN] Message");
        logger.severe("[ERROR] Message");
        Exception ex = new Exception("Message");
        logger.log(Level.SEVERE, "[ERROR] " + ex.getMessage(), ex);
    }


}
