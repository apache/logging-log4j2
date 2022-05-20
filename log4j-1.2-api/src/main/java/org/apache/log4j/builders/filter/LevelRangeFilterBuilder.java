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
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.filter.LevelRangeFilter;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.w3c.dom.Element;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.log4j.builders.BuilderManager.NAMESPACE;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

/**
 * Build a Level match filter.
 */
@Namespace(NAMESPACE)
@Plugin("org.apache.log4j.varia.LevelRangeFilter")
public class LevelRangeFilterBuilder extends AbstractBuilder<Filter> implements FilterBuilder {

    private static final String LEVEL_MAX = "LevelMax";
    private static final String LEVEL_MIN = "LevelMin";
    private static final String ACCEPT_ON_MATCH = "AcceptOnMatch";

    public LevelRangeFilterBuilder() {
    }

    public LevelRangeFilterBuilder(String prefix, Properties props) {
        super(prefix, props);
    }

    @Override
    public Filter parse(Element filterElement, XmlConfiguration config) {
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
                        levelMax.set(getValueAttribute(currentElement));
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
    public Filter parse(PropertiesConfiguration config) {
        String levelMax = getProperty(LEVEL_MAX);
        String levelMin = getProperty(LEVEL_MIN);
        boolean acceptOnMatch = getBooleanProperty(ACCEPT_ON_MATCH);
        return createFilter(levelMax, levelMin, acceptOnMatch);
    }

    private Filter createFilter(String levelMax, String levelMin, boolean acceptOnMatch) {
        Level max = Level.FATAL;
        Level min = Level.TRACE;
        if (levelMax != null) {
            max = OptionConverter.toLevel(levelMax, org.apache.log4j.Level.FATAL).getVersion2Level();
        }
        if (levelMin != null) {
            min = OptionConverter.toLevel(levelMin, org.apache.log4j.Level.DEBUG).getVersion2Level();
        }
        org.apache.logging.log4j.core.Filter.Result onMatch = acceptOnMatch
                ? org.apache.logging.log4j.core.Filter.Result.ACCEPT
                : org.apache.logging.log4j.core.Filter.Result.NEUTRAL;

        return FilterWrapper.adapt(LevelRangeFilter.createFilter(min, max, onMatch,
                org.apache.logging.log4j.core.Filter.Result.DENY));
    }
}
