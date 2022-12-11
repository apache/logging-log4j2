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
package org.apache.logging.log4j.spi;

import java.util.Optional;

/**
 * Strategy for managing instances of classes created dynamically rather than statically.
 */
public interface InstanceFactory {
    /**
     * Gets or creates an instance of the given class. At minimum, this should support instantiating classes with a
     * public no-argument constructor, though more sophisticated implementations may support additional dependency
     * injection. The instance returned is not guaranteed to be a fresh instance.
     *
     * @param type the class to look up an instance for
     * @param <T>  the type of the instance
     * @return an instance of the requested class
     * @throws org.apache.logging.log4j.util.InternalException if there was a problem creating the instance
     */
    <T> T getInstance(final Class<T> type);

    /**
     * Attempts to get or create an instance of the given class.
     *
     * @param type the class to look up an instance for
     * @param <T>  the type of the instance
     * @return an instance of the request class or an empty optional
     * @see #getInstance(Class)
     */
    <T> Optional<T> tryGetInstance(final Class<T> type);
}
