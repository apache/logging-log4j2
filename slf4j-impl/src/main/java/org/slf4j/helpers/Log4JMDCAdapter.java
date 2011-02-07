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
package org.slf4j.helpers;

import org.apache.logging.log4j.ThreadContext;
import org.slf4j.spi.MDCAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class Log4JMDCAdapter implements MDCAdapter {

    public void put(String key, String val) {
        ThreadContext.put(key, val);
    }

    public String get(String key) {
        return ThreadContext.get(key).toString();
    }

    public void remove(String key) {
        ThreadContext.remove(key);
    }

    public void clear() {
        ThreadContext.clear();
    }

    public Map getCopyOfContextMap() {
        Map<String, Object> ctx = ThreadContext.getContext();
        Map<String, String> map = new HashMap<String, String>();

        for (Map.Entry<String, Object>entry : ctx.entrySet()) {
            map.put(entry.getKey(), entry.getValue().toString());
        }
        return map;
    }

    public void setContextMap(Map map) {
        ThreadContext.clear();
        for (Map.Entry<String, String> entry : ((Map<String, String>) map).entrySet()) {
            ThreadContext.put(entry.getKey(), entry.getValue());
        }
    }
}
