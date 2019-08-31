package net.tiny.boot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ShutdownManager implements Runnable {

    private static final ShutdownManager instance = new ShutdownManager();
    private ShutdownManager() {}
    private List<Runnable> listeners = new ArrayList<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(instance));
    }

    public static ShutdownManager getInstance() {
        return instance;
    }

    public void addListeners(Runnable... listeners) {
        this.listeners.addAll(Arrays.asList(listeners));
    }
    public void addListener(Runnable listener) {
        this.listeners.add(listener);
    }

    @Override
    public void run() {
        this.listeners.forEach(Runnable::run);
    }
}
