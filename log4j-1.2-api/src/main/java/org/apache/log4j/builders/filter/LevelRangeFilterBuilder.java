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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.filter.LevelRangeFilter;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;


import java.util.Properties;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfiguration.*;

/**
 * Build a Level match failter.
 */
@Plugin(name = "org.apache.log4j.varia.LevelRangeFilter", category = CATEGORY)
public class LevelRangeFilterBuilder extends AbstractBuilder implements FilterBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String LEVEL_MAX = "LevelMax";
    private static final String LEVEL_MIN = "LevelMin";
    private static final String ACCEPT_ON_MATCH = "AcceptOnMatch";

    public LevelRangeFilterBuilder() {
    }

    public LevelRangeFilterBuilder(String prefix, Properties props) {
        super(prefix, props);
    }

    @Override
    public Filter parseFilter(Element filterElement, XmlConfiguration config) {
        final Holder<String> levelMax = new Holder<>();
        final Holder<String> levelMin = new Holder<>();
        final Holder<Boolean> acceptOnMatch = new BooleanHolder();
        forEachElement(filterElement.getElementsByTagName("param"), currentElement -> {
            if (currentElement.getTagName().equals("param")) {
                switch (currentElement.getAttribute(NAME_ATTR)) {
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
        return createFilter(levelMax.get(), levelMin.get(), acceptOnMatch.get());
    }

    @Override
    public Filter parseFilter(PropertiesConfiguration config) {
        String levelMax = getProperty(LEVEL_MAX);
        String levelMin = getProperty(LEVEL_MIN);
        boolean acceptOnMatch = getBooleanProperty(ACCEPT_ON_MATCH);
        return createFilter(levelMax, levelMin, acceptOnMatch);
    }

    private Filter createFilter(String levelMax, String levelMin, boolean acceptOnMatch) {
        Level max = Level.FATAL;
        Level min = Level.TRACE;
        if (levelMax != null) {
            max = Level.toLevel(levelMax, Level.FATAL);
        }
        if (levelMin != null) {
            min = Level.toLevel(levelMin, Level.DEBUG);
        }
        org.apache.logging.log4j.core.Filter.Result onMatch = acceptOnMatch
                ? org.apache.logging.log4j.core.Filter.Result.ACCEPT
                : org.apache.logging.log4j.core.Filter.Result.NEUTRAL;

        return new FilterWrapper(LevelRangeFilter.createFilter(min, max, onMatch,
                org.apache.logging.log4j.core.Filter.Result.DENY));
    }
}
