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
package org.apache.logging.log4j.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.Test;

@SetTestProperty(key = "log4j2.isWebapp", value = "false")
@LoggerContextSource("log4j-test3.xml")
class ShutdownDisabledTest {

    @Test
    void testShutdownFlag(final Configuration config, final LoggerContext ctx) throws NoSuchFieldException {
        Field shutdownCallback = LoggerContext.class.getDeclaredField("shutdownCallback");
        Object fieldValue = ReflectionUtil.getFieldValue(shutdownCallback, ctx);
        assertFalse(config.isShutdownHookEnabled(), "Shutdown hook is enabled");
        assertNull(fieldValue, "Shutdown callback");
    }
}
