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

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.net.SocketOptions;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

/**
 * Sends log events over multiple sockets.
 * <p>
 * for TCP only
 */
@Configurable(elementType = Appender.ELEMENT_TYPE, printObject = true)
@Plugin("MultipleSocket")
public final class MultipleSocketAppender extends AbstractAppender {
    /**
     * Builds MultipleSocketAppender instances.
     */
    public static class Builder extends AbstractAppender.Builder<Builder>
            implements org.apache.logging.log4j.plugins.util.Builder<MultipleSocketAppender> {
        /**
         * address:port[,address:port]
         */
        @PluginBuilderAttribute
        @Required(message = "server list is required")
        private String serverList;

        @PluginElement("SocketOptions")
        private SocketOptions socketOptions;

        @PluginBuilderAttribute
        private int connectTimeoutMillis = 0;

        @PluginBuilderAttribute
        private int reconnectionDelayMillis = 0;

        /**
         * for future versions
         */
        @PluginBuilderAttribute
        private boolean immediateFlush = true;

        @PluginBuilderAttribute
        private boolean immediateFail = true;

        @Override
        public MultipleSocketAppender build() {
            final Layout layout = getLayout();
            if (layout == null) {
                AbstractLifeCycle.LOGGER.error("No layout provided for MultipleSocketAppender");
                return null;
            }

            final String name = getName();
            if (name == null) {
                AbstractLifeCycle.LOGGER.error("No name provided for MultipleSocketAppender");
                return null;
            }

            final MultipleSocketManager multipleSocketManager = new MultipleSocketManager(
                    getConfiguration().getLoggerContext(),
                    getName(),
                    getConfiguration(),
                    serverList,
                    socketOptions,
                    connectTimeoutMillis,
                    reconnectionDelayMillis,
                    immediateFlush,
                    immediateFail);

            final MultipleSocketAppender multipleSocketAppender = new MultipleSocketAppender(
                    getName(),
                    getLayout(),
                    getFilter(),
                    isIgnoreExceptions(),
                    multipleSocketManager,
                    getPropertyArray());

            return multipleSocketAppender;
        }

        public String getServerList() {
            return serverList;
        }

        public SocketOptions getSocketOptions() {
            return socketOptions;
        }

        public int getConnectTimeoutMillis() {
            return connectTimeoutMillis;
        }

        public int getReconnectionDelayMillis() {
            return reconnectionDelayMillis;
        }

        public boolean getImmediateFlush() {
            return immediateFlush;
        }

        public boolean getImmediateFail() {
            return immediateFail;
        }

        public Builder setServerList(final String serverList) {
            this.serverList = serverList;
            return asBuilder();
        }

        public Builder setSocketOptions(final SocketOptions socketOptions) {
            this.socketOptions = socketOptions;
            return asBuilder();
        }

        public Builder setConnectTimeoutMillis(final int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
            return asBuilder();
        }

        public Builder setReconnectionDelayMillis(final int reconnectionDelayMillis) {
            this.reconnectionDelayMillis = reconnectionDelayMillis;
            return asBuilder();
        }

        public Builder setImmediateFlush(final boolean immediateFlush) {
            this.immediateFlush = immediateFlush;
            return asBuilder();
        }

        public Builder setImmediateFail(final boolean immediateFail) {
            this.immediateFail = immediateFail;
            return asBuilder();
        }
    }

    /**
     * @return a builder for a MultipleSocketAppender.
     */
    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    private final MultipleSocketManager manager;

    private MultipleSocketAppender(
            final String name,
            final Layout layout,
            final Filter filter,
            final boolean ignoreExceptions,
            final MultipleSocketManager manager,
            final Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
        Objects.requireNonNull(layout, "layout");
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void append(final LogEvent event) {
        try {
            manager.send(getLayout(), event);
        } catch (final Exception e) {
            error("Unable to send event in appender [" + getName() + "]", event, e);
            throw new AppenderLoggingException(e);
        }
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        boolean stopped = super.stop(timeout, timeUnit, false);
        stopped &= manager.stop(timeout, timeUnit);
        setStopped();
        return stopped;
    }

    @Override
    public String toString() {
        return "MultipleSocketAppender{" + "name=" + getName() + ", state=" + getState() + '}';
    }
}
