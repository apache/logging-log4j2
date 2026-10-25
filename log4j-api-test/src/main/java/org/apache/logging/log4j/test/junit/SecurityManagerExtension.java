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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Sets a security manager for each test. The current security manager is first saved then restored after the test is
 * run.
 * <p>
 * Using a security manager can mess up other tests so this is best used from integration tests (classes that end in
 * "IT" instead of "Test" and "TestCase".)
 * </p>
 * <p>
 * Tests are aborted if the running JVM does not allow setting a security manager.
 * </p>
 *
 * @see SecurityManagerTestRule
 * @since 2.27.0
 */
public class SecurityManagerExtension implements BeforeEachCallback, AfterEachCallback {

    private final SecurityManager securityManager;

    private SecurityManager securityManagerBefore;

    /**
     * Constructs a new instance with the given {@link SecurityManager}.
     *
     * @param securityManager
     *            the {@link SecurityManager} to use while running a test.
     */
    public SecurityManagerExtension(final SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        securityManagerBefore = System.getSecurityManager();
        try {
            System.setSecurityManager(securityManager);
        } catch (final UnsupportedOperationException e) {
            Assumptions.abort("The JVM does not allow setting a security manager: " + e.getMessage());
        }
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        if (System.getSecurityManager() == securityManager) {
            System.setSecurityManager(securityManagerBefore);
        }
    }
}
