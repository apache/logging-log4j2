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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FilePermission;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Permission;
import java.util.PropertyPermission;
import org.apache.logging.log4j.test.junit.SecurityManagerExtension;
import org.junit.jupiter.api.BeforeAll;
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
 * @see PropertyFilePropertySource
 * @see SecurityManager
 * @see System#setSecurityManager(SecurityManager)
 * @see PropertyPermission
 */
@ResourceLock("java.lang.SecurityManager")
public class PropertyFilePropertySourceSecurityManagerIT {

    @BeforeAll
    public static void beforeAll() {
        assertTrue(Files.exists(Paths.get(TEST_FIXTURE_PATH)), TEST_FIXTURE_PATH);
    }

    @RegisterExtension
    public final SecurityManagerExtension ext = new SecurityManagerExtension(new TestSecurityManager());

    private static final String TEST_FIXTURE_PATH = "src/test/resources/PropertiesUtilTest.properties";

    /**
     * Always throws a SecurityException for any environment variables permission
     * check.
     */
    private static class TestSecurityManager extends SecurityManager {

        @Override
        public void checkPermission(final Permission permission) {
            if (permission instanceof FilePermission && permission.getName().endsWith(TEST_FIXTURE_PATH)) {
                throw new SecurityException();
            }
        }
    }

    /**
     * Makes sure we do not blow up with exception below due to a security manager
     * rejecting environment variable access in
     * {@link SystemPropertiesPropertySource}.
     *
     * <pre>
     * </pre>
     */
    @Test
    public void test() {
        final PropertiesUtil propertiesUtil = new PropertiesUtil(TEST_FIXTURE_PATH);
        assertNull(propertiesUtil.getStringProperty("a.1"));
    }
}
