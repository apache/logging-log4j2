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
package org.apache.logging.log4j.status;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.org.webcompere.systemstubs.SystemStubs.restoreSystemProperties;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariable;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.Permission;
import java.util.PropertyPermission;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ResourceLock(Resources.GLOBAL)
@EnabledForJreRange(
        max = JRE.JAVA_17,
        disabledReason = "Later JDKs require opting in to SecurityManager support at startup")
class StatusLoggerSecurityManagerTest {

    @ParameterizedTest
    @CsvSource({"false, false, WARN", "true, false, WARN", "false, true, TRACE", "true, true, ERROR"})
    void initializes_using_available_properties(
            final boolean denySystemProperties, final boolean denyEnvironment, final String expectedLevel)
            throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(StatusLogger.DEFAULT_STATUS_LISTENER_LEVEL, "TRACE");
            withEnvironmentVariable("LOG4J_STATUS_LOGGER_LEVEL", "WARN").execute(() -> {
                final URL apiLocation =
                        StatusLogger.class.getProtectionDomain().getCodeSource().getLocation();
                // Each case must initialize a fresh StatusLogger, independently of other tests.
                try (final URLClassLoader loader = new URLClassLoader(new URL[] {apiLocation}, null)) {
                    final SecurityManager previous = System.getSecurityManager();
                    final SecurityManager securityManager = new SecurityManager() {
                        @Override
                        public void checkPermission(final Permission permission) {
                            if ((denySystemProperties
                                            && permission instanceof PropertyPermission
                                            && "*".equals(permission.getName()))
                                    || (denyEnvironment
                                            && permission instanceof RuntimePermission
                                            && "getenv.*".equals(permission.getName()))) {
                                throw new SecurityException("Denied: " + permission);
                            }
                        }
                    };
                    final Object logger;
                    try {
                        System.setSecurityManager(securityManager);
                        logger = loader.loadClass(StatusLogger.class.getName())
                                .getMethod("getLogger")
                                .invoke(null);
                    } finally {
                        System.setSecurityManager(previous);
                    }
                    assertThat(logger).isNotNull();
                    assertThat(logger.getClass()
                                    .getMethod("getLevel")
                                    .invoke(logger)
                                    .toString())
                            .isEqualTo(expectedLevel);
                }
            });
        });
    }
}
