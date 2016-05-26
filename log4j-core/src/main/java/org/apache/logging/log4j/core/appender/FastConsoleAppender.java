/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.CloseShieldOutputStream;

/**
 * Appends log events to <code>System.out</code> or <code>System.err</code> using a layout specified by the user. The
 * default target is <code>System.out</code>.
 */
@Plugin(name = "FastConsole", category = "Core", elementType = "appender", printObject = true)
public final class FastConsoleAppender extends AbstractOutputStreamAppender<OutputStreamManager> {

    private static ConsoleManagerFactory factory = new ConsoleManagerFactory();
    private static final Target DEFAULT_TARGET = Target.SYSTEM_OUT;
    private static final AtomicInteger COUNT = new AtomicInteger();

    private final Target target;

    /**
     * Enumeration of console destinations.
     */
    public enum Target {
        /** Standard output. */
        SYSTEM_OUT,
        /** Standard error output. */
        SYSTEM_ERR
    }

    private FastConsoleAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
                                final OutputStreamManager manager, final boolean ignoreExceptions, Target target) {
        super(name, layout, filter, ignoreExceptions, true, manager);
        this.target = target;
    }

    /**
     * Creates a Console Appender.
     *
     * @param layout The layout to use (required).
     * @param filter The Filter or null.
     * @param target The target (SYSTEM_OUT or SYSTEM_ERR). The default is SYSTEM_OUT.
     * @param name The name of the Appender (required).
     * @param ignoreExceptions If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise they
     *            are propagated to the caller.
     * @return The ConsoleAppender.
     */
    @PluginFactory
    public static FastConsoleAppender createAppender(
            // @formatter:off
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute(value = "target") Target target,
            @PluginAttribute("name") final String name,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions) {
            // @formatter:on
        if (name == null) {
            LOGGER.error("No name provided for ConsoleAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        target = target == null ? Target.SYSTEM_OUT : target;
        return new FastConsoleAppender(name, layout, filter, getManager(target, layout), ignoreExceptions, target);
    }

    public static FastConsoleAppender createDefaultAppenderForLayout(final Layout<? extends Serializable> layout) {
        // this method cannot use the builder class without introducing an infinite loop due to DefaultConfiguration
        return new FastConsoleAppender("DefaultConsole-" + COUNT.incrementAndGet(), layout, null,
                getDefaultManager(DEFAULT_TARGET, layout), true, DEFAULT_TARGET);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builds ConsoleAppender instances.
     */
    public static class Builder implements org.apache.logging.log4j.core.util.Builder<FastConsoleAppender> {

        @PluginElement("Layout")
        @Required
        private Layout<? extends Serializable> layout = PatternLayout.createDefaultLayout();

        @PluginElement("Filter")
        private Filter filter;

        @PluginBuilderAttribute
        @Required
        private Target target = DEFAULT_TARGET;

        @PluginBuilderAttribute
        @Required
        private String name;

        @PluginBuilderAttribute
        private boolean ignoreExceptions = true;

        public Builder setLayout(final Layout<? extends Serializable> aLayout) {
            this.layout = aLayout;
            return this;
        }

        public Builder setFilter(final Filter aFilter) {
            this.filter = aFilter;
            return this;
        }

        public Builder setTarget(final Target aTarget) {
            this.target = aTarget;
            return this;
        }

        public Builder setName(final String aName) {
            this.name = aName;
            return this;
        }

        public Builder setIgnoreExceptions(final boolean shouldIgnoreExceptions) {
            this.ignoreExceptions = shouldIgnoreExceptions;
            return this;
        }

        @Override
        public FastConsoleAppender build() {
            return new FastConsoleAppender(name, layout, filter, getManager(target, layout), ignoreExceptions, target);
        }
    }

    private static OutputStreamManager getDefaultManager(final Target target, final Layout<? extends Serializable> layout) {
        final OutputStream os = getOutputStream(target);

        // LOG4J2-1176 DefaultConfiguration should not share OutputStreamManager instances to avoid memory leaks.
        final String managerName = target.name() + '-' + COUNT.get();
        return OutputStreamManager.getManager(managerName, new FactoryData(os, managerName, layout), factory);
    }

    private static OutputStreamManager getManager(final Target target, final Layout<? extends Serializable> layout) {
        final OutputStream os = getOutputStream(target);
        final String managerName = target.name();
        return OutputStreamManager.getManager(managerName, new FactoryData(os, managerName, layout), factory);
    }

    private static OutputStream getOutputStream(final Target target) {
        OutputStream outputStream = target == Target.SYSTEM_OUT
            ? new FileOutputStream(FileDescriptor.out)
            : new FileOutputStream(FileDescriptor.err);
        return new CloseShieldOutputStream(outputStream);
    }

    /**
     * Data to pass to factory method.
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
