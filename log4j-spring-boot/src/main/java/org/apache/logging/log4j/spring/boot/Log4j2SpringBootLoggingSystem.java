/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
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
 * limitations under the License.
 */
package org.apache.logging.log4j.spring.boot;

import org.apache.logging.log4j.util.PropertiesUtil;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemFactory;
import org.springframework.boot.logging.log4j2.Log4J2LoggingSystem;
import org.springframework.core.annotation.Order;

/**
 * Override Spring's implementation of the Log4j 2 Logging System to properly support Spring Cloud Config.
 *
 * <p>Spring Boot 3.5.x provides credential-aware remote configuration loading in {@link Log4J2LoggingSystem}.
 * This subclass remains registered via {@code META-INF/spring.factories} and only customizes factory selection.
 */
public class Log4j2SpringBootLoggingSystem extends Log4J2LoggingSystem {

    /**
     * Property that disables the usage of this {@link LoggingSystem}.
     */
    public static final String LOG4J2_DISABLE_CLOUD_CONFIG_LOGGING_SYSTEM = "log4j2.disableCloudConfigLoggingSystem";

    /**
     * Legacy environment object key retained for tests and manual {@link org.apache.logging.log4j.core.LoggerContext}
     * wiring. Prefer {@link Log4J2LoggingSystem#getEnvironment(org.apache.logging.log4j.core.LoggerContext)} for
     * Spring Boot 3.x.
     */
    public static final String ENVIRONMENT_KEY = "SpringEnvironment";

    private static final int PRECEDENCE = 0;

    public Log4j2SpringBootLoggingSystem(final ClassLoader loader) {
        super(loader);
    }

    @Order(PRECEDENCE)
    public static class Factory implements LoggingSystemFactory {

        @Override
        public LoggingSystem getLoggingSystem(final ClassLoader classLoader) {
            if (PropertiesUtil.getProperties().getBooleanProperty(LOG4J2_DISABLE_CLOUD_CONFIG_LOGGING_SYSTEM)) {
                return null;
            }
            return new Log4j2SpringBootLoggingSystem(classLoader);
        }
    }
}
