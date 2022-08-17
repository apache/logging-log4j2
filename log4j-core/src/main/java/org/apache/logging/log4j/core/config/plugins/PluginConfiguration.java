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
package org.apache.logging.log4j.core.config.plugins;

import org.apache.logging.log4j.core.config.plugins.visit.PluginConfigurationVisitor;
import org.apache.logging.log4j.plugins.QualifierType;
import org.apache.logging.log4j.plugins.visit.NodeVisitor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies the current {@link org.apache.logging.log4j.core.config.Configuration}. This can be injected as a
 * parameter to a static {@linkplain org.apache.logging.log4j.plugins.PluginFactory factory method}, or as a field
 * or single-parameter method in a plugin {@linkplain org.apache.logging.log4j.plugins.util.Builder builder class}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
@NodeVisitor.Kind(PluginConfigurationVisitor.class)
@QualifierType
public @interface PluginConfiguration {
    // empty
}
