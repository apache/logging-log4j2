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
import org.apache.log4j.builders.BooleanHolder;
import org.apache.log4j.builders.Holder;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

import java.util.Properties;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.config.Log4j1Configuration.THRESHOLD_PARAM;
import static org.apache.log4j.xml.XmlConfiguration.*;

/**
 * Build a Daily Rolling File Appender
 */
@Plugin(name = "org.apache.log4j.DailyRollingFileAppender", category = CATEGORY)
public class DailyRollingFileAppenderBuilder extends AbstractBuilder implements AppenderBuilder {

    private static final String DEFAULT_DATE_PATTERN = ".yyyy-MM-dd";
    private static final String DATE_PATTERN_PARAM = "DatePattern";

    private static final Logger LOGGER = StatusLogger.getLogger();

    public DailyRollingFileAppenderBuilder() {
    }

    public DailyRollingFileAppenderBuilder(final String prefix, final Properties props) {
        super(prefix, props);
    }

    @Override
    public Appender parseAppender(final Element appenderElement, final XmlConfiguration config) {
        String name = getNameAttribute(appenderElement);
        Holder<Layout> layout = new Holder<>();
        Holder<Filter> filter = new Holder<>();
        Holder<String> fileName = new Holder<>();
        Holder<String> level = new Holder<>();
        Holder<Boolean> immediateFlush = new BooleanHolder(true);
        Holder<Boolean> append = new BooleanHolder(true);
        Holder<Boolean> bufferedIo = new BooleanHolder();
        Holder<Integer> bufferSize = new Holder<>(8192);
        Holder<String> datePattern = new Holder<>(DEFAULT_DATE_PATTERN);
        forEachElement(appenderElement.getChildNodes(), (currentElement) -> {
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
                            setString(FILE_PARAM, currentElement, fileName);
                            break;
                        case APPEND_PARAM:
                            setBoolean(APPEND_PARAM, currentElement, append);
                            break;
                        case BUFFERED_IO_PARAM:
                            setBoolean(BUFFERED_IO_PARAM, currentElement, bufferedIo);
                            break;
                        case BUFFER_SIZE_PARAM:
                            setInteger(BUFFER_SIZE_PARAM, currentElement, bufferSize);
                            break;
                        case THRESHOLD_PARAM:
                            setString(THRESHOLD_PARAM, currentElement, level);
                            break;
                        case DATE_PATTERN_PARAM:
                            setString(DATE_PATTERN_PARAM, currentElement, datePattern);
                            break;
                        case IMMEDIATE_FLUSH_PARAM:
                            setBoolean(IMMEDIATE_FLUSH_PARAM, currentElement, immediateFlush);
                            break;
                    }
                    break;
            }
        });
        return createAppender(name, layout.get(), filter.get(), fileName.get(), append.get(), immediateFlush.get(),
                level.get(), bufferedIo.get(), bufferSize.get(), datePattern.get(), config,
                config.getComponent(Clock.KEY));
    }

    @Override
    public Appender parseAppender(final String name, final String appenderPrefix, final String layoutPrefix,
            final String filterPrefix, final Properties props, final PropertiesConfiguration configuration) {
        final Layout layout = configuration.parseLayout(layoutPrefix, name, props);
        final Filter filter = configuration.parseAppenderFilters(props, filterPrefix, name);
        final String fileName = getProperty(FILE_PARAM);
        final String level = getProperty(THRESHOLD_PARAM);
        final boolean append = getBooleanProperty(APPEND_PARAM, true);
        final boolean immediateFlush = getBooleanProperty(IMMEDIATE_FLUSH_PARAM, true);
        final boolean bufferedIo = getBooleanProperty(BUFFERED_IO_PARAM, false);
        final int bufferSize = getIntegerProperty(BUFFER_SIZE_PARAM, 8192);
        final String datePattern = getProperty(DATE_PATTERN_PARAM, DEFAULT_DATE_PATTERN);
        return createAppender(name, layout, filter, fileName, append, immediateFlush, level, bufferedIo, bufferSize,
                datePattern, configuration, configuration.getComponent(Clock.KEY));
    }

    private <T extends Log4j1Configuration> Appender createAppender(final String name, final Layout layout,
            final Filter filter, final String fileName, final boolean append, boolean immediateFlush,
            final String level, final boolean bufferedIo, final int bufferSize, String datePattern,
            final T configuration, final Clock clock) {

        org.apache.logging.log4j.core.Layout<?> fileLayout = null;
        if (bufferedIo) {
            immediateFlush = false;
        }
        if (layout instanceof LayoutWrapper) {
            fileLayout = ((LayoutWrapper) layout).getLayout();
        } else if (layout != null) {
            fileLayout = new LayoutAdapter(layout);
        }
        final org.apache.logging.log4j.core.Filter fileFilter = buildFilters(level, filter);
        if (fileName == null) {
            LOGGER.warn("Unable to create File Appender, no file name provided");
            return null;
        }
        final String filePattern = fileName + "%d{" + datePattern + "}";
        final TriggeringPolicy timePolicy = TimeBasedTriggeringPolicy.newBuilder().setClock(clock).setModulate(true).build();
        final TriggeringPolicy policy = CompositeTriggeringPolicy.createPolicy(timePolicy);
        final RolloverStrategy strategy = DefaultRolloverStrategy.newBuilder()
                .setConfig(configuration)
                .setMax(Integer.toString(Integer.MAX_VALUE))
                .build();
        return new AppenderWrapper(RollingFileAppender.newBuilder()
                .setName(name)
                .setAppend(append)
                .setConfiguration(configuration)
                .setLayout(fileLayout)
                .setFilter(fileFilter)
                .setFileName(fileName)
                .setAppend(append)
                .setBufferedIo(bufferedIo)
                .setBufferSize(bufferSize)
                .setImmediateFlush(immediateFlush)
                .setFilePattern(filePattern)
                .setPolicy(policy)
                .setStrategy(strategy)
                .build());
    }
}
