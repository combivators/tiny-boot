package net.tiny.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * The only difference to the standard StreamHandler is
 * that a MAXLEVEL can be defined (which then is not published)
 *
 */
//TODO On Cloud Foundry(PaaS) Can't load log handler "net.tiny.logging.LevelableHandler" error!
//https://github.com/SAP/cf-java-logging-support
//TODO ClassNotFoundException: net.tiny.logging.LevelableHandler
//https://stackoverflow.com/questions/16501138/classnotfoundexception-for-java-util-logging-handler-in-maven-tomcat7run
public class LevelableHandler extends StreamHandler {

    private final StreamHandler stderrHandler;

    private final Level line = Level.INFO;  // by default, put out everything

    /** Constructor forwarding */
    public LevelableHandler() {
        super(System.out, new ConsoleFormatter());
        stderrHandler = new StreamHandler(System.err, new ConsoleFormatter());
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

}