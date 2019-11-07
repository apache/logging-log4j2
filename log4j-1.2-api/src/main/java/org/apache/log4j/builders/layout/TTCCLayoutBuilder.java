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

import org.apache.log4j.Layout;
import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.log4j.builders.BooleanHolder;
import org.apache.log4j.builders.Holder;
import org.apache.log4j.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfigurationFactory.*;

/**
 * Build a Pattern Layout
 */
@Plugin(name = "org.apache.log4j.TTCCLayout", category = CATEGORY)
public class TTCCLayoutBuilder implements LayoutBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final String THREAD_PRINTING_PARAM = "threadprinting";
    private static final String CATEGORY_PREFIXING_PARAM = "categoryprefixing";
    private static final String CONTEXT_PRINTING_PARAM = "contextprinting";
    private static final String DATE_FORMAT_PARAM = "dateformat";
    private static final String TIMEZONE_FORMAT = "timezone";

    @Override
    public Layout parseLayout(Element layoutElement, XmlConfigurationFactory factory) {
        final Holder<Boolean> threadPrinting = new BooleanHolder();
        final Holder<Boolean> categoryPrefixing = new BooleanHolder();
        final Holder<Boolean> contextPrinting = new BooleanHolder();
        final Holder<String> dateFormat = new Holder<>();
        final Holder<String> timezone = new Holder<>();
        forEachElement(layoutElement.getElementsByTagName("param"), (currentElement) -> {
            if (currentElement.getTagName().equals("param")) {
                switch (currentElement.getAttribute(NAME_ATTR).toLowerCase()) {
                    case THREAD_PRINTING_PARAM:
                        threadPrinting.set(Boolean.parseBoolean(currentElement.getAttribute(VALUE_ATTR)));
                        break;
                    case CATEGORY_PREFIXING_PARAM:
                        categoryPrefixing.set(Boolean.parseBoolean(currentElement.getAttribute(VALUE_ATTR)));
                        break;
                    case CONTEXT_PRINTING_PARAM:
                        contextPrinting.set(Boolean.parseBoolean(currentElement.getAttribute(VALUE_ATTR)));
                        break;
                    case DATE_FORMAT_PARAM:
                        dateFormat.set(currentElement.getAttribute(VALUE_ATTR));
                        break;
                    case TIMEZONE_FORMAT:
                        timezone.set(currentElement.getAttribute(VALUE_ATTR));
                        break;
                }
            }
        });
        StringBuilder sb = new StringBuilder();
        if (dateFormat.get() != null) {
            if (RELATIVE.equalsIgnoreCase(dateFormat.get())) {
                sb.append("%r ");
            } else {
                sb.append("%d{").append(dateFormat.get()).append("}");
                if (timezone.get() != null) {
                    sb.append("{").append(timezone.get()).append("}");
                }
                sb.append(" ");
            }
        }
        if (threadPrinting.get()) {
            sb.append("[%t] ");
        }
        sb.append("%p ");
        if (categoryPrefixing.get()) {
            sb.append("%c ");
        }
        if (contextPrinting.get()) {
            sb.append("%notEmpty{%ndc }");
        }
        sb.append("- %m%n");
        return new LayoutWrapper(PatternLayout.newBuilder()
                .withPattern(sb.toString())
                .withConfiguration(factory.getConfiguration())
                .build());
    }
}
