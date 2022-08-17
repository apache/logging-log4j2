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
import org.apache.logging.log4j.plugins.name.PluginValueNameProvider;
import org.apache.logging.log4j.plugins.visit.NodeVisitor;
import org.apache.logging.log4j.plugins.visit.PluginValueVisitor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a Plugin Value and its corresponding attribute alias for configuration formats that don't distinguish
 * between values and attributes. A value is typically used differently from an attribute in that it is either the
 * main configuration value required or it is the only value needed to create a plugin. A plugin value can be injected
 * as a parameter to a static {@linkplain PluginFactory factory method}, or as a field or single-parameter method in a
 * plugin {@linkplain org.apache.logging.log4j.plugins.util.Builder builder class}.
 *
 * <p>For example, a Property plugin corresponds to a property entry in a configuration file. The property name is
 * specified as an attribute, and the property value is specified as a value.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
@NodeVisitor.Kind(PluginValueVisitor.class)
@NameProvider(PluginValueNameProvider.class)
@QualifierType
public @interface PluginValue {

    /**
     * Specifies the case-insensitive attribute name to use in configuration formats that don't distinguish between
     * attributes and values. By default, this uses the attribute name {@code value}.
     * @return the value of the attribute.
     */
    String value() default "value";
}
