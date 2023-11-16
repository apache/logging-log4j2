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
package org.apache.logging.log4j.util;

import java.security.Permission;
import org.apache.logging.log4j.test.junit.SecurityManagerTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Tests https://issues.apache.org/jira/browse/LOG4J2-2274.
 * <p>
 * Using a security manager can mess up other tests so this is best used from
 * integration tests (classes that end in "IT" instead of "Test" and
 * "TestCase".)
 * </p>
 *
 * @see EnvironmentPropertySource
 * @see SecurityManager
 * @see System#setSecurityManager(SecurityManager)
 */
@ResourceLock("java.lang.SecurityManager")
public class EnvironmentPropertySourceSecurityManagerIT {

    @Rule
    public final SecurityManagerTestRule rule = new SecurityManagerTestRule(new TestSecurityManager());

    /**
     * Always throws a SecurityException for any environment variables permission
     * check.
     */
    private static class TestSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(final Permission permission) {
            if ("getenv.*".equals(permission.getName())) {
                throw new SecurityException();
            }
        }
    }

    /**
     * Makes sure we do not blow up with exception below due to a security manager
     * rejecting environment variable access in {@link EnvironmentPropertySource}.
     *
     * <pre>
     * java.lang.NoClassDefFoundError: Could not initialize class org.apache.logging.log4j.util.PropertiesUtil
     *     at org.apache.logging.log4j.status.StatusLogger.<clinit>(StatusLogger.java:78)
     *     at org.apache.logging.log4j.core.AbstractLifeCycle.<clinit>(AbstractLifeCycle.java:38)
     *     at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
     *     at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)
     *     at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
     *     at java.lang.reflect.Constructor.newInstance(Constructor.java:423)
     *     at org.apache.logging.log4j.core.config.builder.impl.DefaultConfigurationBuilder.build(DefaultConfigurationBuilder.java:172)
     *     at org.apache.logging.log4j.core.config.builder.impl.DefaultConfigurationBuilder.build(DefaultConfigurationBuilder.java:161)
     *     at org.apache.logging.log4j.core.config.builder.impl.DefaultConfigurationBuilder.build(DefaultConfigurationBuilder.java:1)
     *     at org.apache.logging.log4j.util.EnvironmentPropertySourceSecurityManagerTest.test(EnvironmentPropertySourceSecurityManagerTest.java:55)
     * </pre>
     */
    @Test
    public void test() {
        PropertiesUtil.getProperties();
    }
}
