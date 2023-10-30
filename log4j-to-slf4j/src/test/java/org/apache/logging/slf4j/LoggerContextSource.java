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
package org.apache.logging.slf4j;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.logging.log4j.test.junit.TempLoggingDirectory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Specifies a configuration file to use for unit tests.
 *
 * This is similar to the org.apache.logging.log4j.core.junit.LoggerContextSource annotation for Log4j.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Inherited
@Tag("functional")
@ExtendWith(TempLoggingDirectory.class)
@ExtendWith(LoggerContextResolver.class)
@ExtendWith(LoggerResolver.class)
public @interface LoggerContextSource {
    /**
     * Specifies the name of the configuration file to use for the annotated test.
     * <p>
     * Defaults to the fully qualified name of the test class with '.xml' appended.
     * E.g. this class would have a default of
     * {@code org/apache/logging/log4j/core/test/junit/LoggerContextSource.xml}.
     * </p>
     */
    String value() default "";
}
