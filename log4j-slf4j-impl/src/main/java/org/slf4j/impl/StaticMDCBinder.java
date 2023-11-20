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
package org.slf4j.impl;

import org.apache.logging.slf4j.Log4jMDCAdapter;
import org.slf4j.spi.MDCAdapter;

/**
 *
 */
public final class StaticMDCBinder {

    /**
     * The unique instance of this class.
     */
    public static final StaticMDCBinder SINGLETON = new StaticMDCBinder();

    private final MDCAdapter mdcAdapter = new Log4jMDCAdapter();

    private StaticMDCBinder() {}

    /**
     * Returns the {@link #SINGLETON} {@link StaticMDCBinder}.
     * Added to slf4j-api 1.7.14 via https://github.com/qos-ch/slf4j/commit/ea3cca72cd5a9329a06b788317a17e806ee8acd0
     * @return the singleton instance
     */
    public static StaticMDCBinder getSingleton() {
        return SINGLETON;
    }

    /**
     * Currently this method always returns an instance of {@link StaticMDCBinder}.
     * @return an MDC adapter
     */
    public MDCAdapter getMDCA() {
        return mdcAdapter;
    }

    /**
     * Retrieve the adapter class name.
     * @return The adapter class name.
     */
    public String getMDCAdapterClassStr() {
        return Log4jMDCAdapter.class.getName();
    }
}
