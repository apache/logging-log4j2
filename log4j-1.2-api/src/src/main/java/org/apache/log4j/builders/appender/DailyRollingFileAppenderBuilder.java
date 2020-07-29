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
import org.apache.log4j.bridge.FilterAdapter;
import org.apache.log4j.bridge.FilterWrapper;
import org.apache.log4j.bridge.LayoutAdapter;
import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.log4j.builders.BooleanHolder;
import org.apache.log4j.builders.Holder;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfigurationFactory.*;

/**
 * Build a Daily Rolling File Appender
 */
@Plugin(name = "org.apache.log4j.DailyRollingFileAppender", category = CATEGORY)
public class DailyRollingFileAppenderBuilder implements AppenderBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();

    @Override
    public Appender parseAppender(Element appenderElement, XmlConfigurationFactory factory) {
        String name = appenderElement.getAttribute(NAME_ATTR);
        Holder<Layout> layout = new Holder<>();
        Holder<Filter> filter = new Holder<>();
        Holder<String> fileName = new Holder<>();
        Holder<Boolean> immediateFlush = new BooleanHolder();
        Holder<Boolean> append = new BooleanHolder();
        Holder<Boolean> bufferedIo = new BooleanHolder();
        Holder<Integer> bufferSize = new Holder<>(8192);
        forEachElement(appenderElement.getChildNodes(), (currentElement) -> {
            switch (currentElement.getTagName()) {
                case LAYOUT_TAG:
                    layout.set(factory.parseLayout(currentElement));
                    break;
                case FILTER_TAG:
                    filter.set(factory.parseFilters(currentElement));
                    break;
                case PARAM_TAG: {
                    switch (currentElement.getAttribute(NAME_ATTR).toLowerCase()) {
                        case FILE_PARAM:
                            fileName.set(currentElement.getAttribute(VALUE_ATTR));
                            break;
                        case APPEND_PARAM: {
                            String bool = currentElement.getAttribute(VALUE_ATTR);
                            if (bool != null) {
                                append.set(Boolean.parseBoolean(bool));
                            } else {
                                LOGGER.warn("No value provided for append parameter");
                            }
                            break;
                        }
                        case BUFFERED_IO_PARAM: {
                            String bool = currentElement.getAttribute(VALUE_ATTR);
                            if (bool != null) {
                                bufferedIo.set(Boolean.parseBoolean(bool));
                            } else {
                                LOGGER.warn("No value provided for bufferedIo parameter");
                            }
                            break;
                        }
                        case BUFFER_SIZE_PARAM:
                            String size = currentElement.getAttribute(VALUE_ATTR);
                            if (size != null) {
                                bufferSize.set(Integer.parseInt(size));
                            } else {
                                LOGGER.warn("No value provide for bufferSize parameter");
                            }
                            break;
                    }
                    break;
                }
            }
        });

        org.apache.logging.log4j.core.Layout<?> fileLayout = null;
        org.apache.logging.log4j.core.Filter fileFilter = null;
        if (bufferedIo.get()) {
            immediateFlush.set(Boolean.TRUE);
        }
        if (layout.get() instanceof LayoutWrapper) {
            fileLayout = ((LayoutWrapper) layout.get()).getLayout();
        } else if (layout.get() != null) {
            fileLayout = new LayoutAdapter(layout.get());
        }
        if (filter.get() != null) {
            if (filter.get() instanceof FilterWrapper) {
                fileFilter = ((FilterWrapper) filter.get()).getFilter();
            } else {
                fileFilter = new FilterAdapter(filter.get());
            }
        }
        if (fileName.get() == null) {
            LOGGER.warn("Unable to create File Appender, no file name provided");
            return null;
        }
        String filePattern = fileName.get() +"%d{yyy-MM-dd}";
        TriggeringPolicy policy = TimeBasedTriggeringPolicy.newBuilder().withModulate(true).build();
        RolloverStrategy strategy = DefaultRolloverStrategy.newBuilder()
                .withConfig(factory.getConfiguration())
                .withMax(Integer.toString(Integer.MAX_VALUE))
                .build();
        return new AppenderWrapper(RollingFileAppender.newBuilder()
                .setName(name)
                .setConfiguration(factory.getConfiguration())
                .setLayout(fileLayout)
                .setFilter(fileFilter)
                .withFileName(fileName.get())
                .withImmediateFlush(immediateFlush.get())
                .withFilePattern(filePattern)
                .withPolicy(policy)
                .withStrategy(strategy)
                .build());
    }
}
