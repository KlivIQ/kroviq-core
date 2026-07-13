package kroviq.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class LoadProperties {
    private static final Logger logger = LogManager.getLogger(LoadProperties.class);

    // Defaults (files are in main/resources)
    private static final String DEFAULT_CONFIG_FS = "src/main/resources/Config.properties";
    private static final String DEFAULT_GLOBAL_FS = "src/main/resources/GlobalVariable.properties";
    private static final String DEFAULT_CONFIG_CP = "Config.properties";
    private static final String DEFAULT_GLOBAL_CP = "GlobalVariable.properties";

    // Holders
    private static final Properties CONFIG = new Properties();
    private static final Properties GLOBAL = new Properties();
    // Merged view = CONFIG -> GLOBAL -> System.getProperties() (later overrides earlier)
    private static final Properties MERGED = new Properties();

    static {
        // Load order: config first (base), then global (override), then system props (final override)
        loadInto(CONFIG,   DEFAULT_CONFIG_FS, DEFAULT_CONFIG_CP, "config");
        loadInto(GLOBAL,   DEFAULT_GLOBAL_FS, DEFAULT_GLOBAL_CP, "global");
        rebuildMerged();
    }

    // ---------- Public API ----------

    /** Get from merged (GlobalVariable overrides config; system properties override both). */
    public static String get(String key) {
        return MERGED.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return MERGED.getProperty(key, defaultValue);
    }

    /** Get explicitly from config.properties only. */
    public static String getFromConfig(String key) {
        return CONFIG.getProperty(key);
    }

    /** Get explicitly from GlobalVariable.properties only. */
    public static String getFromGlobal(String key) {
        return GLOBAL.getProperty(key);
    }

    // Typed getters (handy)
    public static int getInt(String key, int def) {
        String v = get(key);
        try { return v == null ? def : Integer.parseInt(v.trim()); }
        catch (NumberFormatException e) { return def; }
    }

    public static boolean getBool(String key, boolean def) {
        String v = get(key);
        return v == null ? def : v.trim().equalsIgnoreCase("true");
    }

    /** Reload both files (optionally with custom paths). Null = keep defaults. */
    public static synchronized void reload(String configPath, String globalPath) {
        logger.info("Reloading properties. configPath={}, globalPath={}", configPath, globalPath);
        CONFIG.clear();
        GLOBAL.clear();

        loadInto(CONFIG,
                configPath != null ? configPath : DEFAULT_CONFIG_FS,
                DEFAULT_CONFIG_CP, "config");

        loadInto(GLOBAL,
                globalPath != null ? globalPath : DEFAULT_GLOBAL_FS,
                DEFAULT_GLOBAL_CP, "global");

        rebuildMerged();
    }

    // ---------- Internal helpers ----------

    private static void rebuildMerged() {
        MERGED.clear();
        // base
        CONFIG.forEach((k, v) -> MERGED.setProperty(String.valueOf(k), String.valueOf(v)));
        // override by global
        GLOBAL.forEach((k, v) -> MERGED.setProperty(String.valueOf(k), String.valueOf(v)));
        // final override by -D system properties
        System.getProperties().forEach((k, v) -> MERGED.setProperty(String.valueOf(k), String.valueOf(v)));
        // simple ${var} substitution (one pass)
        interpolateOnce(MERGED);
    }

    private static void loadInto(Properties target, String fsPath, String cpPath, String label) {
        // try filesystem
        try (InputStream is = resolveInputStream(fsPath)) {
            if (is != null) {
                target.load(is);
                logger.info("Loaded {} properties from FS: {}", label, fsPath);
                return;
            }
        } catch (IOException e) {
            logger.warn("Failed FS load for {} at {}: {}", label, fsPath, e.toString());
        }

        // try classpath (direct and stripped)
        try (InputStream is = resolveClasspathStream(fsPath, cpPath)) {
            if (is != null) {
                target.load(is);
                logger.info("Loaded {} properties from CP: {}", label, cpPath);
                return;
            }
        } catch (IOException e) {
            logger.warn("Failed CP load for {} at {}: {}", label, cpPath, e.toString());
        }

        logger.warn("No {} properties found (FS={}, CP={}). Continuing with empty.", label, fsPath, cpPath);
    }

    private static InputStream resolveInputStream(String fsPath) throws IOException {
        if (fsPath != null && Files.exists(Paths.get(fsPath))) {
            return Files.newInputStream(Paths.get(fsPath));
        }
        return null;
    }

    private static InputStream resolveClasspathStream(String fsPath, String cpDefault) {
        // try exact fsPath as resource
        InputStream is = LoadProperties.class.getClassLoader().getResourceAsStream(fsPath);
        if (is != null) return is;

        // strip common prefixes
        String stripped = fsPath;
        if (fsPath != null) {
            if (fsPath.startsWith("src/main/resources/")) stripped = fsPath.substring("src/main/resources/".length());
            else if (fsPath.startsWith("src/test/resources/")) stripped = fsPath.substring("src/test/resources/".length());
        }
        if (stripped != null) {
            is = LoadProperties.class.getClassLoader().getResourceAsStream(stripped);
            if (is != null) return is;
        }

        // final fallback: provided cp default
        if (cpDefault != null) {
            return LoadProperties.class.getClassLoader().getResourceAsStream(cpDefault);
        }
        return null;
    }

    /** One-pass ${key} -> value interpolation inside the same Properties. */
    private static void interpolateOnce(Properties props) {
        for (String k : props.stringPropertyNames()) {
            String v = props.getProperty(k);
            if (v != null && v.contains("${")) {
                String out = v;
                for (String ref : props.stringPropertyNames()) {
                    out = out.replace("${" + ref + "}", props.getProperty(ref));
                }
                props.setProperty(k, out);
            }
        }
    }
}