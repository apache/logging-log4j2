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

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.bridge.FilterWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.filter.LevelMatchFilter;
import org.w3c.dom.Element;

/**
 * Build a Level match failter.
 */
@Plugin(name = "org.apache.log4j.varia.LevelMatchFilter", category = CATEGORY)
public class LevelMatchFilterBuilder extends AbstractBuilder<Filter> implements FilterBuilder {

    private static final String LEVEL = "LevelToMatch";
    private static final String ACCEPT_ON_MATCH = "AcceptOnMatch";

    public LevelMatchFilterBuilder() {
    }

    public LevelMatchFilterBuilder(String prefix, Properties props) {
        super(prefix, props);
    }

    @Override
    public Filter parse(Element filterElement, XmlConfiguration config) {
        final AtomicReference<String> level = new AtomicReference<>();
        final AtomicBoolean acceptOnMatch = new AtomicBoolean();
        forEachElement(filterElement.getElementsByTagName("param"), currentElement -> {
            if (currentElement.getTagName().equals("param")) {
                switch (getNameAttributeKey(currentElement)) {
                    case LEVEL:
                        level.set(getValueAttribute(currentElement));
                        break;
                    case ACCEPT_ON_MATCH:
                        acceptOnMatch.set(getBooleanValueAttribute(currentElement));
                        break;
                }
            }
        });
        return createFilter(level.get(), acceptOnMatch.get());
    }

    @Override
    public Filter parse(PropertiesConfiguration config) {
        String level = getProperty(LEVEL);
        boolean acceptOnMatch = getBooleanProperty(ACCEPT_ON_MATCH);
        return createFilter(level, acceptOnMatch);
    }

    private Filter createFilter(String level, boolean acceptOnMatch) {
        Level lvl = Level.ERROR;
        if (level != null) {
            lvl = Level.toLevel(level, Level.ERROR);
        }
        org.apache.logging.log4j.core.Filter.Result onMatch = acceptOnMatch
                ? org.apache.logging.log4j.core.Filter.Result.ACCEPT
                : org.apache.logging.log4j.core.Filter.Result.DENY;
        return new FilterWrapper(LevelMatchFilter.newBuilder()
                .setLevel(lvl)
                .setOnMatch(onMatch)
                .setOnMismatch(org.apache.logging.log4j.core.Filter.Result.NEUTRAL)
                .build());
    }
}
