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
import org.apache.logging.log4j.plugins.name.NameProvider;
import org.apache.logging.log4j.plugins.name.PluginBuilderAttributeNameProvider;
import org.apache.logging.log4j.util.Strings;

/**
 * Qualifier for a plugin attribute for configuration options of a plugin. This works similarly to {@link PluginAttribute}
 * but without a default value being specified in the annotation; instead, default values should be handled programmatically
 * such as through a default field value.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.TYPE, ElementType.TYPE_USE})
@NameProvider(PluginBuilderAttributeNameProvider.class)
@QualifierType
public @interface PluginBuilderAttribute {

    /**
     * Specifies the attribute name this corresponds to. If no attribute is set (i.e., a blank string), then the name
     * of the field (or member) this annotation is attached to will be used.
     * @return the name of the attribute.
     */
    String value() default Strings.EMPTY;

    /**
     * Indicates that this attribute is a sensitive one that shouldn't be logged directly. Such attributes will instead
     * be output as a hashed value.
     * @return true if this attribute should be considered sensitive.
     */
    boolean sensitive() default false;
}
