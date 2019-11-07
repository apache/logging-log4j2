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
import org.apache.log4j.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;

/**
 * Build a Pattern Layout
 */
@Plugin(name = "org.apache.log4j.PatternLayout", category = CATEGORY)
@PluginAliases("org.apache.log4j.EnhancedPatternLayout")
public class PatternLayoutBuilder implements LayoutBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();

    @Override
    public Layout parseLayout(Element layoutElement, XmlConfigurationFactory factory) {
        NodeList params = layoutElement.getElementsByTagName("param");
        final int length = params.getLength();
        String pattern = null;
        for (int index = 0; index < length; ++ index) {
            Node currentNode = params.item(index);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element currentElement = (Element) currentNode;
                if (currentElement.getTagName().equals("param")) {
                    if ("conversionPattern".equalsIgnoreCase(currentElement.getAttribute("name"))) {
                        pattern = currentElement.getAttribute("value")
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
                                .replace("%X", "%properties");
                        break;
                    }
                }
            }
        }
        return new LayoutWrapper(PatternLayout.newBuilder()
                .withPattern(pattern)
                .withConfiguration(factory.getConfiguration())
                .build());
    }
}
