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
package org.apache.logging.log4j.test.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Properties;

import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.test.ListStatusListener;
import org.junit.jupiter.api.extension.ExtendWith;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Configures and injects a {@link StatusListener} of type
 * {@link ListStatusListener}, that will collect status messages for the test
 * context.
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
@Inherited
@Documented
@ExtendWith(ExtensionContextAnchor.class)
@ExtendWith(StatusLoggerExtension.class)
public @interface UsingStatusListener {
}
