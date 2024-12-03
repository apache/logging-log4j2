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

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.CloseShieldOutputStream;
import org.apache.logging.log4j.util.PropertiesUtil;

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
@Plugin(
        name = ConsoleAppender.PLUGIN_NAME,
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE,
        printObject = true)
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
                return getCharset("sun.stdout.encoding", Charset.defaultCharset());
            }
        },

        /** Standard error output. */
        SYSTEM_ERR {
            @Override
            public Charset getDefaultCharset() {
                // "sun.stderr.encoding" is only set when running from the console.
                return getCharset("sun.stderr.encoding", Charset.defaultCharset());
            }
        };

        public abstract Charset getDefaultCharset();

        protected Charset getCharset(final String property, final Charset defaultCharset) {
            return new PropertiesUtil(PropertiesUtil.getSystemProperties())
                    .getCharsetProperty(property, defaultCharset);
        }
    }

    private ConsoleAppender(
            final String name,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final OutputStreamManager manager,
            final boolean ignoreExceptions,
            final Target target,
            final Property[] properties) {
        super(name, layout, filter, ignoreExceptions, true, properties, manager);
        this.target = target;
    }

    /**
     * Creates a Console Appender.
     *
     * @param layout The layout to use (required).
     * @param filter The Filter or null.
     * @param target The target ("SYSTEM_OUT" or "SYSTEM_ERR"). The default is "SYSTEM_OUT".
     * @param name The name of the Appender (required).
     * @param follow If true will follow changes to the underlying output stream.
     * @param ignoreExceptions If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise they
     *            are propagated to the caller.
     * @return The ConsoleAppender.
     * @deprecated Deprecated in 2.7; use {@link #newBuilder()}.
     */
    @Deprecated
    public static ConsoleAppender createAppender(
            Layout<? extends Serializable> layout,
            final Filter filter,
            final String target,
            final String name,
            final String follow,
            final String ignoreExceptions) {
        return newBuilder()
                .setLayout(layout)
                .setFilter(filter)
                .setTarget(target == null ? DEFAULT_TARGET : Target.valueOf(target))
                .setName(name)
                .setFollow(Boolean.parseBoolean(follow))
                .setIgnoreExceptions(Booleans.parseBoolean(ignoreExceptions, true))
                .build();
    }

    /**
     * Creates a Console Appender.
     *
     * @param layout The layout to use (required).
     * @param filter The Filter or null.
     * @param target The target (SYSTEM_OUT or SYSTEM_ERR). The default is SYSTEM_OUT.
     * @param name The name of the Appender (required).
     * @param follow If true will follow changes to the underlying output stream.
     * @param direct If true will write directly to {@link java.io.FileDescriptor} and bypass
     *            {@link System#out}/{@link System#err}.
     * @param ignoreExceptions If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise they
     *            are propagated to the caller.
     * @return The ConsoleAppender.
     * @deprecated Deprecated in 2.7; use {@link #newBuilder()}.
     */
    @Deprecated
    public static ConsoleAppender createAppender(
            // @formatter:off
            Layout<? extends Serializable> layout,
            final Filter filter,
            Target target,
            final String name,
            final boolean follow,
            final boolean direct,
            final boolean ignoreExceptions) {
        return newBuilder()
                .setLayout(layout)
                .setFilter(filter)
                .setTarget(target)
                .setName(name)
                .setFollow(follow)
                .setDirect(direct)
                .setIgnoreExceptions(ignoreExceptions)
                .build();
    }

    public static ConsoleAppender createDefaultAppenderForLayout(final Layout<? extends Serializable> layout) {
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

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    /**
     * Builds ConsoleAppender instances.
     * @param <B> The type to build
     */
    public static class Builder<B extends Builder<B>> extends AbstractOutputStreamAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<ConsoleAppender> {

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
            if (!isValid()) {
                return null;
            }
            if (direct && follow) {
                LOGGER.error("Cannot use both `direct` and `follow` on ConsoleAppender.");
                return null;
            }
            final Layout<? extends Serializable> layout = getOrCreateLayout(target.getDefaultCharset());

            OutputStream stream = direct
                    ? getDirectOutputStream(target)
                    : follow ? getFollowOutputStream(target) : getDefaultOutputStream(target);

            final String managerName = target.name() + '.' + follow + '.' + direct;
            final OutputStreamManager manager =
                    OutputStreamManager.getManager(managerName, new FactoryData(stream, managerName, layout), factory);
            return new ConsoleAppender(
                    getName(), layout, getFilter(), manager, isIgnoreExceptions(), target, getPropertyArray());
        }
    }

    private static OutputStreamManager getDefaultManager(final Layout<? extends Serializable> layout) {
        final OutputStream os = getDefaultOutputStream(ConsoleAppender.DEFAULT_TARGET);
        // LOG4J2-1176 DefaultConfiguration should not share OutputStreamManager instances to avoid memory leaks.
        final String managerName = ConsoleAppender.DEFAULT_TARGET.name() + ".false.false-" + COUNT.get();
        return OutputStreamManager.getManager(managerName, new FactoryData(os, managerName, layout), factory);
    }

    private static OutputStream getDefaultOutputStream(Target target) {
        return new CloseShieldOutputStream(target == Target.SYSTEM_OUT ? System.out : System.err);
    }

    private static OutputStream getDirectOutputStream(Target target) {
        return new CloseShieldOutputStream(
                new FileOutputStream(target == Target.SYSTEM_OUT ? FileDescriptor.out : FileDescriptor.err));
    }

    private static OutputStream getFollowOutputStream(Target target) {
        return target == Target.SYSTEM_OUT ? new SystemOutStream() : new SystemErrStream();
    }

    /**
     * An implementation of OutputStream that redirects to the current System.err.
     */
    private static class SystemErrStream extends OutputStream {
        public SystemErrStream() {}

        @Override
        public void close() {
            // do not close sys err!
        }

        @Override
        public void flush() {
            System.err.flush();
        }

        @Override
        public void write(final byte[] b) throws IOException {
            System.err.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            System.err.write(b, off, len);
        }

        @Override
        public void write(final int b) {
            System.err.write(b);
        }
    }

    /**
     * An implementation of OutputStream that redirects to the current System.out.
     */
    private static class SystemOutStream extends OutputStream {
        public SystemOutStream() {}

        @Override
        public void close() {
            // do not close sys out!
        }

        @Override
        public void flush() {
            System.out.flush();
        }

        @Override
        public void write(final byte[] b) throws IOException {
            System.out.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            System.out.write(b, off, len);
        }

        @Override
        public void write(final int b) throws IOException {
            System.out.write(b);
        }
    }

    /**
     * Data to pass to factory method.Unable to instantiate
     */
    private static class FactoryData {
        private final OutputStream os;
        private final String name;
        private final Layout<? extends Serializable> layout;

        /**
         * Constructor.
         *
         * @param os The OutputStream.
         * @param type The name of the target.
         * @param layout A Serializable layout
         */
        public FactoryData(final OutputStream os, final String type, final Layout<? extends Serializable> layout) {
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
}
