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

package org.apache.logging.log4j.core.config.plugins.visitors;

import org.apache.logging.log4j.core.config.plugins.PluginNode;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.visitors.AbstractPluginVisitor;

import java.util.function.Function;

/**
 *  @deprecated Provided to support legacy plugins.
 */
public class PluginNodeVisitor extends AbstractPluginVisitor<PluginNode, Object> {
    public PluginNodeVisitor() {
        super(PluginNode.class);
    }

    @Override
    public Object visit(final Object unused, final Node node, final Function<String, String> substitutor,
                        final StringBuilder log) {
        if (this.conversionType.isInstance(node)) {
            log.append("Node=").append(node.getName());
            return node;
        }
        LOGGER.warn("Variable annotated with @PluginNode is not compatible with the type {}.", node.getClass());
        return null;
    }
}
