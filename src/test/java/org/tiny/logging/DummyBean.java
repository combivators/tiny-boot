package org.tiny.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DummyBean {

    private static final Logger LOGGER = Logger.getLogger(DummyBean.class.getName());

    public void call() {
        LOGGER.finest("[FINEST] org Dummy Message");
        LOGGER.finer("[FINER] org Dummy Message");
        LOGGER.fine("[FINE] org Dummy Message");
        LOGGER.config("[CONFIG] org Dummy Message");
        LOGGER.info("[INFO] org Dummy Message");
        LOGGER.log(Level.INFO, "[INFO] org Dummy {0} is {1} in English", new Object[]{"Hoge", "Fuga"});
        LOGGER.warning("[WARN] org Dummy Message");
        LOGGER.severe("[ERROR] org Dummy Message");
    }
}
