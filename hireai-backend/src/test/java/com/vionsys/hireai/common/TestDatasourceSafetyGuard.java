package com.vionsys.hireai.common;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails fast if the test datasource does not target an isolated test database.
 * Prevents any accidental test writes to development or production databases.
 */
@Component
public class TestDatasourceSafetyGuard implements InitializingBean {

    private final Environment environment;

    public TestDatasourceSafetyGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        String url = environment.getProperty("spring.datasource.url", "");
        if (!url.isBlank()) {
            String lower = url.toLowerCase();
            boolean isTestDb = lower.contains("_test") || lower.contains(":mem:") || lower.contains("testdb");
            if (!isTestDb) {
                throw new IllegalStateException(
                        "FATAL SAFETY VIOLATION: Test suite attempted to connect to a non-test database! " +
                        "The datasource URL must point to an isolated test database whose name ends with '_test' or an in-memory database. " +
                        "Current URL: " + url);
            }
        }
    }
}
