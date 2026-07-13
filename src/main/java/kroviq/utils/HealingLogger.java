package kroviq.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import kroviq.utils.ActionType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HealingLogger {
    
    private static final Logger logger = LogManager.getLogger(HealingLogger.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private static final ConcurrentHashMap<String, AtomicInteger> healingAttempts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicInteger> healingSuccesses = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> successfulAlternates = new ConcurrentHashMap<>();
    
    private static boolean healingLoggingEnabled = true;
    
    public static void logHealingAttempt(String primaryLocator, String alternateLocator, boolean success, ActionType actionType) {
        if (!healingLoggingEnabled) return;
        
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String key = generateKey(primaryLocator, actionType);
        
        healingAttempts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        
        if (success) {
            healingSuccesses.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
            successfulAlternates.put(key, alternateLocator);
            
            logger.info("HEALING_SUCCESS [{}] Primary: {} | Alternate: {} | ActionType: {}", 
                timestamp, truncateLocator(primaryLocator), truncateLocator(alternateLocator), actionType);
        } else {
            logger.debug("HEALING_ATTEMPT [{}] Primary: {} | Alternate: {} | ActionType: {} | Result: FAILED", 
                timestamp, truncateLocator(primaryLocator), truncateLocator(alternateLocator), actionType);
        }
    }
    
    public static void logHealingFailure(String primaryLocator, ActionType actionType, int alternateCount) {
        if (!healingLoggingEnabled) return;
        
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        logger.warn("HEALING_FAILED [{}] Primary: {} | ActionType: {} | Alternates tried: {}", 
            timestamp, truncateLocator(primaryLocator), actionType, alternateCount);
    }
    
    public static void logHealingDisabled(String primaryLocator, ActionType actionType, String reason) {
        if (!healingLoggingEnabled) return;
        
        logger.debug("HEALING_DISABLED Primary: {} | ActionType: {} | Reason: {}", 
            truncateLocator(primaryLocator), actionType, reason);
    }
    
    public static String generateHealingReport() {
        if (healingAttempts.isEmpty()) {
            return "No healing attempts recorded.";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("\n=== AUTO-HEALING SUMMARY REPORT ===\n");
        report.append("Generated: ").append(LocalDateTime.now().format(TIMESTAMP_FORMAT)).append("\n\n");
        
        int totalAttempts = healingAttempts.values().stream().mapToInt(AtomicInteger::get).sum();
        int totalSuccesses = healingSuccesses.values().stream().mapToInt(AtomicInteger::get).sum();
        double successRate = totalAttempts > 0 ? (double) totalSuccesses / totalAttempts * 100 : 0;
        
        report.append("Overall Statistics:\n");
        report.append("- Total Healing Attempts: ").append(totalAttempts).append("\n");
        report.append("- Total Healing Successes: ").append(totalSuccesses).append("\n");
        report.append("- Success Rate: ").append(String.format("%.1f%%", successRate)).append("\n\n");
        
        report.append("Top Successful Alternates:\n");
        successfulAlternates.entrySet().stream()
            .limit(10)
            .forEach(entry -> {
                String key = entry.getKey();
                String alternate = entry.getValue();
                int successes = healingSuccesses.getOrDefault(key, new AtomicInteger(0)).get();
                report.append("- ").append(successes).append("x: ").append(truncateLocator(alternate)).append("\n");
            });
        
        report.append("\n=== END REPORT ===\n");
        return report.toString();
    }
    
    public static void resetStatistics() {
        healingAttempts.clear();
        healingSuccesses.clear();
        successfulAlternates.clear();
        logger.info("Healing statistics reset");
    }
    
    public static void setHealingLoggingEnabled(boolean enabled) {
        healingLoggingEnabled = enabled;
        logger.info("Healing logging {}", enabled ? "enabled" : "disabled");
    }
    
    public static int getHealingAttempts(String primaryLocator, ActionType actionType) {
        String key = generateKey(primaryLocator, actionType);
        return healingAttempts.getOrDefault(key, new AtomicInteger(0)).get();
    }
    
    public static int getHealingSuccesses(String primaryLocator, ActionType actionType) {
        String key = generateKey(primaryLocator, actionType);
        return healingSuccesses.getOrDefault(key, new AtomicInteger(0)).get();
    }
    
    private static String generateKey(String primaryLocator, ActionType actionType) {
        return actionType + ":" + primaryLocator.hashCode();
    }
    
    private static String truncateLocator(String locator) {
        if (locator == null) return "null";
        return locator.length() > 80 ? locator.substring(0, 77) + "..." : locator;
    }
}