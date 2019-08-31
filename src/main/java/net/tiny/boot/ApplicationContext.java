package net.tiny.boot;

import java.util.List;
import java.util.concurrent.Future;

/**
 * How to get ApplicationContext on an service class :
 *
 * <code>
 * RuntimeMXBean mxRuntime = ManagementFactory.getRuntimeMXBean();
 * String name = mxRuntime.getName();
 * Integer processId = Integer.valueOf(name.substring(0, name.indexOf('@')));
 * ApplicationContext context = (ApplicationContext)System.getProperties().get(processId);
 * </code>
 *
 */
public interface ApplicationContext {
	/*
    interface Listener {
        void trace(String msg);
        void info(String msg);
        void warn(String msg);
        void warn(String msg, Throwable exception);
        void error(String msg);
        void error(String msg, Throwable exception);
    }
    */

    Integer getProcessId();
    String getProfile();

    Throwable getLastError();

    List<Object> getBootBeans();
    <T> List<T> getBootBeans(Class<T> type);
    <T> T getBootBean(Class<T> type);
    <T> T getBean(String key, Class<T> type);
    Future<Integer> getFuture();
}
