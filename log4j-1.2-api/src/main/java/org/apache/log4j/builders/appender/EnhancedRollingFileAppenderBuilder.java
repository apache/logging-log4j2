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
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

/**
 * Build a File Appender
 */
@Plugin(name = "org.apache.log4j.rolling.RollingFileAppender", category = CATEGORY)
public class EnhancedRollingFileAppenderBuilder extends AbstractBuilder<Appender> implements AppenderBuilder<Appender> {

    private static final String TIME_BASED_ROLLING_POLICY = "org.apache.log4j.rolling.TimeBasedRollingPolicy";
    private static final String FIXED_WINDOW_ROLLING_POLICY = "org.apache.log4j.rolling.FixedWindowRollingPolicy";
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String TRIGGERING_TAG = "triggeringPolicy";
    private static final String ROLLING_TAG = "rollingPolicy";
    private static final int DEFAULT_MIN_INDEX = 1;
    private static final int DEFAULT_MAX_INDEX = 7;
    private static final String ACTIVE_FILE_PARAM = "ActiveFileName";
    private static final String FILE_PATTERN_PARAM = "FileNamePattern";
    private static final String MIN_INDEX_PARAM = "MinIndex";
    private static final String MAX_INDEX_PARAM = "MaxIndex";

    public EnhancedRollingFileAppenderBuilder() {}

    public EnhancedRollingFileAppenderBuilder(final String prefix, final Properties properties) {
        super(prefix, properties);
    }

    private void parseRollingPolicy(
            final Element element,
            final XmlConfiguration configuration,
            final AtomicReference<String> rollingPolicyClassName,
            final AtomicReference<String> activeFileName,
            final AtomicReference<String> fileNamePattern,
            final AtomicInteger minIndex,
            final AtomicInteger maxIndex) {
        rollingPolicyClassName.set(configuration.subst(element.getAttribute("class"), getProperties()));
        forEachElement(element.getChildNodes(), currentElement -> {
            switch (currentElement.getTagName()) {
                case PARAM_TAG:
                    switch (getNameAttributeKey(currentElement)) {
                        case ACTIVE_FILE_PARAM:
                            set(ACTIVE_FILE_PARAM, currentElement, activeFileName);
                            break;
                        case FILE_PATTERN_PARAM:
                            set(FILE_PATTERN_PARAM, currentElement, fileNamePattern);
                            break;
                        case MIN_INDEX_PARAM:
                            set(MIN_INDEX_PARAM, currentElement, minIndex);
                            break;
                        case MAX_INDEX_PARAM:
                            set(MAX_INDEX_PARAM, currentElement, maxIndex);
                    }
            }
        });
    }

