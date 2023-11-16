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
package org.apache.logging.log4j.test.util;

import java.lang.invoke.MethodHandles;
import java.util.stream.Stream;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.util.OsgiServiceLocator;
import org.apache.logging.log4j.util.PropertySource;

public class OsgiServiceLocatorTest {

    /**
     * Used by OSGI {@link AbstractLoadBundleTest} to preserve caller
     * sensitivity.
     *
     * @return
     */
    public static Stream<Provider> loadProviders() {
        return OsgiServiceLocator.loadServices(Provider.class, MethodHandles.lookup());
    }

    public static Stream<PropertySource> loadPropertySources() {
        return OsgiServiceLocator.loadServices(PropertySource.class, MethodHandles.lookup());
    }
}
