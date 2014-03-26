package org.apache.logging.log4j.junit;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit {@link TestRule} for constructing a new LoggerContext using a specified configuration file.
 */
public class InitialLoggerContext implements TestRule {

    private final String configLocation;

    private LoggerContext context;

    public InitialLoggerContext(String configLocation) {
        this.configLocation = configLocation;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                context = Configurator.initialize(
                        description.getDisplayName(),
                        description.getTestClass().getClassLoader(),
                        configLocation
                );
                try {
                    base.evaluate();
                } finally {
                    Configurator.shutdown(context);
                }
            }
        };
    }

    public LoggerContext getContext() {
        return context;
    }
}
