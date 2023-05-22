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
package org.apache.logging.log4j.plugins.di.resolver;

import java.util.List;

import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.di.spi.FactoryResolversPostProcessor;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.plugins.model.PluginType;

/**
 * Post-processor that adds support for various plugin-related annotations. These cover injectable keys with a
 * defined {@link Namespace} and one of the following types. Note that the generic parameter {@code T} may be a
 * plugin class, interface, or a {@link java.util.function.Supplier Supplier&lt;T&gt;} where plugin factories
 * will be injected rather than plugin instances.
 *
 * <ol>
 *     <li>{@link PluginNamespace} for a collection of plugin types within a namespace</li>
 *     <li>{@link PluginType PluginType&lt;T&gt;} for a named or typed plugin within a namespace</li>
 *     <li>{@link java.util.Map Map&lt;String, T&gt;} for a map of plugins keyed by name</li>
 *     <li>{@link java.util.stream.Stream Stream&lt;T&gt;} for a stream of plugins</li>
 *     <li>{@link java.util.Set Set&lt;T&gt;} for a set of plugins</li>
 *     <li>{@link List List&lt;T&gt;} for a list of plugins</li>
 *     <li>{@link java.util.Optional Optional&lt;T&gt;} for an optional plugin</li>
 *     <li>{@link org.apache.logging.log4j.plugins.PluginElement @PluginElement} for configurable children plugin instances</li>
 *     <li>{@link org.apache.logging.log4j.plugins.PluginAttribute @PluginAttribute} for configurable plugin options</li>
 *     <li>{@link org.apache.logging.log4j.plugins.PluginBuilderAttribute @PluginBuilderAttribute} for configurable
 *     plugin options — in contrast to {@code PluginAttribute}, default values for options should be specified in a
 *     field which is useful in plugin builder classes</li>
 *     <li>{@link org.apache.logging.log4j.plugins.PluginValue @PluginValue} for configurable plugin values — special
 *     type of attribute that may have its own dedicated syntax depending on the configuration format in use such as XML</li>
 * </ol>
 *
 * @see PluginNamespaceFactoryResolver
 * @see PluginTypeFactoryResolver
 * @see PluginMapFactoryResolver
 * @see PluginStreamFactoryResolver
 * @see PluginSetFactoryResolver
 * @see PluginListFactoryResolver
 * @see PluginOptionalFactoryResolver
 * @see PluginElementFactoryResolver
 * @see PluginAttributeFactoryResolver
 * @see PluginBuilderAttributeFactoryResolver
 * @see PluginValueFactoryResolver
 */
@Ordered(100)
public class PluginAnnotationFactoryResolversPostProcessor extends FactoryResolversPostProcessor {
    public PluginAnnotationFactoryResolversPostProcessor() {
        super(List.of(
                new PluginNamespaceFactoryResolver(),
                new PluginTypeFactoryResolver(),
                new PluginMapFactoryResolver(),
                new PluginStreamFactoryResolver(),
                new PluginSetFactoryResolver(),
                new PluginListFactoryResolver(),
                new PluginOptionalFactoryResolver(),
                new PluginElementFactoryResolver(),
                new PluginAttributeFactoryResolver(),
                new PluginBuilderAttributeFactoryResolver(),
                new PluginValueFactoryResolver()
        ));
    }
}
