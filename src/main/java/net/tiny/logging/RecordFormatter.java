package net.tiny.logging;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class RecordFormatter extends Formatter {

    private static final int MAX_REC_SIZE = 128;
    private static final int MAX_ORG_SIZE = 30; //40

    private int limit = MAX_ORG_SIZE;
    @Override
    public synchronized String format(final LogRecord record) {
        //2019-04-04 11:49:47.242	DEBUG	net.tiny.rest.test.LoggingTest#testLoggingFormat	[FINEST] Message
        //2019-04-04T11:41:24.576	INFO	cls:n.t.s.controller.HelloWorldResource     	[REST] HelloWorldResource-1874879719
        final StringBuffer message = new StringBuffer(MAX_REC_SIZE);
        long millis = record.getMillis();
        String time = String.format("%tF %<tT.%<tL", millis);
        message.append(time);
        //message.append(' ');
        //message.append(record.getThreadID());
        message.append('\t');
        message.append(level(record.getLevel()));
        message.append("\t");
        String className = record.getSourceClassName();
        if(className == null) {
            className = record.getLoggerName();
        }

        message.append(shortClassName(limit, className));

        message.append('.');
        String methodName = record.getSourceMethodName();
        message.append(methodName != null ? methodName : "-");
        message.append(" [" + record.getThreadID() + "]");
        //message.append("(" + record.getSequenceNumber() + ")"); //TODO BUG on jar

        message.append('\t');
        message.append(formatMessage(record));
        message.append('\n');
        Throwable throwable = record.getThrown();
        if (throwable != null) {
            message.append(throwable.toString());
            message.append('\n');
            for (StackTraceElement trace : throwable.getStackTrace()) {
                message.append('\t');
                message.append(trace.toString());
                message.append('\n');
            }
        }
        return message.toString();
    }

    String getrReference(final LogRecord record) {
        String className = record.getSourceClassName();
        int pos = className.lastIndexOf(".");
        if(pos != -1) {
            className = className.substring(pos+1);
        }
        return String.format("(%s.java:%d)", className, record.getSequenceNumber());
    }

    String shortClassName(final int maxLength, final String className) {
        final String[] array = className.split("[.]");
        int size = className.length();
        int i = 0;
        while (size > maxLength) {
            size -= array[i].length() - 1;
            array[i] = array[i].substring(0, 1);
            i++;
        }
        final StringBuffer buffer = new StringBuffer();
        for(String e : array) {
            if(buffer.length() > 0) {
                buffer.append('.');
            }
            buffer.append(e);
        }
        return buffer.toString();
    }

    private String level(Level level) {
        String mark = level.toString();
        switch(mark) {
        case "FINEST":
        case "FINER":
        case "FINE":
        case "TRACE":
            mark = "DEBUG";
            break;
        case "CONFIG":
            mark = "CONF";
            break;
        case "INFO":
            mark = "INFO";
            break;
        case "WARNING":
            mark = "WARN";
            break;
        case "SEVERE":
            mark = "ERROR";
            break;
        case "OFF":
            mark = "----";
            break;
        case "ALL":
            mark = "****";
            break;
        default:
            break;
        }
        return mark;
    }
}
