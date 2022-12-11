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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Optional;

import org.apache.logging.log4j.util.InternalException;
import org.apache.logging.log4j.util.LowLevelLogUtil;

/**
 * Default strategy for creating instances of classes. This wraps security-sensitive reflection lookups using
 * {@link AccessController} when a {@link SecurityManager} is enabled.
 */
public class DefaultInstanceFactory implements InstanceFactory {
    @Override
    public <T> T getInstance(final Class<T> type) {
        try {
            final Constructor<T> constructor = getConstructor(type);
            return constructor.newInstance();
        } catch (final NoSuchMethodException | InstantiationException | LinkageError | IllegalAccessException e) {
            throw new InternalException(e);
        } catch (final InvocationTargetException e) {
            throw new InternalException(e.getCause());
        }
    }

    private static <T> Constructor<T> getConstructor(final Class<T> type) throws NoSuchMethodException, IllegalAccessException {
        if (System.getSecurityManager() == null) {
            return doGetConstructor(type);
        }
        final PrivilegedExceptionAction<Constructor<T>> action = () -> doGetConstructor(type);
        try {
            return AccessController.doPrivileged(action);
        } catch (final PrivilegedActionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof NoSuchMethodException) {
                throw (NoSuchMethodException) cause;
            }
            if (cause instanceof IllegalAccessException) {
                throw (IllegalAccessException) cause;
            }
            throw new InternalException(cause);
        }
    }

    private static <T> Constructor<T> doGetConstructor(final Class<T> type) throws NoSuchMethodException, IllegalAccessException {
        try {
            return type.getConstructor();
        } catch (final NoSuchMethodException ignored) {
            final Constructor<T> constructor = type.getDeclaredConstructor();
            if (!(constructor.canAccess(null) || constructor.trySetAccessible())) {
                throw new IllegalAccessException("Cannot access " + constructor);
            }
            return constructor;
        }
    }

    @Override
    public <T> Optional<T> tryGetInstance(final Class<T> type) {
        try {
            Constructor<T> constructor = getConstructor(type);
            return Optional.of(constructor.newInstance());
        } catch (final NoSuchMethodException e) {
            LowLevelLogUtil.logException("Unable to find a default constructor for " + type, e);
            return Optional.empty();
        } catch (final IllegalAccessException | SecurityException | InternalException e) {
            LowLevelLogUtil.logException("Unable to access constructor for " + type, e);
            return Optional.empty();
        } catch (final InvocationTargetException e) {
            LowLevelLogUtil.logException("Exception thrown by constructor for " + type, e.getCause());
        } catch (final InstantiationException | LinkageError e) {
            LowLevelLogUtil.logException("Unable to create instance of " + type, e);
        }
        return Optional.empty();
    }
}
