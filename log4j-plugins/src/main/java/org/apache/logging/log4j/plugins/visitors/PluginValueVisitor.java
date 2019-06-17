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

package org.apache.logging.log4j.plugins.visitors;

import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.PluginValue;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.Strings;

import java.util.function.Function;

/**
 * PluginVisitor implementation for {@link PluginValue}.
 */
public class PluginValueVisitor extends AbstractPluginVisitor<PluginValue, Object> {
    public PluginValueVisitor() {
        super(PluginValue.class);
    }

    @Override
    public Object visit(final Object unused, final Node node, final Function<String, String> substitutor,
                        final StringBuilder log) {
        final String name = this.annotation.value();
        final String elementValue = node.getValue();
        final String attributeValue = node.getAttributes().get("value");
        String rawValue = null; // if neither is specified, return null (LOG4J2-1313)
        if (Strings.isNotEmpty(elementValue)) {
            if (Strings.isNotEmpty(attributeValue)) {
                LOGGER.error("Configuration contains {} with both attribute value ({}) AND element" +
                                " value ({}). Please specify only one value. Using the element value.",
                        node.getName(), attributeValue, elementValue);
            }
            rawValue = elementValue;
        } else {
            rawValue = removeAttributeValue(node.getAttributes(), "value");
        }
        final String value = substitutor.apply(rawValue);
        StringBuilders.appendKeyDqValue(log, name, value);
        return value;
    }
}
