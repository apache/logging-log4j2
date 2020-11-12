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
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.builders.BooleanHolder;
import org.apache.log4j.builders.Holder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.XmlLayout;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

import java.util.Properties;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfiguration.PARAM_TAG;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

/**
 * Build an XML Layout
 */
@Plugin(name = "org.apache.log4j.xml.XMLLayout", category = CATEGORY)
public class XmlLayoutBuilder extends AbstractBuilder implements LayoutBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final String LOCATION_INFO = "LocationInfo";
    private static final String PROPERTIES = "Properties";

    public XmlLayoutBuilder() {
    }

    public XmlLayoutBuilder(String prefix, Properties props) {
        super(prefix, props);
    }


    @Override
    public Layout parseLayout(Element layoutElement, XmlConfiguration config) {
        final Holder<Boolean> properties = new BooleanHolder();
        final Holder<Boolean> locationInfo = new BooleanHolder();
        forEachElement(layoutElement.getElementsByTagName(PARAM_TAG), currentElement -> {
            if (PROPERTIES.equalsIgnoreCase(currentElement.getAttribute("name"))) {
                properties.set(Boolean.parseBoolean(currentElement.getAttribute("value")));
            } else if (LOCATION_INFO.equalsIgnoreCase(currentElement.getAttribute("name"))) {
                locationInfo.set(Boolean.parseBoolean(currentElement.getAttribute("value")));
            }
        });
        return createLayout(properties.get(), locationInfo.get());
    }

    @Override
    public Layout parseLayout(PropertiesConfiguration config) {
        boolean properties = getBooleanProperty(PROPERTIES);
        boolean locationInfo = getBooleanProperty(LOCATION_INFO);
        return createLayout(properties, locationInfo);
    }

    private Layout createLayout(boolean properties, boolean locationInfo) {
        return new LayoutWrapper(XmlLayout.newBuilder()
                .setLocationInfo(locationInfo)
                .setProperties(properties)
                .build());
    }
}
