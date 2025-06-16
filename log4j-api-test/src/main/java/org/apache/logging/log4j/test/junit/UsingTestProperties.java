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
package org.apache.logging.log4j.test.junit;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.apache.logging.log4j.test.TestProperties;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.ReadsEnvironmentVariable;
import org.junitpioneer.jupiter.ReadsSystemProperty;

/**
 * A field or method parameter of type {@link TestProperties} will be injected with a per-test source of Log4j2's
 * system properties.
 */
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Inherited
@Documented
@ExtendWith({ExtensionContextAnchor.class, TestPropertyResolver.class})
@ReadsSystemProperty
@ReadsEnvironmentVariable
public @interface UsingTestProperties {}
