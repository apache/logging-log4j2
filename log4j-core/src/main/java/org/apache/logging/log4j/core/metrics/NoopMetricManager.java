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
package org.apache.logging.log4j.core.metrics;

import java.util.Map;
import java.util.function.ToDoubleFunction;

public enum NoopMetricManager implements MetricManager {
    INSTANCE;

    @Override
    public LongCounter counter(final String name, final Map<String, String> tags) {
        return NoopLongCounter.INSTANCE;
    }

    @Override
    public <T extends Number> T gauge(final String name, final Map<String, String> tags, final T number) {
        return number;
    }

    @Override
    public <T> T gauge(final String name, final Map<String, String> tags, final T object, final ToDoubleFunction<T> measurement) {
        return object;
    }
}
