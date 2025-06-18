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
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

/**
 * Build a File Appender
 */
@Plugin(name = "org.apache.log4j.RollingFileAppender", category = CATEGORY)
public class RollingFileAppenderBuilder extends AbstractBuilder implements AppenderBuilder {

    private static final String DEFAULT_MAX_SIZE = "10 MB";
    private static final String DEFAULT_MAX_BACKUPS = "1";

    private static final Logger LOGGER = StatusLogger.getLogger();

    public RollingFileAppenderBuilder() {}

    public RollingFileAppenderBuilder(final String prefix, final Properties properties) {
        super(prefix, properties);
    }

    @Override
    public Appender parseAppender(final Element appenderElement, final XmlConfiguration config) {
        final String name = getNameAttribute(appenderElement);
        final AtomicReference<Layout> layout = new AtomicReference<>();
        final AtomicReference<Filter> filter = new AtomicReference<>();
        final AtomicReference<String> fileName = new AtomicReference<>();
        final AtomicBoolean immediateFlush = new AtomicBoolean(true);
        final AtomicBoolean append = new AtomicBoolean(true);
        final AtomicBoolean bufferedIo = new AtomicBoolean();
        final AtomicInteger bufferSize = new AtomicInteger(8192);
        final AtomicReference<String> maxSize = new AtomicReference<>(DEFAULT_MAX_SIZE);
        final AtomicReference<String> maxBackups = new AtomicReference<>(DEFAULT_MAX_BACKUPS);
        final AtomicReference<String> level = new AtomicReference<>();
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
                        case FILE_PARAM:
                            set(FILE_PARAM, currentElement, fileName);
                            break;
                        case APPEND_PARAM:
                            set(APPEND_PARAM, currentElement, append);
                            break;
                        case BUFFERED_IO_PARAM:
                            set(BUFFERED_IO_PARAM, currentElement, bufferedIo);
                            break;
                        case BUFFER_SIZE_PARAM:
                            set(BUFFER_SIZE_PARAM, currentElement, bufferSize);
                            break;
                        case MAX_BACKUP_INDEX:
                            set(MAX_BACKUP_INDEX, currentElement, maxBackups);
                            break;
                        case MAX_SIZE_PARAM:
                            set(MAX_SIZE_PARAM, currentElement, maxSize);
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
                config,
                layout.get(),
                filter.get(),
                append.get(),
                bufferedIo.get(),
                bufferSize.get(),
                immediateFlush.get(),
                fileName.get(),
                level.get(),
                maxSize.get(),
                maxBackups.get());
    }

    @Override
    public Appender parseAppender(
            final String name,
            final String appenderPrefix,
            final String layoutPrefix,
            final String filterPrefix,
            final Properties props,
            final PropertiesConfiguration configuration) {
        final Layout layout = configuration.parseLayout(layoutPrefix, name, props);
        final Filter filter = configuration.parseAppenderFilters(props, filterPrefix, name);
        final String fileName = getProperty(FILE_PARAM);
        final String level = getProperty(THRESHOLD_PARAM);
        final boolean append = getBooleanProperty(APPEND_PARAM, true);
        final boolean immediateFlush = getBooleanProperty(IMMEDIATE_FLUSH_PARAM, true);
        final boolean bufferedIo = getBooleanProperty(BUFFERED_IO_PARAM, false);
        final int bufferSize = getIntegerProperty(BUFFER_SIZE_PARAM, 8192);
        final String maxSize = getProperty(MAX_SIZE_PARAM, DEFAULT_MAX_SIZE);
        final String maxBackups = getProperty(MAX_BACKUP_INDEX, DEFAULT_MAX_BACKUPS);
        return createAppender(
                name,
                configuration,
                layout,
                filter,
                append,
                bufferedIo,
                bufferSize,
                immediateFlush,
                fileName,
                level,
                maxSize,
                maxBackups);
    }

    private Appender createAppender(
            final String name,
            final Log4j1Configuration config,
            final Layout layout,
            final Filter filter,
            final boolean append,
            final boolean bufferedIo,
            final int bufferSize,
            boolean immediateFlush,
            final String fileName,
            final String level,
            final String maxSize,
            final String maxBackups) {
        final org.apache.logging.log4j.core.Layout<?> fileLayout = LayoutAdapter.adapt(layout);
        if (!bufferedIo) {
            immediateFlush = false;
        }
        final org.apache.logging.log4j.core.Filter fileFilter = buildFilters(level, filter);
        if (fileName == null) {
            LOGGER.error("Unable to create RollingFileAppender, no file name provided");
            return null;
        }
        final String filePattern = fileName + ".%i";
        final SizeBasedTriggeringPolicy sizePolicy = SizeBasedTriggeringPolicy.createPolicy(maxSize);
        final CompositeTriggeringPolicy policy = CompositeTriggeringPolicy.createPolicy(sizePolicy);
        final RolloverStrategy strategy = DefaultRolloverStrategy.newBuilder()
                .setConfig(config)
                .setMax(maxBackups)
                .setFileIndex("min")
                .build();
        return AppenderWrapper.adapt(RollingFileAppender.newBuilder()
                .setName(name)
                .setConfiguration(config)
                .setLayout(fileLayout)
                .setFilter(fileFilter)
                .setAppend(append)
                .setBufferedIo(bufferedIo)
                .setBufferSize(bufferSize)
                .setImmediateFlush(immediateFlush)
                .setFileName(fileName)
                .setFilePattern(filePattern)
                .setPolicy(policy)
                .setStrategy(strategy)
                .build());
    }
}
