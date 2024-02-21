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
package org.apache.logging.log4j.kit.env.support;

import org.apache.logging.log4j.Logger;

/**
 * An environment implementation that uses a specific classloader to load classes.
 */
public abstract class ClassloaderPropertyEnvironment extends BasicPropertyEnvironment {

    private final ClassLoader loader;

    public ClassloaderPropertyEnvironment(final ClassLoader loader, final Logger statusLogger) {
        super(statusLogger);
        this.loader = loader;
    }

    @Override
    protected Class<?> getClassForName(final String className) throws ReflectiveOperationException {
        return Class.forName(className, true, loader);
    }
}
