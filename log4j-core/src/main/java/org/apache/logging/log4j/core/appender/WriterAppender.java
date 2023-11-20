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

import java.io.Writer;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.util.CloseShieldWriter;

/**
 * Appends log events to a {@link Writer}.
 */
public final class WriterAppender extends AbstractWriterAppender<WriterManager> {

    /**
     * Builds WriterAppender instances.
     */
    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.plugins.util.Builder<WriterAppender> {

        private boolean follow = false;

        private Writer target;

        @Override
        public WriterAppender build() {
            final StringLayout layout = (StringLayout) getOrCreateLayout();
            return new WriterAppender(
                    getName(),
                    layout,
                    getFilter(),
                    getManager(target, follow, layout),
                    isIgnoreExceptions(),
                    getPropertyArray());
        }

        public B setFollow(final boolean shouldFollow) {
            this.follow = shouldFollow;
            return asBuilder();
        }

        public B setTarget(final Writer aTarget) {
            this.target = aTarget;
            return asBuilder();
        }
    }
    /**
     * Holds data to pass to factory method.
     */
    private static class FactoryData {
        private final StringLayout layout;
        private final String name;
        private final Writer writer;

        /**
         * Builds instances.
         *
         * @param writer
         *            The OutputStream.
         * @param type
         *            The name of the target.
         * @param layout
         *            A String layout
         */
        public FactoryData(final Writer writer, final String type, final StringLayout layout) {
            this.writer = writer;
            this.name = type;
            this.layout = layout;
        }
    }

    private static class WriterManagerFactory implements ManagerFactory<WriterManager, FactoryData> {

        /**
         * Creates a WriterManager.
         *
         * @param name
         *            The name of the entity to manage.
         * @param data
         *            The data required to create the entity.
         * @return The WriterManager
         */
        @Override
        public WriterManager createManager(final String name, final FactoryData data) {
            return new WriterManager(data.writer, data.name, data.layout, true);
        }
    }

    private static final WriterManagerFactory factory = new WriterManagerFactory();

    private static WriterManager getManager(final Writer target, final boolean follow, final StringLayout layout) {
        final Writer writer = new CloseShieldWriter(target);
        final String managerName =
                target.getClass().getName() + "@" + Integer.toHexString(target.hashCode()) + '.' + follow;
        return WriterManager.getManager(managerName, new FactoryData(writer, managerName, layout), factory);
    }

    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private WriterAppender(
            final String name,
            final StringLayout layout,
            final Filter filter,
            final WriterManager manager,
            final boolean ignoreExceptions,
            final Property[] properties) {
        super(name, layout, filter, ignoreExceptions, true, properties, manager);
    }
}
