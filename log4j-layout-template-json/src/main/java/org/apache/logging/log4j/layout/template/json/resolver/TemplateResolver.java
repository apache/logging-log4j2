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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

@FunctionalInterface
public interface TemplateResolver<V> {

    /**
     * Indicates if the resolution should be appended to the parent JSON object.
     * <p>
     * For instance, {@link ThreadContextDataResolver}, i.e., MDC resolver,
     * uses this flag to indicate whether the contents should be appended to the
     * parent JSON object or not.
     */
    default boolean isFlattening() {
        return false;
    }

    /**
     * Indicates if the resolver if applicable at all.
     * <p>
     * For instance, the source line resolver can be short-circuited using this
     * check if the location information is disabled in the layout configuration.
     */
    default boolean isResolvable() {
        return true;
    }

    /**
     * Indicates if the resolver if applicable for the given {@code value}.
     * <p>
     * For instance, the stack trace resolver can be short-circuited using this
     * check if the stack traces are disabled in the layout configuration.
     */
    default boolean isResolvable(V value) {
        return true;
    }

    /**
     * Resolves the given {@code value} using the provided {@link JsonWriter}.
     */
    void resolve(V value, JsonWriter jsonWriter);

    /**
     * Resolves the given {@code value} using the provided {@link JsonWriter}.
     *
     * @param succeedingEntry false, if this is the first element in a collection; true, otherwise
     */
    default void resolve(V value, JsonWriter jsonWriter, boolean succeedingEntry) {
        resolve(value, jsonWriter);
    }

}
