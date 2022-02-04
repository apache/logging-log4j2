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
import static org.apache.log4j.xml.XmlConfiguration.PARAM_TAG;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

/**
 * Build a Console Appender
 */
@Plugin(name = "org.apache.log4j.ConsoleAppender", category = CATEGORY)
public class ConsoleAppenderBuilder extends AbstractBuilder implements AppenderBuilder {

    private static final String SYSTEM_OUT = "System.out";
    private static final String SYSTEM_ERR = "System.err";
    private static final String TARGET_PARAM = "Target";
    private static final String FOLLOW_PARAM = "Follow";

    private static final Logger LOGGER = StatusLogger.getLogger();

    public ConsoleAppenderBuilder() {
    }

    public ConsoleAppenderBuilder(String prefix, Properties props) {
        super(prefix, props);
    }

    @Override
    public Appender parseAppender(final Element appenderElement, final XmlConfiguration config) {
        String name = getNameAttribute(appenderElement);
        AtomicReference<String> target = new AtomicReference<>(SYSTEM_OUT);
        AtomicReference<Layout> layout = new AtomicReference<>();
        AtomicReference<List<Filter>> filters = new AtomicReference<>(new ArrayList<>());
        AtomicReference<String> level = new AtomicReference<>();
        AtomicBoolean follow = new AtomicBoolean();
        AtomicBoolean immediateFlush = new AtomicBoolean(true);
        forEachElement(appenderElement.getChildNodes(), currentElement -> {
            switch (currentElement.getTagName()) {
                case LAYOUT_TAG:
                    layout.set(config.parseLayout(currentElement));
                    break;
                case FILTER_TAG:
                    filters.get().add(config.parseFilters(currentElement));
                    break;
                case PARAM_TAG: {
                    switch (getNameAttributeKey(currentElement)) {
                        case TARGET_PARAM: {
                            String value = getValueAttribute(currentElement);
                            if (value == null) {
                                LOGGER.warn("No value supplied for target parameter. Defaulting to " + SYSTEM_OUT);
                            } else {
                                switch (value) {
                                    case SYSTEM_OUT:
                                        target.set(SYSTEM_OUT);
                                        break;
                                    case SYSTEM_ERR:
                                        target.set(SYSTEM_ERR);
                                        break;
                                    default:
                                        LOGGER.warn("Invalid value \"{}\" for target parameter. Using default of {}", value, SYSTEM_OUT);
                                }
                            }
                            break;
                        }
                        case THRESHOLD_PARAM: {
                            String value = getValueAttribute(currentElement);
                            if (value == null) {
                                LOGGER.warn("No value supplied for Threshold parameter, ignoring.");
                            } else {
                                level.set(value);
                            }
                            break;
                        }
                        case FOLLOW_PARAM: {
                            String value = getValueAttribute(currentElement);
                            if (value == null) {
                                LOGGER.warn("No value supplied for Follow parameter. Using default of {}", follow);
                            } else {
                                follow.set(Boolean.parseBoolean(value));
                            }
                            break;
                        }
                        case IMMEDIATE_FLUSH_PARAM: {
                            String value = getValueAttribute(currentElement);
                            if (value == null) {
                                LOGGER.warn("No value supplied for ImmediateFlush parameter. Using default of {}", immediateFlush);
                            } else {
                                immediateFlush.set(Boolean.parseBoolean(value));
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        });
        Filter head = null;
        Filter current = null;
        for (Filter f : filters.get()) {
            if (head == null) {
                head = f;
            } else {
                current.next = f;
            }
            current = f;
        }
        return createAppender(name, layout.get(), head, level.get(), target.get(), immediateFlush.get(), follow.get(), config);
    }

    @Override
    public Appender parseAppender(final String name, final String appenderPrefix, final String layoutPrefix,
            final String filterPrefix, final Properties props, final PropertiesConfiguration configuration) {
        Layout layout = configuration.parseLayout(layoutPrefix, name, props);
        Filter filter = configuration.parseAppenderFilters(props, filterPrefix, name);
        String level = getProperty(THRESHOLD_PARAM);
        String target = getProperty(TARGET_PARAM);
        boolean follow = getBooleanProperty(FOLLOW_PARAM);
        return createAppender(name, layout, filter, level, target, true, follow, configuration);
    }

    private <T extends Log4j1Configuration> Appender createAppender(String name, Layout layout, Filter filter,
            String level, String target, boolean immediateFlush, boolean follow, T configuration) {
        org.apache.logging.log4j.core.Layout<?> consoleLayout = null;

        if (layout instanceof LayoutWrapper) {
            consoleLayout = ((LayoutWrapper) layout).getLayout();
        } else if (layout != null) {
            consoleLayout = new LayoutAdapter(layout);
        }
        org.apache.logging.log4j.core.Filter consoleFilter = buildFilters(level, filter);
        ConsoleAppender.Target consoleTarget = SYSTEM_ERR.equals(target)
                ? ConsoleAppender.Target.SYSTEM_ERR : ConsoleAppender.Target.SYSTEM_OUT;
        return new AppenderWrapper(ConsoleAppender.newBuilder()
                .setName(name)
                .setTarget(consoleTarget)
                .setFollow(follow)
                .setLayout(consoleLayout)
                .setFilter(consoleFilter)
                .setConfiguration(configuration)
                .setImmediateFlush(immediateFlush)
                .build());
    }
}
