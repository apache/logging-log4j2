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
package org.apache.logging.slf4j;

import java.util.Map;

import org.apache.logging.log4j.spi.ThreadContextMap;
import org.slf4j.MDC;

/**
 * Bind the ThreadContextMap to the SLF4J MDC.
 */
public class MDCContextMap implements ThreadContextMap {
    @Override
    public void put(final String key, final String value) {
        MDC.put(key, value);
    }

    @Override
    public String get(final String key) {
        return MDC.get(key);
    }

    @Override
    public void remove(final String key) {
        MDC.remove(key);
    }

    @Override
    public void clear() {
        MDC.clear();
    }

    @Override
    public boolean containsKey(final String key) {
        return MDC.getCopyOfContextMap().containsKey(key);
    }

    @Override
    @SuppressWarnings("unchecked") // nothing we can do about this, restricted by SLF4J API
    public Map<String, String> getCopy() {
        return MDC.getCopyOfContextMap();
    }

    @Override
    @SuppressWarnings("unchecked") // nothing we can do about this, restricted by SLF4J API
    public Map<String, String> getImmutableMapOrNull() {
        return MDC.getCopyOfContextMap();
    }

    @Override
    public boolean isEmpty() {
        return MDC.getCopyOfContextMap().isEmpty();
    }
}
