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
package org.apache.log4j.builders.filter;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.bridge.FilterWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.filter.LevelRangeFilter;
import org.w3c.dom.Element;

/**
 * Build a Level range filter.
 * In this class, order of {@link Level} is log4j1 way, i.e.,
 * {@link Level#ALL} and {@link Level#OFF} have minimum and maximum order, respectively.
 * (see: LOG4J2-2315)
 */
@Plugin(name = "org.apache.log4j.varia.LevelRangeFilter", category = CATEGORY)
public class LevelRangeFilterBuilder extends AbstractBuilder<Filter> implements FilterBuilder {

    private static final String LEVEL_MAX = "LevelMax";
    private static final String LEVEL_MIN = "LevelMin";
    private static final String ACCEPT_ON_MATCH = "AcceptOnMatch";

    public LevelRangeFilterBuilder() {}

    public LevelRangeFilterBuilder(final String prefix, final Properties props) {
        super(prefix, props);
    }

    @Override
    public Filter parse(final Element filterElement, final XmlConfiguration config) {
        final AtomicReference<String> levelMax = new AtomicReference<>();
        final AtomicReference<String> levelMin = new AtomicReference<>();
        final AtomicBoolean acceptOnMatch = new AtomicBoolean();
        forEachElement(filterElement.getElementsByTagName("param"), currentElement -> {
            if (currentElement.getTagName().equals("param")) {
                switch (getNameAttributeKey(currentElement)) {
                    case LEVEL_MAX:
                        levelMax.set(getValueAttribute(currentElement));
                        break;
                    case LEVEL_MIN:
                        levelMin.set(getValueAttribute(currentElement));
                        break;
                    case ACCEPT_ON_MATCH:
                        acceptOnMatch.set(getBooleanValueAttribute(currentElement));
                        break;
                }
            }
        });
        return createFilter(levelMax.get(), levelMin.get(), acceptOnMatch.get());
    }

    @Override
    public Filter parse(final PropertiesConfiguration config) {
        final String levelMax = getProperty(LEVEL_MAX);
        final String levelMin = getProperty(LEVEL_MIN);
        final boolean acceptOnMatch = getBooleanProperty(ACCEPT_ON_MATCH);
        return createFilter(levelMax, levelMin, acceptOnMatch);
    }

    private Filter createFilter(final String levelMax, final String levelMin, final boolean acceptOnMatch) {
        Level max = Level.OFF;
        Level min = Level.ALL;
        if (levelMax != null) {
            max = OptionConverter.toLevel(levelMax, org.apache.log4j.Level.OFF).getVersion2Level();
        }
        if (levelMin != null) {
            min = OptionConverter.toLevel(levelMin, org.apache.log4j.Level.ALL).getVersion2Level();
        }
        final org.apache.logging.log4j.core.Filter.Result onMatch = acceptOnMatch
                ? org.apache.logging.log4j.core.Filter.Result.ACCEPT
                : org.apache.logging.log4j.core.Filter.Result.NEUTRAL;

        // XXX: LOG4J2-2315
        // log4j1 order: ALL < TRACE < DEBUG < ... < FATAL < OFF
        // log4j2 order: ALL > TRACE > DEBUG > ... > FATAL > OFF
        // So we create as LevelRangeFilter.createFilter(minLevel=max, maxLevel=min, ...)
        return FilterWrapper.adapt(
                LevelRangeFilter.createFilter(max, min, onMatch, org.apache.logging.log4j.core.Filter.Result.DENY));
    }
}
