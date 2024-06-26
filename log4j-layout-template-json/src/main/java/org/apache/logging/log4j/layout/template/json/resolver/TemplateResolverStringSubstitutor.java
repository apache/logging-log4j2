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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.lookup.StrSubstitutor;

/**
 * A contextual {@link StrSubstitutor} abstraction.
 *
 * @param <V> {@link TemplateResolver} value
 */
public interface TemplateResolverStringSubstitutor<V> {

    StrSubstitutor getInternalSubstitutor();

    /**
     * A substitutor is stable if the replacement doesn't vary with the provided value.
     * That is, in a stable substitutor, {@code value} argument of {@link #replace(Object, String)} is always ignored.
     */
    boolean isStable();

    String replace(V value, String source);
}
