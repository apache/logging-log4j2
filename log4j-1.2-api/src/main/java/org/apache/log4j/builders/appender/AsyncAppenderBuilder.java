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
import static org.apache.log4j.config.Log4j1Configuration.APPENDER_REF_TAG;
import static org.apache.log4j.config.Log4j1Configuration.THRESHOLD_PARAM;
import static org.apache.log4j.xml.XmlConfiguration.FILTER_TAG;
import static org.apache.log4j.xml.XmlConfiguration.PARAM_TAG;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Appender;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.bridge.FilterAdapter;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.appender.AsyncAppender.Builder;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
import org.w3c.dom.Element;

/**
 * Build an Async Appender
 */
@Plugin(name = "org.apache.log4j.AsyncAppender", category = CATEGORY)
public class AsyncAppenderBuilder extends AbstractBuilder implements AppenderBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String BLOCKING_PARAM = "Blocking";
    private static final String INCLUDE_LOCATION_PARAM = "IncludeLocation";

    public AsyncAppenderBuilder() {}

    public AsyncAppenderBuilder(final String prefix, final Properties props) {
        super(prefix, props);
    }

    @Override
    public Appender parseAppender(final Element appenderElement, final XmlConfiguration config) {
        final String name = getNameAttribute(appenderElement);
        final AtomicReference<List<String>> appenderRefs = new AtomicReference<>(new ArrayList<>());
        final AtomicBoolean blocking = new AtomicBoolean();
        final AtomicBoolean includeLocation = new AtomicBoolean();
        final AtomicReference<String> level = new AtomicReference<>("trace");
        final AtomicInteger bufferSize = new AtomicInteger(1024);
        final AtomicReference<Filter> filter = new AtomicReference<>();
        forEachElement(appenderElement.getChildNodes(), currentElement -> {
            switch (currentElement.getTagName()) {
                case APPENDER_REF_TAG:
                    final Appender appender = config.findAppenderByReference(currentElement);
                    if (appender != null) {
                        appenderRefs.get().add(appender.getName());
                    }
                    break;
                case FILTER_TAG:
                    config.addFilter(filter, currentElement);
                    break;
                case PARAM_TAG: {
                    switch (getNameAttributeKey(currentElement)) {
                        case BUFFER_SIZE_PARAM:
                            set(BUFFER_SIZE_PARAM, currentElement, bufferSize);
                            break;
                        case BLOCKING_PARAM:
                            set(BLOCKING_PARAM, currentElement, blocking);
                            break;
                        case INCLUDE_LOCATION_PARAM:
                            set(INCLUDE_LOCATION_PARAM, currentElement, includeLocation);
                            break;
                        case THRESHOLD_PARAM:
                            set(THRESHOLD_PARAM, currentElement, level);
                            break;
                    }
                    break;
                }
            }
        });
        return createAppender(
                name,
                level.get(),
                appenderRefs.get().toArray(Strings.EMPTY_ARRAY),
                blocking.get(),
                bufferSize.get(),
                includeLocation.get(),
                filter.get(),
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
        final String appenderRef = getProperty(APPENDER_REF_TAG);
        final Filter filter = configuration.parseAppenderFilters(props, filterPrefix, name);
        final boolean blocking = getBooleanProperty(BLOCKING_PARAM);
        final boolean includeLocation = getBooleanProperty(INCLUDE_LOCATION_PARAM);
        final String level = getProperty(THRESHOLD_PARAM);
        final int bufferSize = getIntegerProperty(BUFFER_SIZE_PARAM, 1024);
        if (appenderRef == null) {
            LOGGER.error("No appender references configured for AsyncAppender {}", name);
            return null;
        }
        final Appender appender = configuration.parseAppender(props, appenderRef);
        if (appender == null) {
            LOGGER.error("Cannot locate Appender {}", appenderRef);
            return null;
        }
        return createAppender(
                name, level, new String[] {appenderRef}, blocking, bufferSize, includeLocation, filter, configuration);
    }

    private <T extends Log4j1Configuration> Appender createAppender(
            final String name,
            final String level,
            final String[] appenderRefs,
            final boolean blocking,
            final int bufferSize,
            final boolean includeLocation,
            final Filter filter,
            final T configuration) {
        if (appenderRefs.length == 0) {
            LOGGER.error("No appender references configured for AsyncAppender {}", name);
            return null;
        }
        final Level logLevel = OptionConverter.convertLevel(level, Level.TRACE);
        final AppenderRef[] refs = new AppenderRef[appenderRefs.length];
        int index = 0;
        for (final String appenderRef : appenderRefs) {
            refs[index++] = AppenderRef.createAppenderRef(appenderRef, logLevel, null);
        }
        final Builder builder = AsyncAppender.newBuilder();
        builder.setFilter(FilterAdapter.adapt(filter));
        return AppenderWrapper.adapt(builder.setName(name)
                .setAppenderRefs(refs)
                .setBlocking(blocking)
                .setBufferSize(bufferSize)
                .setIncludeLocation(includeLocation)
                .setConfiguration(configuration)
                .build());
    }
}
