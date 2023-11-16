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
import java.io.Serializable;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.CloseShieldOutputStream;
import org.apache.logging.log4j.core.util.NullOutputStream;

/**
 * Appends log events to a given output stream using a layout.
 * <p>
 * Character encoding is handled within the Layout.
 * </p>
 */
@Plugin(name = "OutputStream", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class OutputStreamAppender extends AbstractOutputStreamAppender<OutputStreamManager> {

    /**
     * Builds OutputStreamAppender instances.
     *
     * @param <B>
     *            The type to build.
     */
    public static class Builder<B extends Builder<B>> extends AbstractOutputStreamAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<OutputStreamAppender> {

        private boolean follow = false;

        private final boolean ignoreExceptions = true;

        private OutputStream target;

        @Override
        public OutputStreamAppender build() {
            final Layout<? extends Serializable> layout = getOrCreateLayout();
            return new OutputStreamAppender(
                    getName(),
                    layout,
                    getFilter(),
                    getManager(target, follow, layout),
                    ignoreExceptions,
                    getPropertyArray());
        }

        public B setFollow(final boolean shouldFollow) {
            this.follow = shouldFollow;
            return asBuilder();
        }

        public B setTarget(final OutputStream aTarget) {
            this.target = aTarget;
            return asBuilder();
        }
    }

    /**
     * Holds data to pass to factory method.
     */
    private static class FactoryData {
        private final Layout<? extends Serializable> layout;
        private final String name;
        private final OutputStream os;

        /**
         * Builds instances.
         *
         * @param os
         *            The OutputStream.
         * @param type
         *            The name of the target.
         * @param layout
         *            A Serializable layout
         */
        public FactoryData(final OutputStream os, final String type, final Layout<? extends Serializable> layout) {
            this.os = os;
            this.name = type;
            this.layout = layout;
        }
    }

    /**
     * Creates the manager.
     */
    private static class OutputStreamManagerFactory implements ManagerFactory<OutputStreamManager, FactoryData> {

        /**
         * Creates an OutputStreamManager.
         *
         * @param name
         *            The name of the entity to manage.
         * @param data
         *            The data required to create the entity.
         * @return The OutputStreamManager
         */
        @Override
        public OutputStreamManager createManager(final String name, final FactoryData data) {
            return new OutputStreamManager(data.os, data.name, data.layout, true);
        }
    }

    private static OutputStreamManagerFactory factory = new OutputStreamManagerFactory();

    /**
     * Creates an OutputStream Appender.
     *
     * @param layout
     *            The layout to use or null to get the default layout.
     * @param filter
     *            The Filter or null.
     * @param target
     *            an output stream.
     * @param follow
     *            If true will follow changes to the underlying output stream.
     *            Use false as the default.
     * @param name
     *            The name of the Appender (required).
     * @param ignore
     *            If {@code "true"} (default) exceptions encountered when
     *            appending events are logged; otherwise they are propagated to
     *            the caller. Use true as the default.
     * @return The ConsoleAppender.
     */
    @PluginFactory
    public static OutputStreamAppender createAppender(
            Layout<? extends Serializable> layout,
            final Filter filter,
            final OutputStream target,
            final String name,
            final boolean follow,
            final boolean ignore) {
        if (name == null) {
            LOGGER.error("No name provided for OutputStreamAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new OutputStreamAppender(name, layout, filter, getManager(target, follow, layout), ignore, null);
    }

    private static OutputStreamManager getManager(
            final OutputStream target, final boolean follow, final Layout<? extends Serializable> layout) {
        final OutputStream os = target == null ? NullOutputStream.getInstance() : new CloseShieldOutputStream(target);
        final OutputStream targetRef = target == null ? os : target;
        final String managerName =
                targetRef.getClass().getName() + "@" + Integer.toHexString(targetRef.hashCode()) + '.' + follow;
        return OutputStreamManager.getManager(managerName, new FactoryData(os, managerName, layout), factory);
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private OutputStreamAppender(
            final String name,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final OutputStreamManager manager,
            final boolean ignoreExceptions,
            final Property[] properties) {
        super(name, layout, filter, ignoreExceptions, true, properties, manager);
    }
}
