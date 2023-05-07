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

import org.apache.logging.slf4j.Log4jMarkerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.spi.MarkerFactoryBinder;

/**
 * SLF4J MarkerFactoryBinder implementation using Log4j. This class is part of the required classes used to specify an
 * SLF4J logging provider implementation.
 */
public class StaticMarkerBinder implements MarkerFactoryBinder {

    /**
     * The unique instance of this class.
     */
    public static final StaticMarkerBinder SINGLETON = new StaticMarkerBinder();

    private final IMarkerFactory markerFactory = new Log4jMarkerFactory();

    /**
     * Returns the {@link #SINGLETON} {@link StaticMarkerBinder}.
     * Added to slf4j-api 1.7.14 via https://github.com/qos-ch/slf4j/commit/ea3cca72cd5a9329a06b788317a17e806ee8acd0
     * @return the singleton instance
     */
    public static StaticMarkerBinder getSingleton() {
        return SINGLETON;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public String getMarkerFactoryClassStr() {
        return Log4jMarkerFactory.class.getName();
    }
}
