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

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;


/**
 * Build a File Appender
 */
@Plugin(name = "org.apache.log4j.RollingFileAppender", category = CATEGORY)
public class RollingFileAppenderBuilder extends AbstractBuilder implements AppenderBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();

    public RollingFileAppenderBuilder() {
    }

    public RollingFileAppenderBuilder(String prefix, Properties properties) {
        super(prefix, properties);
    }

    @Override
    public Appender parseAppender(Element appenderElement, XmlConfiguration config) {
        String name = getNameAttribute(appenderElement);
        AtomicReference<Layout> layout = new AtomicReference<>();
        AtomicReference<Filter> filter = new AtomicReference<>();
        AtomicReference<String> fileName = new AtomicReference<>();
        AtomicBoolean immediateFlush = new AtomicBoolean();
        AtomicBoolean append = new AtomicBoolean();
        AtomicBoolean bufferedIo = new AtomicBoolean();
        AtomicInteger bufferSize = new AtomicInteger(8192);
        AtomicReference<String> maxSize = new AtomicReference<>();
        AtomicReference<String> maxBackups = new AtomicReference<>();
        AtomicReference<String> level = new AtomicReference<>();
        forEachElement(appenderElement.getChildNodes(), currentElement -> {
            switch (currentElement.getTagName()) {
                case LAYOUT_TAG:
                    layout.set(config.parseLayout(currentElement));
                    break;
                case FILTER_TAG:
                    filter.set(config.parseFilters(currentElement));
                    break;
                case PARAM_TAG: {
                    switch (getNameAttributeKey(currentElement)) {
                        case FILE_PARAM:
                            fileName.set(getValueAttribute(currentElement));
                            break;
                        case APPEND_PARAM: {
                            String bool = getValueAttribute(currentElement);
                            if (bool != null) {
                                append.set(Boolean.parseBoolean(bool));
                            } else {
                                LOGGER.warn("No value provided for append parameter");
                            }
                            break;
                        }
                        case BUFFERED_IO_PARAM: {
                            String bool = getValueAttribute(currentElement);
                            if (bool != null) {
                                bufferedIo.set(Boolean.parseBoolean(bool));
                            } else {
                                LOGGER.warn("No value provided for bufferedIo parameter");
                            }
                            break;
                        }
                        case BUFFER_SIZE_PARAM: {
                            String size = getValueAttribute(currentElement);
                            if (size != null) {
                                bufferSize.set(Integer.parseInt(size));
                            } else {
                                LOGGER.warn("No value provide for bufferSize parameter");
                            }
                            break;
                        }
                        case MAX_BACKUP_INDEX: {
                            String size = getValueAttribute(currentElement);
                            if (size != null) {
                                maxBackups.set(size);
                            } else {
                                LOGGER.warn("No value provide for maxBackupIndex parameter");
                            }
                            break;
                        }
                        case MAX_SIZE_PARAM: {
                            String size = getValueAttribute(currentElement);
                            if (size != null) {
                                maxSize.set(size);
                            } else {
                                LOGGER.warn("No value provide for bufferSize parameter");
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
                    }
                    break;
                }
            }
        });
        return createAppender(name, config, layout.get(), filter.get(), bufferedIo.get(), immediateFlush.get(),
                fileName.get(), level.get(), maxSize.get(), maxBackups.get());
    }

    @Override
    public Appender parseAppender(final String name, final String appenderPrefix, final String layoutPrefix,
            final String filterPrefix, final Properties props, final PropertiesConfiguration configuration) {
        Layout layout = configuration.parseLayout(layoutPrefix, name, props);
        Filter filter = configuration.parseAppenderFilters(props, filterPrefix, name);
        String fileName = getProperty(FILE_PARAM);
        String level = getProperty(THRESHOLD_PARAM);
        boolean immediateFlush = false;
        boolean bufferedIo = getBooleanProperty(BUFFERED_IO_PARAM);
        String maxSize = getProperty(MAX_SIZE_PARAM);
        String maxBackups = getProperty(MAX_BACKUP_INDEX);
        return createAppender(name, configuration, layout, filter, bufferedIo, immediateFlush, fileName, level, maxSize,
                maxBackups);
    }

    private Appender createAppender(final String name, final Log4j1Configuration config, final Layout layout,
            final Filter filter, final boolean bufferedIo, boolean immediateFlush, final String fileName,
            final String level, final String maxSize, final String maxBackups) {
        org.apache.logging.log4j.core.Layout<?> fileLayout = null;
        if (!bufferedIo) {
            immediateFlush = true;
        }
        if (layout instanceof LayoutWrapper) {
            fileLayout = ((LayoutWrapper) layout).getLayout();
        } else if (layout != null) {
            fileLayout = new LayoutAdapter(layout);
        }
        org.apache.logging.log4j.core.Filter fileFilter = buildFilters(level, filter);
        if (fileName == null) {
            LOGGER.warn("Unable to create File Appender, no file name provided");
            return null;
        }
        String filePattern = fileName +".%i";
        SizeBasedTriggeringPolicy sizePolicy = SizeBasedTriggeringPolicy.createPolicy(maxSize);
        CompositeTriggeringPolicy policy = CompositeTriggeringPolicy.createPolicy(sizePolicy);
        RolloverStrategy strategy = DefaultRolloverStrategy.newBuilder()
                .withConfig(config)
                .withMax(maxBackups)
                .build();
        return new AppenderWrapper(RollingFileAppender.newBuilder()
                .setName(name)
                .setConfiguration(config)
                .setLayout(fileLayout)
                .setFilter(fileFilter)
                .withBufferedIo(bufferedIo)
                .withImmediateFlush(immediateFlush)
                .withFileName(fileName)
                .withFilePattern(filePattern)
                .withPolicy(policy)
                .withStrategy(strategy)
                .build());
    }
}
