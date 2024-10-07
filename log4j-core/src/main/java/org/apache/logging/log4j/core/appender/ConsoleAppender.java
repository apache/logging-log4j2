/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.internal.DefaultConsoleStreamSupplier;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.util.CloseShieldOutputStream;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.jspecify.annotations.Nullable;

/**
 * Appends log events to <code>System.out</code> or <code>System.err</code> using a layout specified by the user. The
 * default target is <code>System.out</code>.
 * <p>
 * TODO Accessing <code>System.out</code> or <code>System.err</code> as a byte stream instead of a writer bypasses the
 * JVM's knowledge of the proper encoding. (RG) Encoding is handled within the Layout. Typically, a Layout will generate
 * a String and then call getBytes which may use a configured encoding or the system default. OTOH, a Writer cannot
 * print byte streams.
 * </p>
 */
@Configurable(elementType = Appender.ELEMENT_TYPE, printObject = true)
@Plugin(ConsoleAppender.PLUGIN_NAME)
public final class ConsoleAppender extends AbstractOutputStreamAppender<OutputStreamManager> {

    public static final String PLUGIN_NAME = "Console";
    private static final ConsoleManagerFactory factory = new ConsoleManagerFactory();
    private static final Target DEFAULT_TARGET = Target.SYSTEM_OUT;
    private static final AtomicInteger COUNT = new AtomicInteger();
    private final Target target;

    /**
     * Enumeration of console destinations.
     */
    public enum Target {

        /** Standard output. */
        SYSTEM_OUT {
            @Override
            public Charset getDefaultCharset() {
                // "sun.stdout.encoding" is only set when running from the console.
                return getCharset("sun.stdout.encoding");
            }
        },

        /** Standard error output. */
        SYSTEM_ERR {
            @Override
            public Charset getDefaultCharset() {
                // "sun.stderr.encoding" is only set when running from the console.
                return getCharset("sun.stderr.encoding");
            }
        };

        public abstract Charset getDefaultCharset();
    }

    // We don't use PropertyEnvironment, because we just need a very specific source
    // and the property name does NOT start with log4j.
    private static Charset getCharset(final String property) {
        String charsetName = null;
        try {
            charsetName = System.getProperty(property);
            if (charsetName != null) {
                return Charset.forName(charsetName);
            }
        } catch (final SecurityException e) {
            LOGGER.warn(
                    "{} lacks permissions to access system property {}.", ConsoleAppender.class.getName(), property, e);
        } catch (final UnsupportedCharsetException e) {
            LOGGER.warn("The requested charset '{}' is not available.", charsetName, e);
        }
        return Charset.defaultCharset();
    }

    private ConsoleAppender(
            final String name,
            final Layout layout,
            final Filter filter,
            final OutputStreamManager manager,
            final boolean ignoreExceptions,
            final Target target,
            final Property[] properties) {
        super(name, layout, filter, ignoreExceptions, true, properties, manager);
        this.target = target;
    }

    public static ConsoleAppender createDefaultAppenderForLayout(final Layout layout) {
        // this method cannot use the builder class without introducing an infinite loop due to DefaultConfiguration
        return new ConsoleAppender(
                "DefaultConsole-" + COUNT.incrementAndGet(),
                layout,
                null,
                getDefaultManager(layout),
                true,
                DEFAULT_TARGET,
                null);
    }

    @PluginFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    /**
     * Builds ConsoleAppender instances.
     * @param <B> The type to build
     */
    public static class Builder<B extends Builder<B>> extends AbstractOutputStreamAppender.Builder<B>
            implements org.apache.logging.log4j.plugins.util.Builder<ConsoleAppender> {

        @PluginBuilderAttribute
        @Required
        private Target target = DEFAULT_TARGET;

        @PluginBuilderAttribute
        private boolean follow;

        @PluginBuilderAttribute
        private boolean direct;

        public B setTarget(final Target aTarget) {
            this.target = aTarget;
            return asBuilder();
        }

        public B setFollow(final boolean shouldFollow) {
            this.follow = shouldFollow;
            return asBuilder();
        }

        public B setDirect(final boolean shouldDirect) {
            this.direct = shouldDirect;
            return asBuilder();
        }

        @Override
        public ConsoleAppender build() {
            final Layout layout = getOrCreateLayout(target.getDefaultCharset());

            final Configuration configuration = getConfiguration();
            final PropertyEnvironment propertyEnvironment;
            final List<ConsoleStreamSupplier> suppliers;
            if (configuration != null) {
                propertyEnvironment = configuration.getLoggerContext() != null
                        ? configuration.getLoggerContext().getEnvironment()
                        : PropertyEnvironment.getGlobal();

                suppliers = configuration.getComponent(new @Namespace(ConsoleStreamSupplier.NAMESPACE) Key<>() {});
            } else {
                propertyEnvironment = PropertyEnvironment.getGlobal();
                suppliers = List.of(new DefaultConsoleStreamSupplier());
            }

            final OutputStream stream = suppliers.stream()
                    .map(s -> s.getOutputStream(follow, direct, target, propertyEnvironment))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (stream == null) {
                LOGGER.warn("No output stream found for target {}", target);
            }

            final String managerName = target.name() + '.' + follow + '.' + direct;
            final OutputStreamManager manager =
                    OutputStreamManager.getManager(managerName, new FactoryData(stream, managerName, layout), factory);
            return new ConsoleAppender(
                    getName(), layout, getFilter(), manager, isIgnoreExceptions(), target, getPropertyArray());
        }
    }

    private static OutputStreamManager getDefaultManager(final Layout layout) {
        final OutputStream os = new CloseShieldOutputStream(
                ConsoleAppender.DEFAULT_TARGET == Target.SYSTEM_ERR ? System.err : System.out);
        // LOG4J2-1176 DefaultConfiguration should not share OutputStreamManager instances to avoid memory leaks.
        final String managerName = ConsoleAppender.DEFAULT_TARGET.name() + ".false.false-" + COUNT.get();
        return OutputStreamManager.getManager(managerName, new FactoryData(os, managerName, layout), factory);
    }

    /**
     * Data to pass to factory method.Unable to instantiate
     */
    private static class FactoryData {
        private final OutputStream os;
        private final String name;
        private final Layout layout;

        /**
         * Constructor.
         *
         * @param os The OutputStream.
         * @param type The name of the target.
         * @param layout A layout
         */
        public FactoryData(final OutputStream os, final String type, final Layout layout) {
            this.os = os;
            this.name = type;
            this.layout = layout;
        }
    }

    /**
     * Factory to create the Appender.
     */
    private static class ConsoleManagerFactory implements ManagerFactory<OutputStreamManager, FactoryData> {

        /**
         * Create an OutputStreamManager.
         *
         * @param name The name of the entity to manage.
         * @param data The data required to create the entity.
         * @return The OutputStreamManager
         */
        @Override
        public OutputStreamManager createManager(final String name, final FactoryData data) {
            return new OutputStreamManager(data.os, data.name, data.layout, true);
        }
    }

    public Target getTarget() {
        return target;
    }

    /**
     * Abstracts the various ways `System.out` can be accessed.
     *
     * @since 3.0.0
     */
    public interface ConsoleStreamSupplier {

        /**
         * The Log4j plugin namespace of plugins implementing this interface.
         */
        String NAMESPACE = "Console";

        /**
         * @return Selects the output stream to use or {@code null} in case of error.
         */
        @Nullable
        OutputStream getOutputStream(boolean follow, boolean direct, Target target, PropertyEnvironment properties);
    }
}
