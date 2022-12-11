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

import org.apache.logging.log4j.util.LowLevelLogUtil;

/**
 * Indirect form of a {@link ClassLoader} that may be used for loading extensions or plugins.
 *
 * @since 3.0.0
 */
@FunctionalInterface
public interface ClassFactory {

    /**
     * Loads the given class using its binary name.
     *
     * @see ClassLoader#loadClass(String)
     */
    Class<?> loadClass(final String name) throws ClassNotFoundException;

    /**
     * Attempts to load the given class by binary name as an expected subclass of the provided superclass.
     *
     * @param name       binary name of the class to try to load
     * @param superclass expected superclass of the loaded class
     * @param <T>        the expected supertype of the loaded class
     * @return the loaded class if successful or an empty optional otherwise
     */
    default <T> Optional<Class<? extends T>> tryGetClass(final String name, final Class<T> superclass) {
        try {
            final Class<?> clazz = loadClass(name);
            return Optional.of(clazz.asSubclass(superclass));
        } catch (final ClassNotFoundException | LinkageError | ClassCastException e) {
            LowLevelLogUtil.logException("Cannot find class " + name + " with superclass " + superclass.getName(), e);
            return Optional.empty();
        }
    }

    /**
     * Loads the given class by binary name as an expected subclass of the provided superclass.
     *
     * @param name       binary name of the class to try to load
     * @param superclass expected superclass of the loaded class
     * @param <T>        the expected supertype of the loaded class
     * @return the loaded class
     * @throws ClassNotFoundException if the class could not be found with the expected superclass
     */
    default <T> Class<? extends T> getClass(final String name, final Class<T> superclass) throws ClassNotFoundException {
        return tryGetClass(name, superclass).orElseThrow(() ->
                new ClassNotFoundException("Cannot find class " + name + " with superclass " + superclass.getName()));
    }

    /**
     * Checks if a class with the given binary name can be found.
     *
     * @param name binary name of the class to check for existence
     * @return true if a class with the given binary name could be loaded
     */
    default boolean isClassAvailable(final String name) {
        try {
            return loadClass(name) != null;
        } catch (final ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

}
