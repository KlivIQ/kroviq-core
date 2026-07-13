package kroviq.reporting.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportFileManager {
    private static final Logger logger = LogManager.getLogger(ReportFileManager.class);
    
    public static boolean createDirectory(String directoryPath) {
        try {
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (created) {
                    logger.debug("Created directory: {}", directoryPath);
                    return true;
                } else {
                    logger.warn("Failed to create directory: {}", directoryPath);
                    return false;
                }
            }
            return true; // Directory already exists
        } catch (Exception e) {
            logger.error("Error creating directory {}: {}", directoryPath, e.getMessage());
            return false;
        }
    }
    
    public static boolean deleteFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    logger.debug("Deleted file: {}", filePath);
                } else {
                    logger.warn("Failed to delete file: {}", filePath);
                }
                return deleted;
            }
            return true; // File doesn't exist
        } catch (Exception e) {
            logger.error("Error deleting file {}: {}", filePath, e.getMessage());
            return false;
        }
    }
    
    public static boolean fileExists(String filePath) {
        return new File(filePath).exists();
    }
    
    public static long getFileSize(String filePath) {
        File file = new File(filePath);
        return file.exists() ? file.length() : 0;
    }
    
    public static boolean isFileReadable(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.canRead();
    }
    
    public static boolean isDirectoryWritable(String directoryPath) {
        File directory = new File(directoryPath);
        return directory.exists() && directory.canWrite();
    }
    
    public static String generateTimestampedFilename(String baseName, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return baseName + "_" + timestamp + "." + extension;
    }
    
    public static void cleanupOldFiles(String directoryPath, int daysOld) {
        try {
            File directory = new File(directoryPath);
            if (!directory.exists() || !directory.isDirectory()) {
                return;
            }
            
            long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);
            
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.lastModified() < cutoffTime) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            logger.info("Cleaned up old file: {}", file.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error cleaning up old files in {}: {}", directoryPath, e.getMessage());
        }
    }
    
    public static String getAbsolutePath(String relativePath) {
        return new File(relativePath).getAbsolutePath();
    }
    
    public static void ensureDirectoryExists(String directoryPath) {
        if (!createDirectory(directoryPath)) {
            throw new RuntimeException("Failed to create required directory: " + directoryPath);
        }
    }
}