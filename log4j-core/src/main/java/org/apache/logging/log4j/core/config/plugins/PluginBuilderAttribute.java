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
import org.apache.logging.log4j.plugins.NameProvider;
import org.apache.logging.log4j.plugins.QualifierType;
import org.apache.logging.log4j.util.Strings;

/**
 * Marks a field as a Plugin Attribute.
 * @deprecated Exists for compatibility with Log4j 2 2.x plugins. Not used for Log4j 2 3.x plugins.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@NameProvider
@QualifierType
@Deprecated(since = "3.0", forRemoval = true)
@SuppressWarnings("removal")
public @interface PluginBuilderAttribute {

    /**
     * Specifies the attribute name this corresponds to. If no attribute is set (i.e., a blank string), then the name
     * of the field (or member) this annotation is attached to will be used.
     */
    String value() default Strings.EMPTY;

    /**
     * Indicates that this attribute is a sensitive one that shouldn't be logged directly. Such attributes will instead
     * be output as a hashed value.
     */
    boolean sensitive() default false;
}
