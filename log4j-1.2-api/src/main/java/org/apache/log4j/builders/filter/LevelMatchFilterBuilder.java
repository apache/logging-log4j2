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
import org.apache.logging.log4j.core.filter.LevelMatchFilter;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

import java.util.Properties;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfiguration.*;
import static org.apache.log4j.xml.XmlConfiguration.VALUE_ATTR;

/**
 * Build a Level match failter.
 */
@Plugin(name = "org.apache.log4j.varia.LevelMatchFilter", category = CATEGORY)
public class LevelMatchFilterBuilder extends AbstractBuilder implements FilterBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String LEVEL = "LevelToMatch";
    private static final String ACCEPT_ON_MATCH = "AcceptOnMatch";

    public LevelMatchFilterBuilder() {
    }

    public LevelMatchFilterBuilder(String prefix, Properties props) {
        super(prefix, props);
    }

    @Override
    public Filter parseFilter(Element filterElement, XmlConfiguration config) {
        final Holder<String> level = new Holder<>();
        final Holder<Boolean> acceptOnMatch = new BooleanHolder();
        forEachElement(filterElement.getElementsByTagName("param"), currentElement -> {
            if (currentElement.getTagName().equals("param")) {
                switch (currentElement.getAttribute(NAME_ATTR)) {
                    case LEVEL:
                        level.set(currentElement.getAttribute(VALUE_ATTR));
                        break;
                    case ACCEPT_ON_MATCH:
                        acceptOnMatch.set(Boolean.parseBoolean(currentElement.getAttribute(VALUE_ATTR)));
                        break;
                }
            }
        });
        return createFilter(level.get(), acceptOnMatch.get());
    }

    @Override
    public Filter parseFilter(PropertiesConfiguration config) {
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
