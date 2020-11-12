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
package org.apache.log4j.builders.filter;

import org.apache.log4j.bridge.FilterWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.builders.BooleanHolder;
import org.apache.log4j.builders.Holder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.filter.StringMatchFilter;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfiguration.*;

/**
 * Build a String match filter.
 */
@Plugin(name = "org.apache.log4j.varia.StringMatchFilter", category = CATEGORY)
public class StringMatchFilterBuilder extends AbstractBuilder implements FilterBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String STRING_TO_MATCH = "StringToMatch";
    private static final String ACCEPT_ON_MATCH = "AcceptOnMatch";

    @Override
    public Filter parseFilter(Element filterElement, XmlConfiguration config) {
        final Holder<Boolean> acceptOnMatch = new BooleanHolder();
        final Holder<String> text = new Holder<>();
        forEachElement(filterElement.getElementsByTagName("param"), currentElement -> {
            if (currentElement.getTagName().equals("param")) {
                switch (currentElement.getAttribute(NAME_ATTR)) {
                    case STRING_TO_MATCH:
                        text.set(currentElement.getAttribute(VALUE_ATTR));
                        break;
                    case ACCEPT_ON_MATCH:
                        acceptOnMatch.set(Boolean.parseBoolean(currentElement.getAttribute(VALUE_ATTR)));
                        break;

                }
            }
        });
        return createFilter(text.get(), acceptOnMatch.get());
    }

    @Override
    public Filter parseFilter(PropertiesConfiguration config) {
        String text = getProperty(STRING_TO_MATCH);
        boolean acceptOnMatch = getBooleanProperty(ACCEPT_ON_MATCH);
        return createFilter(text, acceptOnMatch);
    }

    private Filter createFilter(String text, boolean acceptOnMatch) {
        if (text == null) {
            LOGGER.warn("No text provided for StringMatchFilter");
            return null;
        }
        org.apache.logging.log4j.core.Filter.Result onMatch = acceptOnMatch
                ? org.apache.logging.log4j.core.Filter.Result.ACCEPT
                : org.apache.logging.log4j.core.Filter.Result.DENY;
        return new FilterWrapper(StringMatchFilter.newBuilder()
                .setMatchString(text)
                .setOnMatch(onMatch)
                .setOnMismatch(org.apache.logging.log4j.core.Filter.Result.NEUTRAL)
                .build());
    }
}
