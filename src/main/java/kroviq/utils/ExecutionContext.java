package kroviq.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Holds execution-level metadata that is resolved once per run.
 * Populated before any reporting or test hooks fire.
 */
public final class ExecutionContext {
    private static final Logger logger = LogManager.getLogger(ExecutionContext.class);

    private static String executionUser;
    private static String machineName;
    private static String executionId;
    private static boolean initialized = false;

    private ExecutionContext() {}

    /**
     * Must be called exactly once, before report initialization.
     * Safe to call multiple times — subsequent calls are no-ops.
     */
    public static synchronized void initialize() {
        if (initialized) return;

        // Execution ID
        executionId = UUID.randomUUID().toString();

        // User: config override → OS username
        String configUser = LoadProperties.get("execution.user");
        executionUser = (configUser != null && !configUser.trim().isEmpty())
                ? configUser.trim()
                : System.getProperty("user.name", "unknown");

        // Machine: config override → hostname
        String configMachine = LoadProperties.get("execution.machine");
        if (configMachine != null && !configMachine.trim().isEmpty()) {
            machineName = configMachine.trim();
        } else {
            try {
                machineName = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                machineName = "unknown";
                logger.warn("Could not resolve hostname: {}", e.getMessage());
            }
        }

        initialized = true;
        logger.info("Execution started by: {} on {} [executionId={}]", executionUser, machineName, executionId);
    }

    public static String getExecutionUser() { return executionUser; }
    public static String getMachineName()   { return machineName; }
    public static String getExecutionId()   { return executionId; }
    public static boolean isInitialized()   { return initialized; }
}
