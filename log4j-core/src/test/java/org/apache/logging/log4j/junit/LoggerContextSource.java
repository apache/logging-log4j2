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

package org.apache.logging.log4j.junit;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Specifies a configuration file to use for unit tests. This configuration file will be loaded once and used for all tests
 * executed in the annotated test class unless otherwise specified by {@link #reconfigure()}. When annotated on a test method,
 * this will override the class-level configuration if provided for that method. By using this JUnit 5 extension, the following
 * types can be injected into tests via constructor or method parameters:
 *
 * <ul>
 *     <li>{@link LoggerContext};</li>
 *     <li>{@link Configuration};</li>
 *     <li>any subclass of {@link Appender} paired with a {@link Named} annotation to select the appender by name.</li>
 * </ul>
 *
 * Tests using this extension will automatically be tagged as {@code functional} to indicate they perform functional tests that
 * rely on configuration files and production code.
 *
 * @since 2.14.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Inherited
@Tag("functional")
@ExtendWith(LoggerContextResolver.class)
@ExtendWith(ConfigurationResolver.class)
@ExtendWith(AppenderResolver.class)
public @interface LoggerContextSource {
    /**
     * Specifies the name of the configuration file to use for the annotated test.
     */
    String value();

    /**
     * Specifies when to {@linkplain LoggerContext#reconfigure() reconfigure} the logging system.
     */
    ReconfigurationPolicy reconfigure() default ReconfigurationPolicy.NEVER;

    /**
     * Specifies the shutdown timeout limit. Defaults to 0 to mean no limit.
     */
    long timeout() default 0L;

    /**
     * Specifies the time unit {@link #timeout()} is measured in.
     */
    TimeUnit unit() default TimeUnit.SECONDS;
}
