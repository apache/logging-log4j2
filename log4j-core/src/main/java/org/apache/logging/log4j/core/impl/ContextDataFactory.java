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

import java.util.Map;

import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;

public interface ContextDataFactory {
    StringMap createContextData();

    StringMap createContextData(final int initialCapacity);

    default StringMap createContextData(final Map<String, String> context) {
        final StringMap contextData = createContextData(context.size());
        for (final Map.Entry<String, String> entry : context.entrySet()) {
            contextData.putValue(entry.getKey(), entry.getValue());
        }
        return contextData;
    }

    default StringMap createContextData(final ReadOnlyStringMap readOnlyStringMap) {
        final StringMap contextData = createContextData(readOnlyStringMap.size());
        contextData.putAll(readOnlyStringMap);
        return contextData;
    }

    StringMap emptyFrozenContextData();
}
