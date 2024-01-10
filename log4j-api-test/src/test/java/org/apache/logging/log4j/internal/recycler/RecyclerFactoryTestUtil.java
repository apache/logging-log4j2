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
package org.apache.logging.log4j.internal.recycler;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Properties;
import org.apache.logging.log4j.spi.LoggingSystemProperty;
import org.apache.logging.log4j.spi.recycler.RecyclerFactory;
import org.apache.logging.log4j.spi.recycler.RecyclerFactoryRegistry;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;

final class RecyclerFactoryTestUtil {

    private RecyclerFactoryTestUtil() {}

    @Nullable
    static RecyclerFactory createForEnvironment(
            @Nullable Boolean webApp, @Nullable final String factory, @Nullable final Integer capacity) {
        final Properties properties = new Properties();
        if (webApp != null) {
            properties.setProperty(LoggingSystemProperty.IS_WEBAPP.getSystemKey(), webApp + "");
        }
        if (factory != null) {
            properties.setProperty(LoggingSystemProperty.RECYCLER_FACTORY.getSystemKey(), factory);
        }
        if (capacity != null) {
            properties.setProperty(LoggingSystemProperty.RECYCLER_CAPACITY.getSystemKey(), capacity + "");
        }
        final PropertyEnvironment env = new PropertiesUtil(properties);
        return RecyclerFactoryRegistry.findRecyclerFactory(env);
    }
}
