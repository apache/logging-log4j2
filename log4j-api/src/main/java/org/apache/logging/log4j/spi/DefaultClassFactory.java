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

import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.LowLevelLogUtil;

/**
 * Default strategy for locating and loading classes by name. This attempts to load a class using multiple available
 * ClassLoaders depending on the runtime environment.
 *
 * @see LoaderUtil#getClassLoaders()
 */
public class DefaultClassFactory implements ClassFactory {
    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        ClassNotFoundException exception = null;
        for (final ClassLoader classLoader : LoaderUtil.getClassLoaders()) {
            try {
                return classLoader.loadClass(name);
            } catch (final ClassNotFoundException e) {
                if (exception == null) {
                    exception = e;
                } else {
                    exception.addSuppressed(e);
                }
            }
        }
        throw exception != null ? exception : new ClassNotFoundException(name);
    }

    @Override
    public <T> Optional<Class<? extends T>> tryGetClass(final String name, final Class<T> superclass) {
        for (final ClassLoader classLoader : LoaderUtil.getClassLoaders()) {
            final Class<?> clazz;
            try {
                clazz = classLoader.loadClass(name);
                return Optional.of(clazz.asSubclass(superclass));
            } catch (final ClassNotFoundException | LinkageError | ClassCastException ignored) {
            }
        }
        LowLevelLogUtil.log("Unable to get class " + name + " with superclass " + superclass.getName());
        return Optional.empty();
    }

    @Override
    public boolean isClassAvailable(final String name) {
        for (final ClassLoader classLoader : LoaderUtil.getClassLoaders()) {
            try {
                if (classLoader.loadClass(name) != null) {
                    return true;
                }
            } catch (final ClassNotFoundException | LinkageError ignored) {
            }
        }
        return false;
    }
}
