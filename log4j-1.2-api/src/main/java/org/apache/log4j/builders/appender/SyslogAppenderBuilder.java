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

import java.io.Serializable;
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
import org.apache.log4j.layout.Log4j1SyslogLayout;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.SyslogAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
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
    private static final String FACILITY_PRINTING_PARAM = "FacilityPrinting";
    private static final String HEADER_PARAM = "Header";
    private static final String PROTOCOL_PARAM = "Protocol";
    private static final String SYSLOG_HOST_PARAM = "SyslogHost";

    public SyslogAppenderBuilder() {}

    public SyslogAppenderBuilder(final String prefix, final Properties props) {
        super(prefix, props);
    }

    @Override
    public Appender parseAppender(final Element appenderElement, final XmlConfiguration config) {
        final String name = getNameAttribute(appenderElement);
        final AtomicReference<Layout> layout = new AtomicReference<>();
        final AtomicReference<Filter> filter = new AtomicReference<>();
        final AtomicReference<String> facility = new AtomicReference<>();
        final AtomicReference<String> level = new AtomicReference<>();
        final AtomicReference<String> host = new AtomicReference<>();
        final AtomicReference<Protocol> protocol = new AtomicReference<>(Protocol.TCP);
        final AtomicBoolean header = new AtomicBoolean(false);
        final AtomicBoolean facilityPrinting = new AtomicBoolean(false);
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
                        case FACILITY_PARAM:
                            set(FACILITY_PARAM, currentElement, facility);
                            break;
                        case FACILITY_PRINTING_PARAM:
                            set(FACILITY_PRINTING_PARAM, currentElement, facilityPrinting);
                            break;
                        case HEADER_PARAM:
                            set(HEADER_PARAM, currentElement, header);
                            break;
                        case PROTOCOL_PARAM:
                            protocol.set(Protocol.valueOf(getValueAttribute(currentElement, Protocol.TCP.name())));
                            break;
                        case SYSLOG_HOST_PARAM:
                            set(SYSLOG_HOST_PARAM, currentElement, host);
                            break;
                        case THRESHOLD_PARAM:
                            set(THRESHOLD_PARAM, currentElement, level);
                            break;
                    }
                    break;
            }
        });

        return createAppender(
                name,
                config,
                layout.get(),
                facility.get(),
                filter.get(),
                host.get(),
                level.get(),
                protocol.get(),
                header.get(),
                facilityPrinting.get());
    }

    @Override
    public Appender parseAppender(
            final String name,
            final String appenderPrefix,
            final String layoutPrefix,
            final String filterPrefix,
            final Properties props,
            final PropertiesConfiguration configuration) {
        final Filter filter = configuration.parseAppenderFilters(props, filterPrefix, name);
        final Layout layout = configuration.parseLayout(layoutPrefix, name, props);
        final String level = getProperty(THRESHOLD_PARAM);
        final String facility = getProperty(FACILITY_PARAM, DEFAULT_FACILITY);
        final boolean facilityPrinting = getBooleanProperty(FACILITY_PRINTING_PARAM, false);
        final boolean header = getBooleanProperty(HEADER_PARAM, false);
        final String protocol = getProperty(PROTOCOL_PARAM, Protocol.TCP.name());
        final String syslogHost = getProperty(SYSLOG_HOST_PARAM, DEFAULT_HOST + ":" + DEFAULT_PORT);

        return createAppender(
                name,
                configuration,
                layout,
                facility,
                filter,
                syslogHost,
                level,
                Protocol.valueOf(protocol),
                header,
                facilityPrinting);
    }

    private Appender createAppender(
            final String name,
            final Log4j1Configuration configuration,
            final Layout layout,
            final String facility,
            final Filter filter,
            final String syslogHost,
            final String level,
            final Protocol protocol,
            final boolean header,
            final boolean facilityPrinting) {
        final AtomicReference<String> host = new AtomicReference<>();
        final AtomicInteger port = new AtomicInteger();
        resolveSyslogHost(syslogHost, host, port);
        final org.apache.logging.log4j.core.Layout<? extends Serializable> messageLayout = LayoutAdapter.adapt(layout);
        final Log4j1SyslogLayout appenderLayout = Log4j1SyslogLayout.newBuilder()
                .setHeader(header)
                .setFacility(Facility.toFacility(facility))
                .setFacilityPrinting(facilityPrinting)
                .setMessageLayout(messageLayout)
                .build();

        final org.apache.logging.log4j.core.Filter fileFilter = buildFilters(level, filter);
        return AppenderWrapper.adapt(SyslogAppender.newSyslogAppenderBuilder()
                .setName(name)
                .setConfiguration(configuration)
                .setLayout(appenderLayout)
                .setFilter(fileFilter)
                .setPort(port.get())
                .setProtocol(protocol)
                .setHost(host.get())
                .build());
    }

    private void resolveSyslogHost(
            final String syslogHost, final AtomicReference<String> host, final AtomicInteger port) {
        //
        //  If not an unbracketed IPv6 address then
        //      parse as a URL
        //
        final String[] parts = syslogHost != null ? syslogHost.split(":") : Strings.EMPTY_ARRAY;
        if (parts.length == 1) {
            host.set(parts[0]);
            port.set(DEFAULT_PORT);
        } else if (parts.length == 2) {
            host.set(parts[0]);
            port.set(Integer.parseInt(parts[1].trim()));
        } else {
            LOGGER.warn("Invalid {} setting: {}. Using default.", SYSLOG_HOST_PARAM, syslogHost);
            host.set(DEFAULT_HOST);
            port.set(DEFAULT_PORT);
        }
    }
}
