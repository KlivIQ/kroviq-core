package kroviq.wrapper.desktop.session;

import kroviq.wrapper.desktop.core.DesktopEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DesktopSessionManager {

    private static final Logger logger = LogManager.getLogger(DesktopSessionManager.class);
    private static final ThreadLocal<DesktopEngine> activeEngine = new ThreadLocal<>();
    private static final ThreadLocal<DesktopSessionConfig> activeConfig = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> sessionActive = ThreadLocal.withInitial(() -> false);

    public static void startSession(DesktopEngine engine, DesktopSessionConfig config) {
        if (sessionActive.get() && config.isReuseSession()) {
            logger.info("Reusing existing desktop session");
            return;
        }
        engine.launchApplication(config);
        activeEngine.set(engine);
        activeConfig.set(config);
        sessionActive.set(true);
        logger.info("Desktop session started: {}", config.getApplicationPath());
    }

    public static void endSession() {
        DesktopEngine engine = activeEngine.get();
        if (engine != null && sessionActive.get()) {
            try {
                engine.closeApplication();
                logger.info("Desktop session closed");
            } catch (Exception e) {
                logger.warn("Error closing desktop session: {}", e.getMessage());
            }
        }
        activeEngine.remove();
        activeConfig.remove();
        sessionActive.set(false);
    }

    public static DesktopEngine getActiveEngine() {
        return activeEngine.get();
    }

    public static boolean isSessionActive() {
        return sessionActive.get();
    }

    public static void clearSession() {
        activeEngine.remove();
        activeConfig.remove();
        sessionActive.set(false);
    }
}
