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
package org.apache.logging.log4j.jul;

/**
 * Constants for the JUL adapter.
 *
 * @since 2.1
 */
public final class Constants {

    /**
     * Name of the Log4j property to set to override the {@link AbstractLoggerAdapter} to be used. By
     * default, when this property is not set, an appropriate LoggerAdaptor is chosen based on the presence of
     * {@code log4j-core}.
     */
    public static final String LOGGER_ADAPTOR_PROPERTY = "log4j.jul.LoggerAdapter";
    
    /**
     * The Log4j property to set to a custom implementation of {@link org.apache.logging.log4j.jul.LevelConverter}. The specified class must have
     * a default constructor.
     */
    public static final String LEVEL_CONVERTER_PROPERTY = "log4j.jul.levelConverter";

    static final String CORE_LOGGER_CLASS_NAME = "org.apache.logging.log4j.core.Logger";
    static final String CORE_LOGGER_ADAPTER_CLASS_NAME = "org.apache.logging.log4j.jul.CoreLoggerAdapter";
    static final String API_LOGGER_ADAPTER_CLASS_NAME = "org.apache.logging.log4j.jul.ApiLoggerAdapter";

    private Constants() {
    }
}
