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
package org.apache.logging.log4j.plugins;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.logging.log4j.plugins.di.resolver.PluginElementFactoryResolver;
import org.apache.logging.log4j.plugins.name.NameProvider;
import org.apache.logging.log4j.plugins.name.PluginElementNameProvider;
import org.apache.logging.log4j.util.Strings;

/**
 * Qualifier for plugin elements which are configurable child plugins. A plugin may form a tree of plugins through
 * child elements configuring child plugins.
 *
 * @see Configurable
 * @see PluginElementFactoryResolver
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE_USE})
@NameProvider(PluginElementNameProvider.class)
@QualifierType
@Configurable
public @interface PluginElement {

    /**
     * Identifies the case-insensitive element name (or attribute name) this corresponds with in a configuration file.
     * If blank, defaults to using reflection on the annotated element as such:
     *
     * <ul>
     *     <li>Field: uses the field name.</li>
     *     <li>Method: when named <code>set<var>XYZ</var></code> or <code>with<var>XYZ</var></code>, uses the rest
     *     (<var>XYZ</var>) of the method name. Otherwise, uses the name of the first parameter.</li>
     *     <li>Parameter: uses the parameter name.</li>
     * </ul>
     * @return the element name.
     */
    String value() default Strings.EMPTY;
}
