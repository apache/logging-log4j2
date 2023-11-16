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
package org.apache.logging.log4j.core.config.plugins.visitors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

/**
 * Visitor strategy for parsing data from a {@link Node}, doing any relevant type conversion, and returning a
 * parsed value for that variable. Implementations must be constructable using the default constructor.
 *
 * @param <A> the Annotation type.
 */
public interface PluginVisitor<A extends Annotation> {

    /**
     * Sets the Annotation to be used for this. If the given Annotation is not compatible with this class's type, then
     * it is ignored.
     *
     * @param annotation the Annotation instance.
     * @return {@code this}.
     * @throws NullPointerException if the argument is {@code null}.
     */
    PluginVisitor<A> setAnnotation(Annotation annotation);

    /**
     * Sets the list of aliases to use for this visit. No aliases are required, however.
     *
     * @param aliases the list of aliases to use.
     * @return {@code this}.
     */
    PluginVisitor<A> setAliases(String... aliases);

    /**
     * Sets the class to convert the plugin value to on this visit. This should correspond with a class obtained from
     * a factory method or builder class field. Not all PluginVisitor implementations may need this value.
     *
     * @param conversionType the type to convert the plugin string to (if applicable).
     * @return {@code this}.
     * @throws NullPointerException if the argument is {@code null}.
     */
    PluginVisitor<A> setConversionType(Class<?> conversionType);

    /**
     * Sets the StrSubstitutor to use for converting raw strings before type conversion. Generally obtained from a
     * {@link org.apache.logging.log4j.core.config.Configuration}.
     *
     * @param substitutor the StrSubstitutor to use on plugin values.
     * @return {@code this}.
     * @throws NullPointerException if the argument is {@code null}.
     */
    PluginVisitor<A> setStrSubstitutor(StrSubstitutor substitutor);

    /**
     * Sets the Member that this visitor is being used for injection upon. For instance, this could be the Field
     * that is being used for injecting a value, or it could be the factory method being used to inject parameters
     * into.
     *
     * @param member the member this visitor is parsing a value for.
     * @return {@code this}.
     */
    PluginVisitor<A> setMember(Member member);

    /**
     * Visits a Node to obtain a value for constructing a Plugin object.
     *
     * @param configuration the current Configuration.
     * @param node          the current Node corresponding to the Plugin object being created.
     * @param event         the current LogEvent that caused this Plugin object to be made (optional).
     * @param log           the StringBuilder being used to build a debug message.
     * @return the converted value to be used for Plugin creation.
     */
    Object visit(Configuration configuration, Node node, LogEvent event, StringBuilder log);
}
