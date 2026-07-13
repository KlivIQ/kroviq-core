package kroviq.api.utils;

import io.restassured.module.jsv.JsonSchemaValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * JSON Schema validation for API responses.
 * Schema files live in: {test-project-root}/api-schemas/
 */
public final class SchemaValidator {

    private static final Logger logger         = LogManager.getLogger(SchemaValidator.class);
    private static final String SCHEMA_DIR     = "api-schemas/";
    private static final String CLASSPATH_DIR  = "api-schemas/";

    private SchemaValidator() {}

    public static void validate(String responseBody, String schemaFileName) {
        InputStream schemaStream = resolveSchema(schemaFileName);
        if (schemaStream == null) {
            throw new AssertionError(
                "[SchemaValidator] Schema not found: '" + schemaFileName + "'. " +
                "Expected at: " + SCHEMA_DIR + schemaFileName + " or on classpath.");
        }

        try {
            JsonSchemaValidator.matchesJsonSchema(schemaStream).matches(responseBody);
            logger.info("[SchemaValidator] Validated against schema: {}", schemaFileName);
        } catch (Exception e) {
            throw new AssertionError(
                "[SchemaValidator] Schema validation failed for '" + schemaFileName + "': " +
                e.getMessage(), e);
        }
    }

    private static InputStream resolveSchema(String schemaFileName) {
        File file = new File(SCHEMA_DIR + schemaFileName);
        if (file.exists()) {
            try {
                logger.debug("[SchemaValidator] Loaded schema from filesystem: {}", file.getAbsolutePath());
                return new FileInputStream(file);
            } catch (Exception e) {
                logger.warn("[SchemaValidator] Could not open schema file: {}", e.getMessage());
            }
        }

        InputStream stream = SchemaValidator.class.getClassLoader()
                .getResourceAsStream(CLASSPATH_DIR + schemaFileName);
        if (stream != null) {
            logger.debug("[SchemaValidator] Loaded schema from classpath: {}", schemaFileName);
            return stream;
        }

        return null;
    }
}
