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
import org.apache.log4j.builders.Holder;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfigurationFactory.*;

/**
 * Build a Console Appender
 */
@Plugin(name = "org.apache.log4j.ConsoleAppender", category = CATEGORY)
public class ConsoleAppenderBuilder implements AppenderBuilder {
    private static final String SYSTEM_OUT = "System.out";
    private static final String SYSTEM_ERR = "System.err";
    private static final String TARGET = "target";

    private static final Logger LOGGER = StatusLogger.getLogger();

    @Override
    public Appender parseAppender(Element appenderElement, XmlConfigurationFactory factory) {
        String name = appenderElement.getAttribute(XmlConfigurationFactory.NAME_ATTR);
        Holder<String> target = new Holder<>(SYSTEM_OUT);
        Holder<Layout> layout = new Holder<>();
        Holder<Filter> filter = new Holder<>();
        forEachElement(appenderElement.getChildNodes(), (currentElement) -> {
            switch (currentElement.getTagName()) {
                case LAYOUT_TAG:
                    layout.set(factory.parseLayout(currentElement));
                    break;
                case FILTER_TAG:
                    filter.set(factory.parseFilters(currentElement));
                    break;
                case PARAM_TAG: {
                    if (currentElement.getAttribute(NAME_ATTR).equalsIgnoreCase(TARGET)) {
                        String value = currentElement.getAttribute(VALUE_ATTR);
                        if (value == null) {
                            LOGGER.warn("No value supplied for target parameter. Defaulting to System.out.");
                        } else {
                            switch (value) {
                                case SYSTEM_OUT:
                                    target.set(SYSTEM_OUT);
                                    break;
                                case SYSTEM_ERR:
                                    target.set(SYSTEM_ERR);
                                    break;
                                default:
                                    LOGGER.warn("Invalid value \"{}\" for target parameter. Using default of System.out",
                                            value);
                            }
                        }
                    }
                    break;
                }
            }
        });
        org.apache.logging.log4j.core.Layout<?> consoleLayout = null;
        org.apache.logging.log4j.core.Filter consoleFilter = null;

        if (layout.get() instanceof LayoutWrapper) {
            consoleLayout = ((LayoutWrapper) layout.get()).getLayout();
        } else if (layout.get() != null) {
            consoleLayout = new LayoutAdapter(layout.get());
        }
        if (filter.get() != null) {
            if (filter.get() instanceof FilterWrapper) {
                consoleFilter = ((FilterWrapper) filter.get()).getFilter();
            } else {
                consoleFilter = new FilterAdapter(filter.get());
            }
        }
        ConsoleAppender.Target consoleTarget = SYSTEM_ERR.equals(target.get())
                ? ConsoleAppender.Target.SYSTEM_ERR : ConsoleAppender.Target.SYSTEM_OUT;
        return new AppenderWrapper(ConsoleAppender.newBuilder()
                .setName(name)
                .setTarget(consoleTarget)
                .setLayout(consoleLayout)
                .setFilter(consoleFilter)
                .setConfiguration(factory.getConfiguration())
                .build());
    }
}
