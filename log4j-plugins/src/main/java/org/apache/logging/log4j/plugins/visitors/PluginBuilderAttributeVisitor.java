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
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.util.NameUtil;
import org.apache.logging.log4j.util.StringBuilders;

import java.util.Map;
import java.util.function.Function;

/**
 * PluginVisitor for PluginBuilderAttribute. If {@code null} is returned for the
 * {@link #visit(org.apache.logging.log4j.core.config.Configuration, org.apache.logging.log4j.plugins.Node, org.apache.logging.log4j.core.LogEvent, StringBuilder)}
 * method, then the default value of the field should remain untouched.
 *
 * @see org.apache.logging.log4j.core.config.plugins.util.PluginBuilder
 */
public class PluginBuilderAttributeVisitor extends AbstractPluginVisitor<PluginBuilderAttribute, Object> {

    public PluginBuilderAttributeVisitor() {
        super(PluginBuilderAttribute.class);
    }

    @Override
    public Object visit(final Object unused, final Node node, final Function<String, String> substitutor,
                        final StringBuilder log) {
        final String overridden = this.annotation.value();
        final String name = overridden.isEmpty() ? this.member.getName() : overridden;
        final Map<String, String> attributes = node.getAttributes();
        final String rawValue = removeAttributeValue(attributes, name, this.aliases);
        final String replacedValue = substitutor.apply(rawValue);
        final Object value = convert(replacedValue, null);
        final Object debugValue = this.annotation.sensitive() ? NameUtil.md5(value + this.getClass().getName()) : value;
        StringBuilders.appendKeyDqValue(log, name, debugValue);
        return value;
    }
}
