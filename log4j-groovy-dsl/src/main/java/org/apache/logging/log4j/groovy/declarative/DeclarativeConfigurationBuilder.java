package org.apache.logging.log4j.groovy.declarative;

import groovy.lang.Closure;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.util.Builder;
import org.apache.logging.log4j.groovy.api.Component;
import org.apache.logging.log4j.groovy.api.ComponentBuilder;

import java.util.Collections;
import java.util.Map;

public class DeclarativeConfigurationBuilder implements Builder<DeclarativeConfiguration> {

    private final LoggerContext context;
    private final ConfigurationSource source;
    private final ComponentBuilder configuration = Component.newBuilder();

    DeclarativeConfigurationBuilder(final LoggerContext context, final ConfigurationSource source) {
        this.context = context;
        this.source = source;
    }

    public void configuration(final Closure<?> body) {
        configuration(Collections.emptyMap(), body);
    }

    public void configuration(final Map<String, String> attributes, final Closure<?> body) {
        DeclarativeComponentBuilder builder = new DeclarativeComponentBuilder(context, source, configuration.withAttributes(attributes));
        body.setDelegate(builder);
        body.run();
    }

    @Override
    public DeclarativeConfiguration build() {
        return new DeclarativeConfiguration(context, source, configuration.build());
    }

}
