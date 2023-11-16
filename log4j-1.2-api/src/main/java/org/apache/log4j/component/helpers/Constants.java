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
package org.apache.log4j.component.helpers;

/**
 * Constants used internally throughout log4j.
 *
 */
public interface Constants {

    /**
     * log4j package name string literal.
     */
    String LOG4J_PACKAGE_NAME = "org.apache.log4j";

    /**
     *  The name of the default repository is "default" (without the quotes).
     */
    String DEFAULT_REPOSITORY_NAME = "default";

    /**
     * application string literal.
     */
    String APPLICATION_KEY = "application";
    /**
     * hostname string literal.
     */
    String HOSTNAME_KEY = "hostname";
    /**
     * receiver string literal.
     */
    String RECEIVER_NAME_KEY = "receiver";
    /**
     * log4jid string literal.
     */
    String LOG4J_ID_KEY = "log4jid";
    /**
     * time stamp pattern string literal.
     */
    String TIMESTAMP_RULE_FORMAT = "yyyy/MM/dd HH:mm:ss";

    /**
     * The default property file name for automatic configuration.
     */
    String DEFAULT_CONFIGURATION_FILE = "log4j.properties";
    /**
     * The default XML configuration file name for automatic configuration.
     */
    String DEFAULT_XML_CONFIGURATION_FILE = "log4j.xml";
    /**
     * log4j.configuration string literal.
     */
    String DEFAULT_CONFIGURATION_KEY = "log4j.configuration";
    /**
     * log4j.configuratorClass string literal.
     */
    String CONFIGURATOR_CLASS_KEY = "log4j.configuratorClass";

    /**
     * JNDI context name string literal.
     */
    String JNDI_CONTEXT_NAME = "java:comp/env/log4j/context-name";

    /**
     * TEMP_LIST_APPENDER string literal.
     */
    String TEMP_LIST_APPENDER_NAME = "TEMP_LIST_APPENDER";
    /**
     * TEMP_CONSOLE_APPENDER string literal.
     */
    String TEMP_CONSOLE_APPENDER_NAME = "TEMP_CONSOLE_APPENDER";
    /**
     * Codes URL string literal.
     */
    String CODES_HREF = "http://logging.apache.org/log4j/docs/codes.html";

    /**
     * ABSOLUTE string literal.
     */
    String ABSOLUTE_FORMAT = "ABSOLUTE";
    /**
     * SimpleTimePattern for ABSOLUTE.
     */
    String ABSOLUTE_TIME_PATTERN = "HH:mm:ss,SSS";

    /**
     * SimpleTimePattern for ABSOLUTE.
     */
    String SIMPLE_TIME_PATTERN = "HH:mm:ss";

    /**
     * DATE string literal.
     */
    String DATE_AND_TIME_FORMAT = "DATE";
    /**
     * SimpleTimePattern for DATE.
     */
    String DATE_AND_TIME_PATTERN = "dd MMM yyyy HH:mm:ss,SSS";

    /**
     * ISO8601 string literal.
     */
    String ISO8601_FORMAT = "ISO8601";
    /**
     * SimpleTimePattern for ISO8601.
     */
    String ISO8601_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";
}
