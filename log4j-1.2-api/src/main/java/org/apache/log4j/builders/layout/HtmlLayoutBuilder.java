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
package org.apache.log4j.builders.layout;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfiguration.PARAM_TAG;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Layout;
import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.w3c.dom.Element;

/**
 * Build a Pattern Layout
 */
@Plugin(name = "org.apache.log4j.HTMLLayout", category = CATEGORY)
public class HtmlLayoutBuilder extends AbstractBuilder<Layout> implements LayoutBuilder {

    private static final String DEFAULT_TITLE = "Log4J Log Messages";
    private static final String TITLE_PARAM = "Title";
    private static final String LOCATION_INFO_PARAM = "LocationInfo";

    public HtmlLayoutBuilder() {}

    public HtmlLayoutBuilder(final String prefix, final Properties props) {
        super(prefix, props);
    }

    @Override
    public Layout parse(final Element layoutElement, final XmlConfiguration config) {
        final AtomicReference<String> title = new AtomicReference<>("Log4J Log Messages");
        final AtomicBoolean locationInfo = new AtomicBoolean();
        forEachElement(layoutElement.getElementsByTagName("param"), currentElement -> {
            if (currentElement.getTagName().equals(PARAM_TAG)) {
                if (TITLE_PARAM.equalsIgnoreCase(currentElement.getAttribute("name"))) {
                    title.set(currentElement.getAttribute("value"));
                } else if (LOCATION_INFO_PARAM.equalsIgnoreCase(currentElement.getAttribute("name"))) {
                    locationInfo.set(getBooleanValueAttribute(currentElement));
                }
            }
        });
        return createLayout(title.get(), locationInfo.get());
    }

    @Override
    public Layout parse(final PropertiesConfiguration config) {
        final String title = getProperty(TITLE_PARAM, DEFAULT_TITLE);
        final boolean locationInfo = getBooleanProperty(LOCATION_INFO_PARAM);
        return createLayout(title, locationInfo);
    }

    private Layout createLayout(final String title, final boolean locationInfo) {
        return LayoutWrapper.adapt(HtmlLayout.newBuilder()
                .withTitle(title)
                .withLocationInfo(locationInfo)
                .build());
    }
}
