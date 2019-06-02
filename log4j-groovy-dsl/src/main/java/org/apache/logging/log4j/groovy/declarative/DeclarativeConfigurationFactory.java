package org.apache.logging.log4j.groovy.declarative;

import groovy.util.DelegatingScript;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.IOException;

@Plugin(name = "DeclarativeConfigurationFactory", category = ConfigurationFactory.CATEGORY)
public class DeclarativeConfigurationFactory extends ConfigurationFactory {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String[] SUPPORTED_TYPES = {".groovydsl"};

    @Override
    protected String[] getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        DeclarativeConfigurationCompiler compiler = DeclarativeConfigurationCompiler.newCompiler(loggerContext);
        DelegatingScript script;
        try {
            script = compiler.compile(source);
        } catch (IOException e) {
            LOGGER.error("Could not compile ConfigurationSource {}", source, e);
            return null;
        }
        DeclarativeConfigurationBuilder builder = new DeclarativeConfigurationBuilder(loggerContext, source);
        script.setDelegate(builder);
        try {
            script.run();
        } catch (Exception e) {
            LOGGER.error("Could not run ConfigurationSource {}", source, e);
            return null;
        }
        return builder.build();
    }

}
