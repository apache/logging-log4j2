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
package org.apache.log4j.builders.appender;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.config.Log4j1Configuration.THRESHOLD_PARAM;
import static org.apache.log4j.xml.XmlConfiguration.FILTER_TAG;
import static org.apache.log4j.xml.XmlConfiguration.LAYOUT_TAG;
import static org.apache.log4j.xml.XmlConfiguration.NAME_ATTR;
import static org.apache.log4j.xml.XmlConfiguration.PARAM_TAG;
import static org.apache.log4j.xml.XmlConfiguration.VALUE_ATTR;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.bridge.LayoutAdapter;
import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.SyslogLayout;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

/**
 * Build a File Appender
 */
@Plugin(name = "org.apache.log4j.net.SyslogAppender", category = CATEGORY)
public class SyslogAppenderBuilder extends AbstractBuilder implements AppenderBuilder {

    private static final String DEFAULT_HOST = "localhost";
    private static int DEFAULT_PORT = 514;
    private static final String DEFAULT_FACILITY = "LOCAL0";

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String FACILITY_PARAM = "Facility";
    private static final String SYSLOG_HOST_PARAM = "SyslogHost";
    private static final String PROTOCOL_PARAM = "protocol";

    public SyslogAppenderBuilder() {}

    public SyslogAppenderBuilder(String prefix, Properties props) {
        super(prefix, props);
    }

    @Override
    public Appender parseAppender(Element appenderElement, XmlConfiguration config) {
        String name = appenderElement.getAttribute(NAME_ATTR);
        AtomicReference<Layout> layout = new AtomicReference<>();
        AtomicReference<Filter> filter = new AtomicReference<>();
        AtomicReference<String> facility = new AtomicReference<>();
        AtomicReference<String> level = new AtomicReference<>();
        AtomicReference<String> host = new AtomicReference<>();
        AtomicReference<Protocol> protocol = new AtomicReference<>();
        forEachElement(appenderElement.getChildNodes(), currentElement -> {
            switch (currentElement.getTagName()) {
                case LAYOUT_TAG:
                    layout.set(config.parseLayout(currentElement));
                    break;
                case FILTER_TAG:
                    filter.set(config.parseFilters(currentElement));
                    break;
                case PARAM_TAG: {
                    switch (currentElement.getAttribute(NAME_ATTR)) {
                        case SYSLOG_HOST_PARAM: {
                            host.set(currentElement.getAttribute(VALUE_ATTR));
                            break;
                        }
                        case FACILITY_PARAM:
                            facility.set(currentElement.getAttribute(VALUE_ATTR));
                            break;
                        case THRESHOLD_PARAM: {
                            String value = currentElement.getAttribute(VALUE_ATTR);
                            if (value == null) {
                                LOGGER.warn("No value supplied for Threshold parameter, ignoring.");
                            } else {
                                level.set(value);
                            }
                            break;
                        }
                        case PROTOCOL_PARAM:
                            protocol.set(Protocol.valueOf(currentElement.getAttribute(VALUE_ATTR)));
                            break;
                    }
                    break;
                }
            }
        });

        return createAppender(
                name, config, layout.get(), facility.get(), filter.get(), host.get(), level.get(), protocol.get());
    }

    @Override
    public Appender parseAppender(
            final String name,
            final String appenderPrefix,
            final String layoutPrefix,
            final String filterPrefix,
            final Properties props,
            final PropertiesConfiguration configuration) {
        Filter filter = configuration.parseAppenderFilters(props, filterPrefix, name);
        Layout layout = configuration.parseLayout(layoutPrefix, name, props);
        String level = getProperty(THRESHOLD_PARAM);
        String facility = getProperty(FACILITY_PARAM, DEFAULT_FACILITY);
        String syslogHost = getProperty(SYSLOG_HOST_PARAM, DEFAULT_HOST + ":" + DEFAULT_PORT);
        String protocol = getProperty(PROTOCOL_PARAM, Protocol.TCP.name());

        return createAppender(
                name, configuration, layout, facility, filter, syslogHost, level, Protocol.valueOf(protocol));
    }

    private Appender createAppender(
            final String name,
            final Log4j1Configuration configuration,
            Layout layout,
            String facility,
            final Filter filter,
            final String syslogHost,
            final String level,
            final Protocol protocol) {
        AtomicReference<String> host = new AtomicReference<>();
        AtomicInteger port = new AtomicInteger();
        resolveSyslogHost(syslogHost, host, port);
        org.apache.logging.log4j.core.Layout<? extends Serializable> appenderLayout;
        if (layout instanceof LayoutWrapper) {
            appenderLayout = ((LayoutWrapper) layout).getLayout();
        } else if (layout != null) {
            appenderLayout = new LayoutAdapter(layout);
        } else {
            appenderLayout = SyslogLayout.newBuilder()
                    .setFacility(Facility.toFacility(facility))
                    .setConfiguration(configuration)
                    .build();
        }

        org.apache.logging.log4j.core.Filter fileFilter = buildFilters(level, filter);
        return new AppenderWrapper(SocketAppender.newBuilder()
                .setName(name)
                .setConfiguration(configuration)
                .setLayout(appenderLayout)
                .setFilter(fileFilter)
                .withPort(port.get())
                .withProtocol(protocol)
                .withHost(host.get())
                .build());
    }

    private void resolveSyslogHost(String syslogHost, AtomicReference<String> host, AtomicInteger port) {
        //
        //  If not an unbracketed IPv6 address then
        //      parse as a URL
        //
        String[] parts = syslogHost.split(":");
        if (parts.length == 1) {
            host.set(parts[0]);
            port.set(DEFAULT_PORT);
        } else if (parts.length == 2) {
            host.set(parts[0]);
            port.set(Integer.parseInt(parts[1]));
        } else {
            LOGGER.warn("Invalid {} setting: {}. Using default.", SYSLOG_HOST_PARAM, syslogHost);
            host.set(DEFAULT_HOST);
            port.set(DEFAULT_PORT);
        }
    }
}
