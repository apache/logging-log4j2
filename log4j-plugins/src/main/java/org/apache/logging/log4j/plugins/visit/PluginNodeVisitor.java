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
package org.apache.logging.log4j.plugins.visit;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.StringBuilders;

public class PluginNodeVisitor implements NodeVisitor {
    private static final Logger LOGGER = StatusLogger.getLogger();

    @Override
    public Object visitField(final Field field, final Node node, final StringBuilder debugLog) {
        if (TypeUtil.isAssignable(field.getGenericType(), node.getClass())) {
            StringBuilders.appendKeyDqValueWithJoiner(debugLog, "node", node.getName(), ", ");
            return Cast.cast(node);
        } else {
            LOGGER.error("Cannot inject Node instance into field {}", field);
            return null;
        }
    }

    @Override
    public Object visitParameter(final Parameter parameter, final Node node, final StringBuilder debugLog) {
        if (TypeUtil.isAssignable(parameter.getParameterizedType(), node.getClass())) {
            StringBuilders.appendKeyDqValueWithJoiner(debugLog, "node", node.getName(), ", ");
            return Cast.cast(node);
        } else {
            LOGGER.error("Cannot inject Node instance into parameter {}", parameter);
            return null;
        }
    }
}
