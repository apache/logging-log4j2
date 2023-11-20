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
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.util.CloseShieldOutputStream;

/**
 * Appends log events to a given output stream using a layout.
 * <p>
 * Character encoding is handled within the Layout.
 * </p>
 */
public final class OutputStreamAppender extends AbstractOutputStreamAppender<OutputStreamManager> {

    /**
     * Builds OutputStreamAppender instances.
     *
     * @param <B>
     *            The type to build.
     */
    public abstract static class Builder<B extends Builder<B>> extends AbstractOutputStreamAppender.Builder<B>
            implements org.apache.logging.log4j.plugins.util.Builder<OutputStreamAppender> {

        private boolean follow = false;

        private OutputStream target;

        public boolean isFollow() {
            return follow;
        }

        public OutputStream getTarget() {
            return target;
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

    public static class OutputStreamAppenderBuilder extends Builder<OutputStreamAppenderBuilder> {
        @Override
        public OutputStreamAppender build() {
            final Layout layout = getOrCreateLayout();
            final OutputStreamManager manager = getManager(getTarget(), isFollow(), layout);
            return new OutputStreamAppender(
                    getName(), layout, getFilter(), manager, isIgnoreExceptions(), getPropertyArray());
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

    private static OutputStreamManager getManager(
            final OutputStream target, final boolean follow, final Layout layout) {
        final OutputStream os = target == null ? OutputStream.nullOutputStream() : new CloseShieldOutputStream(target);
        final OutputStream targetRef = target == null ? os : target;
        final String managerName =
                targetRef.getClass().getName() + "@" + Integer.toHexString(targetRef.hashCode()) + '.' + follow;
        return OutputStreamManager.getManager(managerName, new FactoryData(os, managerName, layout), factory);
    }

    public static OutputStreamAppenderBuilder newBuilder() {
        return new OutputStreamAppenderBuilder();
    }

    private OutputStreamAppender(
            final String name,
            final Layout layout,
            final Filter filter,
            final OutputStreamManager manager,
            final boolean ignoreExceptions,
            final Property[] properties) {
        super(name, layout, filter, ignoreExceptions, true, null, manager);
    }
}
