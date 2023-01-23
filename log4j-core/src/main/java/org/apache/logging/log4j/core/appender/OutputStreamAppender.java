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

import java.io.OutputStream;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.CloseShieldOutputStream;
import org.apache.logging.log4j.core.util.NullOutputStream;

/**
 * Appends log events to a given output stream using a layout.
 * <p>
 * Character encoding is handled within the Layout.
 * </p>
 */
public final class OutputStreamAppender extends AbstractOutputStreamAppender<OutputStreamManager> {

    /**
     * Builds OutputStreamAppender instances.
     */
    public static class Builder extends AbstractOutputStreamAppender.Builder<Builder>
            implements org.apache.logging.log4j.plugins.util.Builder<OutputStreamAppender> {

        private boolean follow = false;

        private OutputStream target;

        @Override
        public OutputStreamAppender build() {
            final Layout layout = getOrCreateLayout();
            final OutputStreamManager manager = getManager(target, follow, layout);
            return new OutputStreamAppender(getName(), layout, getFilter(), manager, isIgnoreExceptions());
        }

        public boolean isFollow() {
            return follow;
        }

        public OutputStream getTarget() {
            return target;
        }

        public Builder setFollow(final boolean shouldFollow) {
            this.follow = shouldFollow;
            return this;
        }

        public Builder setTarget(final OutputStream aTarget) {
            this.target = aTarget;
            return this;
        }
    }
    /**
     * Holds data to pass to factory method.
     */
    private static class FactoryData {
        private final Layout layout;
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
         *            A layout
         */
        public FactoryData(final OutputStream os, final String type, final Layout layout) {
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

    private static final OutputStreamManagerFactory factory = new OutputStreamManagerFactory();

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
    public static OutputStreamAppender createAppender(Layout layout, final Filter filter,
                                                      final OutputStream target, final String name, final boolean follow, final boolean ignore) {
        if (name == null) {
            LOGGER.error("No name provided for OutputStreamAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new OutputStreamAppender(name, layout, filter, getManager(target, follow, layout), ignore);
    }

    private static OutputStreamManager getManager(final OutputStream target, final boolean follow,
            final Layout layout) {
        final OutputStream os = target == null ? NullOutputStream.getInstance() : new CloseShieldOutputStream(target);
        final OutputStream targetRef = target == null ? os : target;
        final String managerName = targetRef.getClass().getName() + "@" + Integer.toHexString(targetRef.hashCode())
                + '.' + follow;
        return OutputStreamManager.getManager(managerName, new FactoryData(os, managerName, layout), factory);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private OutputStreamAppender(final String name, final Layout layout, final Filter filter,
                                 final OutputStreamManager manager, final boolean ignoreExceptions) {
        super(name, layout, filter, ignoreExceptions, true, null, manager);
    }

}
