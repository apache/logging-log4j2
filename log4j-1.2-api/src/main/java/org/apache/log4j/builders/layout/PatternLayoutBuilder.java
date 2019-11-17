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
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Properties;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.xml.XmlConfiguration.PARAM_TAG;

/**
 * Build a Pattern Layout
 */
@Plugin(name = "org.apache.log4j.PatternLayout", category = CATEGORY)
@PluginAliases("org.apache.log4j.EnhancedPatternLayout")
public class PatternLayoutBuilder extends AbstractBuilder implements LayoutBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String PATTERN = "ConversionPattern";

    public PatternLayoutBuilder() {
    }

    public PatternLayoutBuilder(String prefix, Properties props) {
        super(prefix, props);
    }

    @Override
    public Layout parseLayout(final Element layoutElement, final XmlConfiguration config) {
        NodeList params = layoutElement.getElementsByTagName("param");
        final int length = params.getLength();
        String pattern = null;
        for (int index = 0; index < length; ++ index) {
            Node currentNode = params.item(index);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element currentElement = (Element) currentNode;
                if (currentElement.getTagName().equals(PARAM_TAG)) {
                    if (PATTERN.equalsIgnoreCase(currentElement.getAttribute("name"))) {
                        pattern = currentElement.getAttribute("value");
                        break;
                    }
                }
            }
        }
        return createLayout(pattern, config);
    }

    @Override
    public Layout parseLayout(final PropertiesConfiguration config) {
        String pattern = getProperty(PATTERN);
        return createLayout(pattern, config);
    }

    private Layout createLayout(String pattern, final Log4j1Configuration config) {
        if (pattern == null) {
            LOGGER.info("No pattern provided for pattern layout, using default pattern");
            pattern = PatternLayout.DEFAULT_CONVERSION_PATTERN;
        }
        return new LayoutWrapper(PatternLayout.newBuilder()
                .withPattern(pattern
                        // Log4j 2's %x (NDC) is not compatible with Log4j 1's
                        // %x
                        // Log4j 1: "foo bar baz"
                        // Log4j 2: "[foo, bar, baz]"
                        // Use %ndc to get the Log4j 1 format
                        .replace("%x", "%ndc")

                        // Log4j 2's %X (MDC) is not compatible with Log4j 1's
                        // %X
                        // Log4j 1: "{{foo,bar}{hoo,boo}}"
                        // Log4j 2: "{foo=bar,hoo=boo}"
                        // Use %properties to get the Log4j 1 format
                        .replace("%X", "%properties"))
                .withConfiguration(config)
                .build());
    }
}
