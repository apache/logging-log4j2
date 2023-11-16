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
package org.apache.logging.log4j.core.config.plugins.visitors;

import java.util.Map;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.util.StringBuilders;

/**
 * PluginVisitor implementation for {@link PluginAttribute}.
 */
public class PluginAttributeVisitor extends AbstractPluginVisitor<PluginAttribute> {
    public PluginAttributeVisitor() {
        super(PluginAttribute.class);
    }

    @Override
    public Object visit(
            final Configuration configuration, final Node node, final LogEvent event, final StringBuilder log) {
        final String name = this.annotation.value();
        final Map<String, String> attributes = node.getAttributes();
        final String rawValue = removeAttributeValue(attributes, name, this.aliases);
        final String replacedValue = this.substitutor.replace(event, rawValue);
        final Object defaultValue = findDefaultValue(event);
        final Object value = convert(replacedValue, defaultValue);
        final Object debugValue = this.annotation.sensitive() ? "*****" : value;
        StringBuilders.appendKeyDqValue(log, name, debugValue);
        return value;
    }

    private Object findDefaultValue(final LogEvent event) {
        if (this.conversionType == int.class || this.conversionType == Integer.class) {
            return this.annotation.defaultInt();
        }
        if (this.conversionType == long.class || this.conversionType == Long.class) {
            return this.annotation.defaultLong();
        }
        if (this.conversionType == boolean.class || this.conversionType == Boolean.class) {
            return this.annotation.defaultBoolean();
        }
        if (this.conversionType == float.class || this.conversionType == Float.class) {
            return this.annotation.defaultFloat();
        }
        if (this.conversionType == double.class || this.conversionType == Double.class) {
            return this.annotation.defaultDouble();
        }
        if (this.conversionType == byte.class || this.conversionType == Byte.class) {
            return this.annotation.defaultByte();
        }
        if (this.conversionType == char.class || this.conversionType == Character.class) {
            return this.annotation.defaultChar();
        }
        if (this.conversionType == short.class || this.conversionType == Short.class) {
            return this.annotation.defaultShort();
        }
        if (this.conversionType == Class.class) {
            return this.annotation.defaultClass();
        }
        return this.substitutor.replace(event, this.annotation.defaultString());
    }
}