    @Override
    public Appender parseAppender(final Element element, final XmlConfiguration configuration) {
        // FileAppender
        final String name = getNameAttribute(element);
        final AtomicReference<Layout> layout = new AtomicReference<>();
        final AtomicReference<Filter> filter = new AtomicReference<>();
        final AtomicReference<String> fileName = new AtomicReference<>();
        final AtomicReference<String> level = new AtomicReference<>();
        final AtomicBoolean immediateFlush = new AtomicBoolean(true);
        final AtomicBoolean append = new AtomicBoolean(true);
        final AtomicBoolean bufferedIo = new AtomicBoolean();
        final AtomicInteger bufferSize = new AtomicInteger(8192);
        // specific to RollingFileAppender
        final AtomicReference<String> rollingPolicyClassName = new AtomicReference<>();
        final AtomicReference<String> activeFileName = new AtomicReference<>();
        final AtomicReference<String> fileNamePattern = new AtomicReference<>();
        final AtomicInteger minIndex = new AtomicInteger(DEFAULT_MIN_INDEX);
        final AtomicInteger maxIndex = new AtomicInteger(DEFAULT_MAX_INDEX);
        final AtomicReference<TriggeringPolicy> triggeringPolicy = new AtomicReference<>();
        forEachElement(element.getChildNodes(), currentElement -> {
            switch (currentElement.getTagName()) {
                case ROLLING_TAG:
                    parseRollingPolicy(
                            currentElement,
                            configuration,
                            rollingPolicyClassName,
                            activeFileName,
                            fileNamePattern,
                            minIndex,
                            maxIndex);
                    break;
                case TRIGGERING_TAG:
                    triggeringPolicy.set(configuration.parseTriggeringPolicy(currentElement));
                    break;
                case LAYOUT_TAG:
                    layout.set(configuration.parseLayout(currentElement));
                    break;
                case FILTER_TAG:
                    configuration.addFilter(filter, currentElement);
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
                layout.get(),
                filter.get(),
                fileName.get(),
                level.get(),
                immediateFlush.get(),
                append.get(),
                bufferedIo.get(),
                bufferSize.get(),
                rollingPolicyClassName.get(),
                activeFileName.get(),
                fileNamePattern.get(),
                minIndex.get(),
                maxIndex.get(),
                triggeringPolicy.get(),
                configuration);
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
        final String level = getProperty(THRESHOLD_PARAM);
        final String fileName = getProperty(FILE_PARAM);
        final boolean append = getBooleanProperty(APPEND_PARAM, true);
        final boolean immediateFlush = getBooleanProperty(IMMEDIATE_FLUSH_PARAM, true);
        final boolean bufferedIo = getBooleanProperty(BUFFERED_IO_PARAM, false);
        final int bufferSize = Integer.parseInt(getProperty(BUFFER_SIZE_PARAM, "8192"));
        final String rollingPolicyClassName = getProperty(ROLLING_TAG);
        final int minIndex = getIntegerProperty(ROLLING_TAG + "." + MIN_INDEX_PARAM, DEFAULT_MIN_INDEX);
        final int maxIndex = getIntegerProperty(ROLLING_TAG + "." + MAX_INDEX_PARAM, DEFAULT_MAX_INDEX);
        final String activeFileName = getProperty(ROLLING_TAG + "." + ACTIVE_FILE_PARAM);
        final String fileNamePattern = getProperty(ROLLING_TAG + "." + FILE_PATTERN_PARAM);
        final TriggeringPolicy triggeringPolicy =
                configuration.parseTriggeringPolicy(props, appenderPrefix + "." + TRIGGERING_TAG);
        return createAppender(
                name,
                layout,
                filter,
                fileName,
                level,
                immediateFlush,
                append,
                bufferedIo,
                bufferSize,
                rollingPolicyClassName,
                activeFileName,
                fileNamePattern,
                minIndex,
                maxIndex,
                triggeringPolicy,
                configuration);
    }

    private Appender createAppender(
            final String name,
            final Layout layout,
            final Filter filter,
            final String fileName,
            final String level,
            final boolean immediateFlush,
            final boolean append,
            final boolean bufferedIo,
            final int bufferSize,
            final String rollingPolicyClassName,
            final String activeFileName,
            final String fileNamePattern,
            final int minIndex,
            final int maxIndex,
            final TriggeringPolicy triggeringPolicy,
            final Configuration configuration) {
        final org.apache.logging.log4j.core.Layout<?> fileLayout = LayoutAdapter.adapt(layout);
        final boolean actualImmediateFlush = bufferedIo ? false : immediateFlush;
        final org.apache.logging.log4j.core.Filter fileFilter = buildFilters(level, filter);
        if (rollingPolicyClassName == null) {
            LOGGER.error("Unable to create RollingFileAppender, no rolling policy provided.");
            return null;
        }
        final String actualFileName = activeFileName != null ? activeFileName : fileName;
        if (actualFileName == null) {
            LOGGER.error("Unable to create RollingFileAppender, no file name provided.");
            return null;
        }
        if (fileNamePattern == null) {
            LOGGER.error("Unable to create RollingFileAppender, no file name pattern provided.");
            return null;
        }
        final DefaultRolloverStrategy.Builder rolloverStrategyBuilder = DefaultRolloverStrategy.newBuilder();
        switch (rollingPolicyClassName) {
            case FIXED_WINDOW_ROLLING_POLICY:
                rolloverStrategyBuilder.withMin(Integer.toString(minIndex)).withMax(Integer.toString(maxIndex));
                break;
            case TIME_BASED_ROLLING_POLICY:
                break;
            default:
                LOGGER.warn("Unsupported rolling policy: {}", rollingPolicyClassName);
        }
        final TriggeringPolicy actualTriggeringPolicy;
        if (triggeringPolicy != null) {
            actualTriggeringPolicy = triggeringPolicy;
        } else if (rollingPolicyClassName.equals(TIME_BASED_ROLLING_POLICY)) {
            actualTriggeringPolicy = TimeBasedTriggeringPolicy.newBuilder().build();
        } else {
            LOGGER.error("Unable to create RollingFileAppender, no triggering policy provided.");
            return null;
        }
        return AppenderWrapper.adapt(RollingFileAppender.newBuilder()
                .withAppend(append)
                .setBufferedIo(bufferedIo)
                .setBufferSize(bufferedIo ? bufferSize : 0)
                .setConfiguration(configuration)
                .withFileName(actualFileName)
                .withFilePattern(fileNamePattern)
                .setFilter(fileFilter)
                .setImmediateFlush(actualImmediateFlush)
                .setLayout(fileLayout)
                .setName(name)
                .withPolicy(actualTriggeringPolicy)
                .withStrategy(rolloverStrategyBuilder.build())
                .build());
    }
}
