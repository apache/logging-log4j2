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
package org.apache.log4j.builders;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfiguration.FILTER_TAG;
import static org.apache.log4j.xml.XmlConfiguration.LAYOUT_TAG;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.bridge.LayoutAdapter;
import org.apache.log4j.builders.appender.AppenderBuilder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.w3c.dom.Element;

/**
 * Builder for the native Log4j 2.x list appender to be used in the tests.
 */
@Plugin(name = "org.apache.logging.log4j.test.appender.ListAppender", category = CATEGORY)
public class Log4j2ListAppenderBuilder extends AbstractBuilder implements AppenderBuilder {

    public Log4j2ListAppenderBuilder() {}

    public Log4j2ListAppenderBuilder(final String prefix, final Properties props) {
        super(prefix, props);
    }

    @Override
    public Appender parseAppender(final Element element, final XmlConfiguration configuration) {
        final String name = getNameAttribute(element);
        final AtomicReference<Layout> layout = new AtomicReference<>();
        final AtomicReference<Filter> filter = new AtomicReference<>();
        forEachElement(element.getChildNodes(), currentElement -> {
            switch (currentElement.getTagName()) {
                case LAYOUT_TAG:
                    layout.set(configuration.parseLayout(currentElement));
                    break;
                case FILTER_TAG:
                    configuration.addFilter(filter, currentElement);
                    break;
                default:
            }
        });
        return createAppender(name, layout.get(), filter.get());
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
        return createAppender(name, layout, filter);
    }

    private Appender createAppender(final String name, final Layout layout, final Filter filter) {
        final org.apache.logging.log4j.core.Layout<?> log4j2Layout = LayoutAdapter.adapt(layout);
        return AppenderWrapper.adapt(ListAppender.newBuilder()
                .setName(name)
                .setLayout(log4j2Layout)
                .setFilter(AbstractBuilder.buildFilters(null, filter))
                .build());
    }
}
