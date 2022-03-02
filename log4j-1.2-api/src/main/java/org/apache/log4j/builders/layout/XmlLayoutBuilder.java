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

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfiguration.PARAM_TAG;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Layout;
import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.layout.Log4j1XmlLayout;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.w3c.dom.Element;

/**
 * Build an XML Layout
 */
@Plugin(name = "org.apache.log4j.xml.XMLLayout", category = CATEGORY)
public class XmlLayoutBuilder extends AbstractBuilder<Layout> implements LayoutBuilder {

    private static final String LOCATION_INFO = "LocationInfo";
    private static final String PROPERTIES = "Properties";

    public XmlLayoutBuilder() {
    }

    public XmlLayoutBuilder(String prefix, Properties props) {
        super(prefix, props);
    }


    @Override
    public Layout parse(Element layoutElement, XmlConfiguration config) {
        final AtomicBoolean properties = new AtomicBoolean();
        final AtomicBoolean locationInfo = new AtomicBoolean();
        forEachElement(layoutElement.getElementsByTagName(PARAM_TAG), currentElement -> {
            if (PROPERTIES.equalsIgnoreCase(currentElement.getAttribute("name"))) {
                properties.set(getBooleanValueAttribute(currentElement));
            } else if (LOCATION_INFO.equalsIgnoreCase(currentElement.getAttribute("name"))) {
                locationInfo.set(getBooleanValueAttribute(currentElement));
            }
        });
        return createLayout(properties.get(), locationInfo.get());
    }

    @Override
    public Layout parse(PropertiesConfiguration config) {
        boolean properties = getBooleanProperty(PROPERTIES);
        boolean locationInfo = getBooleanProperty(LOCATION_INFO);
        return createLayout(properties, locationInfo);
    }

    private Layout createLayout(boolean properties, boolean locationInfo) {
        return new LayoutWrapper(Log4j1XmlLayout.createLayout(locationInfo, properties));
    }
}
