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
package org.apache.logging.log4j.core.config.composite;

import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.model.PluginNamespace;

/**
 * Merges two configurations together
 */
public interface MergeStrategy {

    Key<MergeStrategy> KEY = new Key<>() {};

    /**
     * Merge the root node properties into the configuration.
     * @param rootNode The composite root node.
     * @param configuration The configuration to merge.
     */
    void mergeRootProperties(Node rootNode, AbstractConfiguration configuration);

    /**
     * Merge the source node tree into the target node tree.
     *
     * @param target      The target Node tree.
     * @param source      The source Node tree.
     * @param corePlugins The Core plugins to merge.
     */
    void mergeConfigurations(Node target, Node source, PluginNamespace corePlugins);
}
