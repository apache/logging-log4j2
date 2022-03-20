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
package org.apache.logging.log4j.plugins;

import org.apache.logging.log4j.plugins.name.NameProvider;
import org.apache.logging.log4j.plugins.name.PluginElementNameProvider;
import org.apache.logging.log4j.plugins.visit.NodeVisitor;
import org.apache.logging.log4j.plugins.visit.PluginElementVisitor;
import org.apache.logging.log4j.util.Strings;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a Plugin Element which allows for plugins to be configured and injected into another plugin.
 * Plugin elements can be injected as parameters to a static {@linkplain PluginFactory factory method}, or as fields and
 * single-parameter methods in a plugin {@linkplain org.apache.logging.log4j.plugins.util.Builder builder class}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
@NodeVisitor.Kind(PluginElementVisitor.class)
@NameProvider(PluginElementNameProvider.class)
@QualifierType
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
