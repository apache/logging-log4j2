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
package org.apache.logging.log4j.kit.env;

import org.jspecify.annotations.Nullable;

/**
 * Basic interface to retrieve property values.
 * <p>
 *     We can not reuse the property sources from 2.x, since those required some sort of {@code log4j} prefix to be
 *     included. In 3.x we want to use keys without a prefix.
 * </p>
 */
public interface PropertySource {
    /**
     * Provides the priority of the property source.
     * <p>
     *     Property sources are ordered according to the natural ordering of their priority. Sources with lower
     *     numerical value take precedence over those with higher numerical value.
     * </p>
     *
     * @return priority value
     */
    int getPriority();

    /**
     * Gets the named property as a String.
     *
     * @param name the name of the property to look up
     * @return the String value of the property or {@code null} if undefined.
     */
    @Nullable
    String getProperty(String name);
}
