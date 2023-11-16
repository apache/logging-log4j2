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
package org.apache.log4j.builders.appender;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.config.Log4j1Configuration.THRESHOLD_PARAM;
import static org.apache.log4j.xml.XmlConfiguration.FILTER_TAG;
import static org.apache.log4j.xml.XmlConfiguration.LAYOUT_TAG;
import static org.apache.log4j.xml.XmlConfiguration.PARAM_TAG;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.bridge.LayoutAdapter;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

/**
 * Build a Console Appender
 */
@Plugin(name = "org.apache.log4j.net.SocketAppender", category = CATEGORY)
public class SocketAppenderBuilder extends AbstractBuilder implements AppenderBuilder {

    private static final String HOST_PARAM = "RemoteHost";
    private static final String PORT_PARAM = "Port";
    private static final String RECONNECTION_DELAY_PARAM = "ReconnectionDelay";
    private static final int DEFAULT_PORT = 4560;

    /**
     * The default reconnection delay (30000 milliseconds or 30 seconds).
     */
    private static final int DEFAULT_RECONNECTION_DELAY = 30_000;

    public static final Logger LOGGER = StatusLogger.getLogger();

    public SocketAppenderBuilder() {}

    public SocketAppenderBuilder(final String prefix, final Properties props) {
        super(prefix, props);
    }

    private <T extends Log4j1Configuration> Appender createAppender(
            final String name,
            final String host,
            final int port,
            final Layout layout,
            final Filter filter,
            final String level,
            final boolean immediateFlush,
            final int reconnectDelayMillis,
            final T configuration) {
        final org.apache.logging.log4j.core.Layout<?> actualLayout = LayoutAdapter.adapt(layout);
        final org.apache.logging.log4j.core.Filter actualFilter = buildFilters(level, filter);
        // @formatter:off
        return AppenderWrapper.adapt(SocketAppender.newBuilder()
                .setHost(host)
                .setPort(port)
                .setReconnectDelayMillis(reconnectDelayMillis)
                .setName(name)
                .setLayout(actualLayout)
                .setFilter(actualFilter)
                .setConfiguration(configuration)
                .setImmediateFlush(immediateFlush)
                .build());
        // @formatter:on
    }

    @Override
    public Appender parseAppender(final Element appenderElement, final XmlConfiguration config) {
        final String name = getNameAttribute(appenderElement);
        final AtomicReference<String> host = new AtomicReference<>("localhost");
        final AtomicInteger port = new AtomicInteger(DEFAULT_PORT);
        final AtomicInteger reconnectDelay = new AtomicInteger(DEFAULT_RECONNECTION_DELAY);
        final AtomicReference<Layout> layout = new AtomicReference<>();
        final AtomicReference<Filter> filter = new AtomicReference<>();
        final AtomicReference<String> level = new AtomicReference<>();
        final AtomicBoolean immediateFlush = new AtomicBoolean(true);
        forEachElement(appenderElement.getChildNodes(), currentElement -> {
            switch (currentElement.getTagName()) {
                case LAYOUT_TAG:
                    layout.set(config.parseLayout(currentElement));
                    break;
                case FILTER_TAG:
                    config.addFilter(filter, currentElement);
                    break;
                case PARAM_TAG:
                    switch (getNameAttributeKey(currentElement)) {
                        case HOST_PARAM:
                            set(HOST_PARAM, currentElement, host);
                            break;
                        case PORT_PARAM:
                            set(PORT_PARAM, currentElement, port);
                            break;
                        case RECONNECTION_DELAY_PARAM:
                            set(RECONNECTION_DELAY_PARAM, currentElement, reconnectDelay);
                            break;
                        case THRESHOLD_PARAM:
                            set(THRESHOLD_PARAM, currentElement, level);
                            break;
                        case IMMEDIATE_FLUSH_PARAM:
                            set(IMMEDIATE_FLUSH_PARAM, currentElement, immediateFlush);
                            break;
                    }
                    break;
            }
        });
        return createAppender(
                name,
                host.get(),
                port.get(),
                layout.get(),
                filter.get(),
                level.get(),
                immediateFlush.get(),
                reconnectDelay.get(),
                config);
    }

    @Override
    public Appender parseAppender(
            final String name,
            final String appenderPrefix,
            final String layoutPrefix,
            final String filterPrefix,
            final Properties props,
            final PropertiesConfiguration configuration) {
        // @formatter:off
        return createAppender(
                name,
                getProperty(HOST_PARAM),
                getIntegerProperty(PORT_PARAM, DEFAULT_PORT),
                configuration.parseLayout(layoutPrefix, name, props),
                configuration.parseAppenderFilters(props, filterPrefix, name),
                getProperty(THRESHOLD_PARAM),
                getBooleanProperty(IMMEDIATE_FLUSH_PARAM),
                getIntegerProperty(RECONNECTION_DELAY_PARAM, DEFAULT_RECONNECTION_DELAY),
                configuration);
        // @formatter:on
    }
}
