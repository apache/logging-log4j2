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

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.bridge.LayoutAdapter;
import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.builders.Holder;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.SyslogAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.SyslogLayout;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

import java.util.Properties;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.config.Log4j1Configuration.THRESHOLD_PARAM;
import static org.apache.log4j.xml.XmlConfiguration.FILTER_TAG;
import static org.apache.log4j.xml.XmlConfiguration.LAYOUT_TAG;
import static org.apache.log4j.xml.XmlConfiguration.NAME_ATTR;
import static org.apache.log4j.xml.XmlConfiguration.PARAM_TAG;
import static org.apache.log4j.xml.XmlConfiguration.VALUE_ATTR;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

/**
 * Build a File Appender
 */
@Plugin(name = "org.apache.log4j.net.SyslogAppender", category = CATEGORY)
public class SyslogAppenderBuilder extends AbstractBuilder implements AppenderBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String FACILITY_PARAM = "Facility";
    private static final String SYSLOG_HOST_PARAM = "SyslogHost";
    private static int SYSLOG_PORT = 512;

    public SyslogAppenderBuilder() {
    }

    public SyslogAppenderBuilder(String prefix, Properties props) {
        super(prefix, props);
    }

    @Override
    public Appender parseAppender(Element appenderElement, XmlConfiguration config) {
        String name = appenderElement.getAttribute(NAME_ATTR);
        Holder<Layout> layout = new Holder<>();
        Holder<Filter> filter = new Holder<>();
        Holder<String> facility = new Holder<>();
        Holder<String> level = new Holder<>();
        Holder<String> host = new Holder<>();
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
                    }
                    break;
                }
            }
        });

        return createAppender(name, config, layout.get(), facility.get(), filter.get(), host.get(), level.get());
    }


    @Override
    public Appender parseAppender(final String name, final String appenderPrefix, final String layoutPrefix,
            final String filterPrefix, final Properties props, final PropertiesConfiguration configuration) {
        Filter filter = configuration.parseAppenderFilters(props, filterPrefix, name);
        Layout layout = configuration.parseLayout(layoutPrefix, name, props);
        String level = getProperty(THRESHOLD_PARAM);
        String facility = getProperty(FACILITY_PARAM, "LOCAL0");
        String syslogHost = getProperty(SYSLOG_HOST_PARAM, "localhost:514");

        return createAppender(name, configuration, layout, facility, filter, syslogHost, level);
    }

    private Appender createAppender(final String name, final Log4j1Configuration configuration, Layout layout,
            String facility, final Filter filter, final String syslogHost, final String level) {
        Holder<String> host = new Holder<>();
        Holder<Integer> port = new Holder<>();
        resolveSyslogHost(syslogHost, host, port);
        org.apache.logging.log4j.core.Layout appenderLayout;
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
        return new AppenderWrapper(SyslogAppender.newBuilder()
                .setName(name)
                .setConfiguration(configuration)
                .setLayout(appenderLayout)
                .setFilter(fileFilter)
                .withPort(port.get())
                .withProtocol(Protocol.TCP)
                .withHost(host.get())
                .build());
    }

    private void resolveSyslogHost(String syslogHost, Holder<String> host, Holder<Integer> port) {
        int urlPort = -1;

        //
        //  If not an unbracketed IPv6 address then
        //      parse as a URL
        //
        String[] parts = syslogHost.split(":");
        if (parts.length == 1) {
            host.set(parts[0]);
            port.set(SYSLOG_PORT);
        } else if (parts.length == 2) {
            host.set(parts[0]);
            port.set(Integer.parseInt(parts[1]));
        } else {
            LOGGER.warn("Invalid syslogHost setting: {}. Using default", syslogHost);
            host.set("localhost");
            port.set(SYSLOG_PORT);
        }
    }
}
