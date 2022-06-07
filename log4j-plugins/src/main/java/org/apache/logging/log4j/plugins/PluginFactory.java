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

import org.apache.logging.log4j.plugins.visit.NodeVisitor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a static method as a factory to create a {@link Configurable} plugin or a
 * {@linkplain org.apache.logging.log4j.plugins.util.Builder builder class} for constructing a plugin.
 * Factory methods should annotate their parameters with {@link PluginAttribute}, {@link PluginElement},
 * {@link PluginValue}, or other plugin annotations annotated with {@link NodeVisitor.Kind}.
 * If a factory method returns a builder class, this method should have no arguments; instead, the builder class should
 * annotate its fields or parameters in methods to inject plugin configuration data.
 * <p>
 * There can only be one factory method per class.
 * </p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@FactoryType
public @interface PluginFactory {
    // empty
}
