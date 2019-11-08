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
import org.apache.log4j.builders.BooleanHolder;
import org.apache.log4j.builders.Holder;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.filter.LevelMatchFilter;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfigurationFactory.*;
import static org.apache.log4j.xml.XmlConfigurationFactory.VALUE_ATTR;

/**
 * Build a Level match failter.
 */
@Plugin(name = "org.apache.log4j.varia.LevelMatchFilter", category = CATEGORY)
public class LevelMatchFilterBuilder implements FilterBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String LEVEL = "level";
    private static final String ACCEPT_ON_MATCH = "acceptonmatch";

    @Override
    public Filter parseFilter(Element filterElement, XmlConfigurationFactory factory) {
        final Holder<String> level = new Holder<>();
        final Holder<Boolean> acceptOnMatch = new BooleanHolder();
        forEachElement(filterElement.getElementsByTagName("param"), (currentElement) -> {
            if (currentElement.getTagName().equals("param")) {
                switch (currentElement.getAttribute(NAME_ATTR).toLowerCase()) {
                    case LEVEL:
                        level.set(currentElement.getAttribute(VALUE_ATTR));
                        break;
                    case ACCEPT_ON_MATCH:
                        acceptOnMatch.set(Boolean.parseBoolean(currentElement.getAttribute(VALUE_ATTR)));
                        break;
                }
            }
        });
        Level lvl = Level.ERROR;
        if (level.get() != null) {
            lvl = Level.toLevel(level.get(), Level.ERROR);
        }
        org.apache.logging.log4j.core.Filter.Result onMatch = acceptOnMatch.get() != null && acceptOnMatch.get()
                ? org.apache.logging.log4j.core.Filter.Result.ACCEPT
                : org.apache.logging.log4j.core.Filter.Result.DENY;
        return new FilterWrapper(LevelMatchFilter.newBuilder()
                .setLevel(lvl)
                .setOnMatch(onMatch)
                .setOnMismatch(org.apache.logging.log4j.core.Filter.Result.NEUTRAL)
                .build());
    }
}
