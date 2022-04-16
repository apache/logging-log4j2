package org.apache.logging.log4j.core.async;

import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.script.AbstractScript;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.Objects;

/**
 * This class allows users to configure the factory used to create
 * an instance of the LMAX disruptor WaitStrategy
 * used by Async Loggers in the log4j configuration.
 */
@Plugin(name = "AsyncWaitStrategyFactory", category = Core.CATEGORY_NAME, printObject = true)
public class AsyncWaitStrategyFactoryConfig {

    /**
     * Status logger for internal logging.
     */
    protected static final org.apache.logging.log4j.Logger LOGGER = StatusLogger.getLogger();

    private final String factoryClassName;

    public AsyncWaitStrategyFactoryConfig(final String factoryClassName) {
        this.factoryClassName = Objects.requireNonNull(factoryClassName, "factoryClassName");
    }
//    /**
//     * Return the AsyncWaitStrategyFactoryConfig
//     * @param result the AsyncWaitStrategyFactoryConfig.
//     * @return The AsyncWaitStrategyFactoryConfig.
//     */
//    @PluginFactory
//    public static AsyncWaitStrategyFactoryConfig createAsyncWaitStrategyFactoryConfig(
//            @PluginElement("AsyncWaitStrategyFactory") final AsyncWaitStrategyFactoryConfig result) {
//        return result;
//    }

    @PluginBuilderFactory
    public static <B extends AsyncWaitStrategyFactoryConfig.Builder<B>> B newBuilder() {
        return new AsyncWaitStrategyFactoryConfig.Builder<B>().asBuilder();
    }

    /**
     * Builds AsyncWaitStrategyFactoryConfig instances.
     *
     * @param <B>
     *            The type to build
     */
    public static class Builder<B extends AsyncWaitStrategyFactoryConfig.Builder<B>>
            implements org.apache.logging.log4j.core.util.Builder<AsyncWaitStrategyFactoryConfig> {

        @PluginBuilderAttribute("class")
        @Required(message = "AsyncWaitStrategyFactory cannot be configured without a factory class name")
        private String factoryClassName;

        public String getFactoryClassName() {
            return factoryClassName;
        }

        public B withFactoryClassName(String className) {
            this.factoryClassName = className;
            return asBuilder();
        }

        @Override
        public AsyncWaitStrategyFactoryConfig build() {
            return new AsyncWaitStrategyFactoryConfig(factoryClassName);
        }

        @SuppressWarnings("unchecked")
        public B asBuilder() {
            return (B) this;
        }
    }

    public AsyncWaitStrategyFactory createWaitStrategyFactory() {
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends AsyncWaitStrategyFactory> klass = (Class<? extends AsyncWaitStrategyFactory>) Loader.loadClass(factoryClassName);
            return klass.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOGGER.info("Invalid implementation class name value: error creating AsyncWaitStrategyFactory {}: {}", factoryClassName, e);
            return null;
        }

    }
}
