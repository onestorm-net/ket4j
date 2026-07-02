package net.onestorm.ket4j;

import java.util.concurrent.atomic.AtomicReference;

public final class ErrorTrackerProvider {

    private static final AtomicReference<ErrorTracker> INSTANCE = new AtomicReference<>();

    private ErrorTrackerProvider() {
    }

    public static void initialize(ErrorTrackerConfiguration config) {
        ErrorTracker tracker = new ErrorTracker(config);
        if (!INSTANCE.compareAndSet(null, tracker)) {
            throw new IllegalStateException("ErrorTrackerProvider is already initialized");
        }
    }

    public static ErrorTracker getInstance() {
        ErrorTracker tracker = INSTANCE.get();
        if (tracker == null) {
            throw new IllegalStateException("ErrorTrackerProvider is not initialized");
        }
        return tracker;
    }
}
