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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.logging.log4j.plugins.model.PluginService;
import org.apache.logging.log4j.plugins.name.NameProvider;
import org.apache.logging.log4j.plugins.name.PluginNameProvider;
import org.apache.logging.log4j.util.Strings;

/**
 * <p>Annotation that identifies a Class as a Plugin. Plugins are indexed classes with a name that can typically
 * be used to refer to that plugin class in a configuration. Plugin names must be unique within a plugin
 * {@link Namespace}. A plugin is identified by its namespace and name, though the type of the plugin may be
 * used for dependency injection purposes.</p>
 *
 * <p>Plugins are indexed by the plugin annotation processor which generates
 * {@link PluginService} service provider classes containing essential
 * plugin metadata. All plugin namespaces support dependency injection, though some namespaces use alternative
 * factory strategies specific to their plugin types; these plugins will only have their members injected after
 * that factory returns a new instance.</p>
 *
 * @see Inject
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@NameProvider(PluginNameProvider.class)
public @interface Plugin {

    /**
     * Value of the elementType when none is specified.
     */
    String EMPTY = Strings.EMPTY;

    /**
     * Name of the plugin. Note that this name is case-insensitive.
     * If no name is provided, then the {@linkplain Class#getSimpleName() simple name} of the annotated class
     * is used.
     * @return the name of the plugin.
     */
    String value() default EMPTY;

}
