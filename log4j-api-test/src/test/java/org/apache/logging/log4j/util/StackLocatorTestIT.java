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
import java.util.Deque;
import org.apache.logging.log4j.test.junit.SecurityManagerTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Tests https://github.com/apache/logging-log4j2/pull/1214.
 * <p>
 * Using a security manager can mess up other tests so this is best used from
 * integration tests (classes that end in "IT" instead of "Test" and
 * "TestCase".)
 * </p>
 *
 * @see StackLocator
 * @see SecurityManager
 * @see System#setSecurityManager(SecurityManager)
 */
@ResourceLock("java.lang.SecurityManager")
public class StackLocatorTestIT {
    @Rule
    public final SecurityManagerTestRule rule = new SecurityManagerTestRule(new TestSecurityManager());

    /**
     * Always throws a SecurityException for any reques to create a new SecurityManager
     */
    private static class TestSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(final Permission permission) {
            if ("createSecurityManager".equals(permission.getName())) {
                throw new SecurityException();
            }
        }
    }

    @Test
    public void testGetCurrentStacktraceSlowPath() {
        final StackLocator stackLocator = StackLocator.getInstance();
        final Deque<Class<?>> classes = stackLocator.getCurrentStackTrace();
        Assertions.assertSame(StackLocator.class, classes.getFirst());
    }
}
