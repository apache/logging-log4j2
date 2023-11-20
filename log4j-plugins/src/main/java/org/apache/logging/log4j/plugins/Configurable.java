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
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.util.Strings;

/**
 * Annotates a plugin as being a configurable plugin. A configurable plugin corresponds to a {@code Node} element
 * of a configuration tree. Each configuration element may have zero or more {@linkplain PluginAttribute attributes}
 * where attribute values are converted from strings into other types via {@link TypeConverter}, an optional
 * {@linkplain PluginValue value} (another type of plugin attribute which may have dedicated syntax in some configuration
 * formats such as XML), and zero or more child elements. Configurable plugins are not the only type of plugin that
 * may be referenced in a configuration file; other plugin namespaces may define their own rules for interpreting
 * configuration data if the node tree representation is inadequate.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.TYPE_USE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Documented
@Namespace(Node.CORE_NAMESPACE)
public @interface Configurable {
    /**
     * Name of the corresponding category of elements this plugin belongs under. For example, {@code appender} would
     * indicate an Appender plugin which would be in the
     * {@code <Appenders/>} element of a Configuration and is injected into a {@link PluginElement} injection point of
     * the containing plugin. When using a strict XML configuration format, the XML element name must match this value
     * rather than the {@linkplain Named name} of the plugin.
     * @return the element's type.
     */
    String elementType() default Strings.EMPTY;

    /**
     * Indicates if the plugin class implements a useful {@link Object#toString()} method for use in debug log messages.
     * @return true if the object should print nicely.
     */
    boolean printObject() default false;

    /**
     * Indicates if construction and injection of child configuration nodes should be deferred until first use.
     * When enabled, children plugins may only be injected as {@link Node} instances and handled manually.
     * @return true if child elements should defer instantiation until they are accessed.
     */
    boolean deferChildren() default false;
}
