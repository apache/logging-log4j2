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
package org.apache.logging.log4j.core.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.net.Advertiser;

@Plugin(name = "memory", category = Core.CATEGORY_NAME, elementType = "advertiser", printObject = false)
public class InMemoryAdvertiser implements Advertiser {
    private static Map<Object, Map<String, String>> properties = new HashMap<>();

    public static Map<Object, Map<String, String>> getAdvertisedEntries() {
        return new HashMap<>(properties);
    }

    @Override
    public Object advertise(final Map<String, String> newEntry) {
        final Object object = new Object();
        properties.put(object, new HashMap<>(newEntry));
        return object;
    }

    @Override
    public void unadvertise(final Object advertisedObject) {
        properties.remove(advertisedObject);
    }
}
