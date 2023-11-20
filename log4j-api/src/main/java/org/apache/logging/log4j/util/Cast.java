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
package org.apache.logging.log4j.util;

@InternalApi
public final class Cast {

    /**
     * Returns the provided object cast to the generic parameter type or null when the argument is null.
     *
     * @param o   object to cast
     * @param <T> the type to cast
     * @return object after casting or null if the object was null
     * @throws ClassCastException if the object cannot be cast to the provided type
     */
    public static <T> T cast(final Object o) {
        if (o == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        final T t = (T) o;
        return t;
    }

    private Cast() {}
}
