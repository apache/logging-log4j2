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
import org.apache.logging.log4j.core.filter.LevelRangeFilter;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;


import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfigurationFactory.*;

/**
 * Build a Level match failter.
 */
@Plugin(name = "org.apache.log4j.varia.LevelRangeFilter", category = CATEGORY)
public class LevelRangeFilterBuilder implements FilterBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String LEVEL_MAX = "levelmax";
    private static final String LEVEL_MIN = "levelmin";
    private static final String ACCEPT_ON_MATCH = "acceptonmatch";

    @Override
    public Filter parseFilter(Element filterElement, XmlConfigurationFactory factory) {
        final Holder<String> levelMax = new Holder<>();
        final Holder<String> levelMin = new Holder<>();
        final Holder<Boolean> acceptOnMatch = new BooleanHolder();
        forEachElement(filterElement.getElementsByTagName("param"), (currentElement) -> {
            if (currentElement.getTagName().equals("param")) {
                switch (currentElement.getAttribute(NAME_ATTR).toLowerCase()) {
                    case LEVEL_MAX:
                        levelMax.set(currentElement.getAttribute(VALUE_ATTR));
                        break;
                    case LEVEL_MIN:
                        levelMax.set(currentElement.getAttribute(VALUE_ATTR));
                        break;
                    case ACCEPT_ON_MATCH:
                        acceptOnMatch.set(Boolean.parseBoolean(currentElement.getAttribute(VALUE_ATTR)));
                        break;
                }
            }
        });
        Level max = Level.FATAL;
        Level min = Level.TRACE;
        if (levelMax.get() != null) {
            max = Level.toLevel(levelMax.get(), Level.FATAL);
        }
        if (levelMin.get() != null) {
            min = Level.toLevel(levelMin.get(), Level.DEBUG);
        }
        org.apache.logging.log4j.core.Filter.Result onMatch = acceptOnMatch.get() != null && acceptOnMatch.get()
                ? org.apache.logging.log4j.core.Filter.Result.ACCEPT
                : org.apache.logging.log4j.core.Filter.Result.NEUTRAL;

        return new FilterWrapper(LevelRangeFilter.createFilter(min, max, onMatch,
                org.apache.logging.log4j.core.Filter.Result.DENY));
    }
}
