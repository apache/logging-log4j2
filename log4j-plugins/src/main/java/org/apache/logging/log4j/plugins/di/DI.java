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

package org.apache.logging.log4j.plugins.di;

public final class DI {
    private DI() {
        throw new IllegalStateException("Utility class");
    }

    public static Injector createInjector(final Object... modules) {
        final var injector = new DefaultInjector();
        for (final Object module : modules) {
            injector.installModule(module);
        }
        return injector;
    }

    public static Injector createInjector(final Iterable<?> modules) {
        final var injector = new DefaultInjector();
        modules.forEach(injector::installModule);
        return injector;
    }
}
