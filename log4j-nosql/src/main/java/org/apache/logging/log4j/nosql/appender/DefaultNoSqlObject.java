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
package org.apache.logging.log4j.nosql.appender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link org.apache.logging.log4j.nosql.appender.NoSqlObject}. Most NoSQL Java APIs tend
 * to re-use the Map interface for storage and retrieval of the underlying JSON documents. Therefore, this
 * implementation is provided for convenience.
 */
public class DefaultNoSqlObject implements NoSqlObject<Map<String, Object>> {

    private final Map<String, Object> map;

    public DefaultNoSqlObject() {
        this.map = new HashMap<>();
    }

    @Override
    public void set(final String field, final Object value) {
        this.map.put(field, value);
    }

    @Override
    public void set(final String field, final NoSqlObject<Map<String, Object>> value) {
        this.map.put(field, value.unwrap());
    }

    @Override
    public void set(final String field, final Object[] values) {
        this.map.put(field, Arrays.asList(values));
    }

    @Override
    public void set(final String field, final NoSqlObject<Map<String, Object>>[] values) {
        final List<Map<String, Object>> list = new ArrayList<>(values.length);
        for (final NoSqlObject<Map<String, Object>> value : values) {
            list.add(value.unwrap());
        }
        this.map.put(field, list);
    }

    @Override
    public Map<String, Object> unwrap() {
        return this.map;
    }
}
