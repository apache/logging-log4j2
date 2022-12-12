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
package org.apache.logging.log4j.util;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;

import org.apache.logging.log4j.spi.LoggingSystemProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.ReadsSystemProperty;
import org.junitpioneer.jupiter.WritesSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ReadsSystemProperty
@WritesSystemProperty
@ClearSystemProperty(key = LoggingSystemProperties.LOADER_FORCE_THREAD_CONTEXT_LOADER)
public class LoaderUtilTest {
    @BeforeEach
    @AfterEach
    public void reset() {
        System.clearProperty(LoggingSystemProperties.LOADER_FORCE_THREAD_CONTEXT_LOADER);
    }

    @Test
    public void systemClassLoader() {
        final Thread thread = Thread.currentThread();
        final ClassLoader tccl = thread.getContextClassLoader();

        System.setProperty(LoggingSystemProperties.LOADER_FORCE_THREAD_CONTEXT_LOADER, "true");
        final ClassLoader loader = new ClassLoader(tccl) {
            @Override
            public Enumeration<URL> getResources(final String name) {
                return Collections.emptyEnumeration();
            }
        };
        thread.setContextClassLoader(loader);
        try {
            assertEquals(0, LoaderUtil.findUrlResources("Log4j-charsets.properties", false).size());

            System.setProperty(LoggingSystemProperties.LOADER_FORCE_THREAD_CONTEXT_LOADER, "false");
            assertEquals(1, LoaderUtil.findUrlResources("Log4j-charsets.properties", false).size());
        } finally {
            thread.setContextClassLoader(tccl);
        }
    }
}
