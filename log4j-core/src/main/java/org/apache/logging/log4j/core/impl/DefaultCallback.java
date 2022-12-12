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
package org.apache.logging.log4j.core.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.InjectorCallback;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.spi.ClassFactory;
import org.apache.logging.log4j.spi.DefaultClassFactory;
import org.apache.logging.log4j.spi.LoggingSystem;
import org.apache.logging.log4j.util.PropertyResolver;

public class DefaultCallback implements InjectorCallback {
    @Override
    public void configure(final Injector injector) {
        if (System.getSecurityManager() != null) {
            injector.setReflectionAccessor(object -> AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                object.setAccessible(true);
                return null;
            }));

        } else {
            injector.setReflectionAccessor(object -> object.setAccessible(true));
        }
        injector.registerBinding(Key.forClass(PropertyResolver.class), LoggingSystem::getPropertyResolver)
                .registerBinding(Key.forClass(ClassFactory.class), DefaultClassFactory::new)
                .registerBundle(DefaultBundle.class);
    }

    @Override
    public String toString() {
        return getClass().getName();
    }
}
