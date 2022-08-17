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

import org.apache.logging.log4j.util.Strings;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a plugin as being a configurable plugin. Configurable plugins are instantiated from a tree of
 * {@link Node} objects.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Namespace(Node.CORE_NAMESPACE)
public @interface Configurable {
    /**
     * Name of the corresponding category of elements this plugin belongs under. For example, {@code appender} would
     * indicate an Appender plugin which would be in the
     * {@code <Appenders/>} element of a Configuration and is injected into a {@link PluginElement} injection point of
     * the containing plugin.
     * @return the element's type.
     */
    String elementType() default Strings.EMPTY;

    /**
     * Indicates if the plugin class implements a useful {@link Object#toString()} method for use in log messages.
     * @return true if the object should print nicely.
     */
    boolean printObject() default false;

    /**
     * Indicates if construction and injection of child configuration nodes should be deferred until first use.
     * @return true if child elements should defer instantiation until they are accessed.
     */
    boolean deferChildren() default false;

}
