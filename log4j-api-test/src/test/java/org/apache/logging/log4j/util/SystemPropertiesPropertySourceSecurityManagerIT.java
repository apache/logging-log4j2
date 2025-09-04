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

import static org.junit.jupiter.api.Assertions.assertNull;

import java.security.Permission;
import java.util.PropertyPermission;
import org.apache.logging.log4j.test.junit.SecurityManagerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Test related to https://issues.apache.org/jira/browse/LOG4J2-2274.
 * <p>
 * Using a security manager can mess up other tests so this is best used from
 * integration tests (classes that end in "IT" instead of "Test" and
 * "TestCase".)
 * </p>
 *
 * @see SystemPropertiesPropertySource
 * @see SecurityManager
 * @see System#setSecurityManager(SecurityManager)
 * @see PropertyPermission
 */
@ResourceLock("java.lang.SecurityManager")
public class SystemPropertiesPropertySourceSecurityManagerIT {

    @RegisterExtension
    public final SecurityManagerExtension ext = new SecurityManagerExtension(new TestSecurityManager());

    /**
     * Always throws a SecurityException for any environment variables permission
     * check.
     */
    private static class TestSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(final Permission permission) {
            if (permission instanceof PropertyPermission
                    && !permission.getName().equals("java.util.secureRandomSeed")) {
                throw new SecurityException("Unexpected permission: " + permission);
            }
        }
    }

    /**
     * Makes sure we do not blow up with exception below due to a security manager
     * rejecting environment variable access in
     * {@link SystemPropertiesPropertySource}.
     *
     * <pre>
     * java.lang.ExceptionInInitializerError
     * 	at org.apache.logging.log4j.util.SystemPropertiesPropertySourceSecurityManagerTest.test(SystemPropertiesPropertySourceSecurityManagerTest.java:64)
     * 	...
     * Caused by: java.lang.SecurityException
     * 	at org.apache.logging.log4j.util.SystemPropertiesPropertySourceSecurityManagerTest$TestSecurityManager.checkPermission(SystemPropertiesPropertySourceSecurityManagerTest.java:49)
     * 	at java.lang.SecurityManager.checkPropertiesAccess(SecurityManager.java:1265)
     * 	at java.lang.System.getProperties(System.java:624)
     * 	at org.apache.logging.log4j.util.SystemPropertiesPropertySource.forEach(SystemPropertiesPropertySource.java:40)
     * 	at org.apache.logging.log4j.util.PropertiesUtil$Environment.reload(PropertiesUtil.java:330)
     * 	at org.apache.logging.log4j.util.PropertiesUtil$Environment.<init>(PropertiesUtil.java:322)
     * 	at org.apache.logging.log4j.util.PropertiesUtil$Environment.<init>(PropertiesUtil.java:310)
     * 	at org.apache.logging.log4j.util.PropertiesUtil.<init>(PropertiesUtil.java:69)
     * 	at org.apache.logging.log4j.util.PropertiesUtil.<clinit>(PropertiesUtil.java:49)
     * 	... 26 more
     * </pre>
     */
    @Test
    public void test() {
        final PropertiesUtil propertiesUtil = new PropertiesUtil("src/test/resources/PropertiesUtilTest.properties");
        assertNull(propertiesUtil.getStringProperty("a.1"));
    }
}
