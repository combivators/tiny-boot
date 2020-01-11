package com.tiny.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DummyLogger {
    private static final Logger LOGGER = Logger.getLogger(DummyLogger.class.getName());

    public void call() {
        LOGGER.finest("[FINEST] Tiny Dummy Logger");
        LOGGER.finer("[FINER] Tiny Dummy Logger");
        LOGGER.fine("[FINE] Tiny Dummy Logger");
        LOGGER.config("[CONFIG] Tiny Dummy Logger");
        LOGGER.info("[INFO] Tiny Dummy Logger");
        LOGGER.log(Level.INFO, "[INFO] org Dummy {0} is {1} in English", new Object[]{"Hoge", "Fuga"});
        LOGGER.warning("[WARN] Tiny Dummy Logger");
        LOGGER.severe("[ERROR] Tiny Dummy Logger");
    }
}
