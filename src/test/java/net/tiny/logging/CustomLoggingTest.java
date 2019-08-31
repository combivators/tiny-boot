package net.tiny.logging;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.tiny.logging.DummyBean;

public class CustomLoggingTest {
    @Test
    public void testCustomLoggingLevel() throws Exception {
        LogManager.getLogManager().readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging-finest.properties"));
        Logger logger = Logger.getLogger(LoggingTest.class.getName());
        logger.finest("[FINEST] Message");
        logger.finer("[FINER] Message");
        logger.fine("[FINE] Message");
        logger.config("[CONFIG] Message");
        logger.info("[INFO] Message");
        logger.log(Level.INFO, "[INFO] {0} is {1} in English", new Object[]{"Hoge", "Fuga"});

        logger.warning("[WARN] Message");
        logger.severe("[ERROR] Message");

        DummyBean dummy = new DummyBean();
        dummy.call();
    }
}
