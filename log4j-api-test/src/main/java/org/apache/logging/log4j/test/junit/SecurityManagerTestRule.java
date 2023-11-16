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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Sets a security manager for a test run. The current security manager is first saved then restored after the test is
 * run.
 * <p>
 * Using a security manager can mess up other tests so this is best used from integration tests (classes that end in
 * "IT" instead of "Test" and "TestCase".)
 * </p>
 *
 * <p>
 * When this test rule is evaluated, it will:
 * </p>
 * <ol>
 * <li>Save the current SecurityManager.</li>
 * <li>Set the SecurityManager to the instance supplied to this rule.</li>
 * <li>Evaluate the test statement.</li>
 * <li>Reset the current SecurityManager to the one from step (1).</li>
 * </ol>
 *
 * @since 2.11.0
 */
public class SecurityManagerTestRule implements TestRule {

    /**
     * Constructs a new instance with the given {@link SecurityManager}.
     * <p>
     * When this test rule is evaluated, it will:
     * </p>
     * <ol>
     * <li>Save the current SecurityManager.</li>
     * <li>Set the SecurityManager to the instance supplied to this rule.</li>
     * <li>Evaluate the test statement.</li>
     * <li>Reset the current SecurityManager to the one from step (1).</li>
     * </ol>
     *
     * @param securityManager
     *            the {@link SecurityManager} to use while running a test.
     */
    public SecurityManagerTestRule(final SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    private SecurityManager securityManagerBefore;
    private final SecurityManager securityManager;

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    base.evaluate();
                } finally {
                    after();
                }
            }

            private void after() {
                System.setSecurityManager(securityManagerBefore);
            }

            private void before() {
                securityManagerBefore = System.getSecurityManager();
                System.setSecurityManager(securityManager);
            }
        };
    }
}
