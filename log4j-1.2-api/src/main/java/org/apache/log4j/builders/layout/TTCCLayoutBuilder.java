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
package org.apache.log4j.builders.layout;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfiguration.PARAM_TAG;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Layout;
import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.w3c.dom.Element;

/**
 * Build a Pattern Layout
 */
@Plugin(name = "org.apache.log4j.TTCCLayout", category = CATEGORY)
public class TTCCLayoutBuilder extends AbstractBuilder<Layout> implements LayoutBuilder {

    private static final String THREAD_PRINTING_PARAM = "ThreadPrinting";
    private static final String CATEGORY_PREFIXING_PARAM = "CategoryPrefixing";
    private static final String CONTEXT_PRINTING_PARAM = "ContextPrinting";
    private static final String DATE_FORMAT_PARAM = "DateFormat";
    private static final String TIMEZONE_FORMAT = "TimeZone";

    public TTCCLayoutBuilder() {
    }

    public TTCCLayoutBuilder(String prefix, Properties props) {
        super(prefix, props);
    }

    @Override
    public Layout parse(Element layoutElement, XmlConfiguration config) {
        final AtomicBoolean threadPrinting = new AtomicBoolean(Boolean.TRUE);
        final AtomicBoolean categoryPrefixing = new AtomicBoolean(Boolean.TRUE);
        final AtomicBoolean contextPrinting = new AtomicBoolean(Boolean.TRUE);
        final AtomicReference<String> dateFormat = new AtomicReference<>(RELATIVE);
        final AtomicReference<String> timezone = new AtomicReference<>();
        forEachElement(layoutElement.getElementsByTagName("param"), currentElement -> {
            if (currentElement.getTagName().equals(PARAM_TAG)) {
                switch (getNameAttributeKey(currentElement)) {
                    case THREAD_PRINTING_PARAM:
                        threadPrinting.set(getBooleanValueAttribute(currentElement));
                        break;
                    case CATEGORY_PREFIXING_PARAM:
                        categoryPrefixing.set(getBooleanValueAttribute(currentElement));
                        break;
                    case CONTEXT_PRINTING_PARAM:
                        contextPrinting.set(getBooleanValueAttribute(currentElement));
                        break;
                    case DATE_FORMAT_PARAM:
                        dateFormat.set(getValueAttribute(currentElement));
                        break;
                    case TIMEZONE_FORMAT:
                        timezone.set(getValueAttribute(currentElement));
                        break;
                }
            }
        });
        return createLayout(threadPrinting.get(), categoryPrefixing.get(), contextPrinting.get(),
                dateFormat.get(), timezone.get(), config);
    }

    @Override
    public Layout parse(PropertiesConfiguration config) {
        boolean threadPrinting = getBooleanProperty(THREAD_PRINTING_PARAM, true);
        boolean categoryPrefixing = getBooleanProperty(CATEGORY_PREFIXING_PARAM, true);
        boolean contextPrinting = getBooleanProperty(CONTEXT_PRINTING_PARAM, true);
        String dateFormat = getProperty(DATE_FORMAT_PARAM, RELATIVE);
        String timezone = getProperty(TIMEZONE_FORMAT);

        return createLayout(threadPrinting, categoryPrefixing, contextPrinting,
                dateFormat, timezone, config);
    }

    private Layout createLayout(boolean threadPrinting, boolean categoryPrefixing, boolean contextPrinting,
            String dateFormat, String timezone, Log4j1Configuration config) {
        StringBuilder sb = new StringBuilder();
        if (dateFormat != null) {
            if (RELATIVE.equalsIgnoreCase(dateFormat)) {
                sb.append("%r ");
            } else if (!NULL.equalsIgnoreCase(dateFormat)){
                sb.append("%d{").append(dateFormat).append("}");
                if (timezone != null) {
                    sb.append("{").append(timezone).append("}");
                }
                sb.append(" ");
            }
        }
        if (threadPrinting) {
            sb.append("[%t] ");
        }
        sb.append("%p ");
        if (categoryPrefixing) {
            sb.append("%c ");
        }
        if (contextPrinting) {
            sb.append("%notEmpty{%ndc }");
        }
        sb.append("- %m%n");
        return new LayoutWrapper(PatternLayout.newBuilder()
                .withPattern(sb.toString())
                .withConfiguration(config)
                .build());
    }
}
