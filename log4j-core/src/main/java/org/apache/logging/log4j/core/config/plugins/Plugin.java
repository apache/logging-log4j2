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
package org.apache.logging.log4j.core.config.plugins;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.logging.log4j.util.Strings;

/**
 * Annotation that identifies a Class as a Plugin.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {

    /**
     * Value of the elementType when none is specified.
     */
    String EMPTY = Strings.EMPTY;

    /**
     * Name of the plugin. Note that this name is case-insensitive.
     */
    String name();

    /**
     * Category to place the plugin under. Category names are case-sensitive.
     */
    String category();

    /**
     * Name of the corresponding category of elements this plugin belongs under. For example, {@code appender} would
     * indicate an {@link org.apache.logging.log4j.core.Appender} plugin which would be in the
     * {@code <Appenders/>} element of a {@link org.apache.logging.log4j.core.config.Configuration}.
     */
    String elementType() default EMPTY;

    /**
     * Indicates if the plugin class implements a useful {@link Object#toString()} method for use in log messages.
     */
    boolean printObject() default false;

    boolean deferChildren() default false;
}
