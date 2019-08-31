package net.tiny.boot;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.LogManager;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.tiny.config.Config;

public class MainTest {

    @BeforeAll
    public static void beforeAll() throws Exception {
    LogManager.getLogManager()
        .readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
    //Comment out SLF4JBridgeHandler to show exception trace when tomcat start failed
    //Bridge the output of java.util.logging.Logger
//    org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger();
//    org.slf4j.bridge.SLF4JBridgeHandler.install();
//    LOGGER.log(Level.INFO, String.format("[REST] %s() SLF4J Bridge the output of JUL",
//            Bootstrap.class.getSimpleName()));
    }

    @BeforeEach
    public void setUp() throws Exception {
        ConsoleCapture.out.enable(true);
    }

    @AfterEach
    public void tearDown() throws Exception {
        ConsoleCapture.out.enable(false);
        ConsoleCapture.out.clear();
    }

    @Test
    public void testUsageHelp() throws Exception {
        String[] args = new String[] {"-h"};
        Main main = new Main(args);
        assertNotNull(main);
        assertNotNull(main);

        String out = ConsoleCapture.out.getHistory();
        assertTrue(ConsoleCapture.out.contains("Usage"));
        assertTrue(out.contains("Usage"));
    }

    @Test
    public void testNotFoundProfile() throws Exception {
        String[] args = new String[] {"-v", "-p", "dumy"};
        Main main = new Main(args);
        assertNotNull(main);


        String out = ConsoleCapture.out.getHistory();
        assertTrue(ConsoleCapture.out.contains("Usage"));
        assertTrue(out.contains("Usage"));
    }

    @Test
    public void testUnitProfile() throws Exception {
        String regex = "application-unit[.](properties|json|conf|yml)";
        assertTrue(Pattern.matches(regex, "application-unit.properties"));

        String[] args = new String[] {"-v", "-p", "unit"};

        ApplicationContext context = new Main(args).run();
        Thread.sleep(3500L);
        assertNull(context.getLastError());
    }

    @Test
    public void testOnlyOne() throws Exception {
        String[] args = new String[] {"-v", "-p", "unit"};
        One.main(args);
    }

    public static class One {
        private String name = "one1";

        public String getName() {
            return name;
        }

        public static void main(String[] args) throws Exception {
            One one = new One();
            one.exec();
        }

        public void exec() {
            System.out.println(String.format("Task1 '%s' start", name));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            System.out.println(String.format("Task1 '%s' end", name));
        }
    }

    public static class Two implements Runnable {
        private String name;
        private Object config;

        public String getName() {
            return name;
        }

        @Override
        public void run() {
            System.out.println(String.format("Task2 '%s' start", name));
            try {
                System.out.println(String.format("Task2 '%s' Configuration#%d", name, config.hashCode()));
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            System.out.println(String.format("Task2 '%s' end.", name));
        }
    }

    public static class Three extends Thread {
        @Override
        public void run() {
            System.out.println("Task3 'Three' start");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
            System.out.println("Task3 'Three' end.");
        }
    }

    @Test
    public void testBootWithConfiguration() throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        loader = getClass().getClassLoader();
        Enumeration<URL> urls = loader.getResources("application-four.properties");
        assertTrue(urls.hasMoreElements());
        String[] args = new String[] {"-v", "-p", "four"};
        Four.main(args);
    }

    @Config("app.four")
    public static class Four {
        private String name;

        public String getName() {
            return name;
        }

        public static void main(String[] args) throws Exception {
            ApplicationContext context = new Main(Four.class, args).run();
            Four four = context.getBootBean(Four.class);
            try {
            	four.exec();
            	//System.exit(0);
            } catch (Throwable e) {
            	e.printStackTrace();
            	//System.exit(1);
            }
        }

        public void exec() {
            System.out.println(String.format("Task1 '%s' start", name));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            System.out.println(String.format("Task1 '%s' end", name));
        }

        @Override
        public String toString() {
        	return String.format("Test-Four#%d - %s", hashCode(), String.valueOf(name));
        }

    }

    public static class DummyBoot {
        private String name;

        public String getName() {
            return name;
        }

        public static void main(String[] args) throws Exception {
            ApplicationContext context = new Main(DummyBoot.class, args).run();
            DummyBoot boot = context.getBootBean(DummyBoot.class);
            boot.exec();
        }

        public void exec() {
            System.out.println(String.format("Task1 '%1$s' start", name));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            System.out.println(String.format("Task1 '%1$s' end", name));
        }
    }

    public static class Hook implements Runnable {
        private String name;

        public String getName() {
            return name;
        }

        @Override
        public void run() {
            System.out.println(name + " shutdown start");
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
            }
            System.out.println(name + " shutdown end");
        }
    }

    public static class DummyCallback implements Consumer<Callable<Properties>> {
        ////////////////////////////////////////
        // Service consumer callback  method, will be called by main process.
        @Override
        public void accept(Callable<Properties> callable) {
            try {
                Properties services = callable.call();
                System.out.println("[DummyCallback] Called by main booter, Found " + services.size() + " service(s).");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class DummyThreadPool implements ExecutorService {
    	private ThreadPoolExecutor delgate;
        public ThreadPoolExecutor getDelgate() {
            if(delgate == null) {
                delgate = new ThreadPoolExecutor(2, 3, 1L,
                        TimeUnit.SECONDS,
                        new ArrayBlockingQueue<Runnable>(2),
                        new ThreadPoolExecutor.AbortPolicy());
                delgate.allowCoreThreadTimeOut(true);
            }
            return delgate;
        }
        @Override
        public void execute(Runnable command) {
            getDelgate().execute(command);
        }

        @Override
        public void shutdown() {
            getDelgate().shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return getDelgate().shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return getDelgate().isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return getDelgate().isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return getDelgate().awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return getDelgate().submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return getDelgate().submit(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return getDelgate().submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return getDelgate().invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            return getDelgate().invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return getDelgate().invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return getDelgate().invokeAny(tasks, timeout, unit);
        }

    }
}
