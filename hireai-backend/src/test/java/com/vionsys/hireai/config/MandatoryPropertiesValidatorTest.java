package com.vionsys.hireai.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MandatoryPropertiesValidatorTest {

    private void setField(Object target, String fieldName, String value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("Fails fast when spring.datasource.password is null or blank")
    void testFailsWhenDatasourcePasswordMissing() throws Exception {
        MandatoryPropertiesValidator validator = new MandatoryPropertiesValidator();
        setField(validator, "datasourcePassword", "");
        setField(validator, "jwtSecret", "valid-test-jwt-secret-value");
        setField(validator, "aiEngineInternalKey", "valid-ai-engine-key");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateRequiredSecrets);
        assertTrue(ex.getMessage().contains("spring.datasource.password"));
        assertTrue(ex.getMessage().contains("SPRING_DATASOURCE_PASSWORD"));
    }

    @Test
    @DisplayName("Fails fast when jwt.secret is null or blank")
    void testFailsWhenJwtSecretMissing() throws Exception {
        MandatoryPropertiesValidator validator = new MandatoryPropertiesValidator();
        setField(validator, "datasourcePassword", "valid-db-pass");
        setField(validator, "jwtSecret", "   ");
        setField(validator, "aiEngineInternalKey", "valid-ai-engine-key");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateRequiredSecrets);
        assertTrue(ex.getMessage().contains("jwt.secret"));
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
    }

    @Test
    @DisplayName("Fails fast when ai-engine.internal-key is null or blank")
    void testFailsWhenAiEngineKeyMissing() throws Exception {
        MandatoryPropertiesValidator validator = new MandatoryPropertiesValidator();
        setField(validator, "datasourcePassword", "valid-db-pass");
        setField(validator, "jwtSecret", "valid-test-jwt-secret-value");
        setField(validator, "aiEngineInternalKey", null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateRequiredSecrets);
        assertTrue(ex.getMessage().contains("ai-engine.internal-key"));
        assertTrue(ex.getMessage().contains("AI_ENGINE_INTERNAL_KEY"));
    }

    @Test
    @DisplayName("Passes validation when all required secrets are provided")
    void testPassesWhenAllSecretsPresent() throws Exception {
        MandatoryPropertiesValidator validator = new MandatoryPropertiesValidator();
        setField(validator, "datasourcePassword", "db-password-123");
        setField(validator, "jwtSecret", "valid-test-jwt-secret-value");
        setField(validator, "aiEngineInternalKey", "ai-engine-key-123");

        assertDoesNotThrow(validator::validateRequiredSecrets);
    }
}
