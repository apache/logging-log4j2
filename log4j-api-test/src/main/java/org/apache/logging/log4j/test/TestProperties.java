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
package org.apache.logging.log4j.test;

/**
 * A container for per-test properties.
 */
public interface TestProperties {

    /**
     * Path to a directory specific to the test class,
     */
    public static final String LOGGING_PATH = "logging.path";

    String getProperty(final String key);

    boolean containsProperty(final String key);

    void setProperty(final String key, final String value);

    default void setProperty(final String key, final boolean value) {
        setProperty(key, value ? "true" : "false");
    }

    default void setProperty(final String key, final int value) {
        setProperty(key, Integer.toString(value));
    }

    void clearProperty(final String key);
}
