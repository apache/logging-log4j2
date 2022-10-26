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
package org.apache.logging.log4j.migration;

public class Log4j2Types {

    public static final String LOGMANAGER = "org/apache/logging/log4j/LogManager";
    public static final String LOGGERCONTEXT = "org/apache/logging/log4j/spi/LoggerContext";
    public static final String LOGGER = "org/apache/logging/log4j/Logger";

    // LogManager methods
    public static final String GET_CONTEXT_NAME = "getContext";
    public static final String GET_CONTEXT_BOOLEAN_DESC = "(Z)Lorg/apache/logging/log4j/spi/LoggerContext;";
    public static final String GET_LOGGER_NAME = "getLogger";
    public static final String LOGMANAGER_GET_LOGGER_STRING_DESC = "(Ljava/lang/String;)Lorg/apache/logging/log4j/Logger;";
    public static final String LOGMANAGER_GET_LOGGER_CLASS_DESC = "(Ljava/lang/Class;)Lorg/apache/logging/log4j/Logger;";

    // LoggerContext methods
    public static final String LOGGERCONTEXT_GET_LOGGER_STRING_DESC = "(Ljava/lang/String;)Lorg/apache/logging/log4j/spi/ExtendedLogger;";
    public static final String LOGGERCONTEXT_GET_LOGGER_CLASS_DESC = "(Ljava/lang/Class;)Lorg/apache/logging/log4j/spi/ExtendedLogger;";

    private Log4j2Types() {
        // prevent instantiation
    }
}
