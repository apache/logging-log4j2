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

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.test.ThreadContextUtilityClass;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.platform.commons.support.AnnotationSupport;

class ThreadContextInitializer implements BeforeAllCallback, BeforeEachCallback {

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        if (AnnotationSupport.isAnnotated(context.getRequiredTestClass(), InitializesThreadContext.class)) {
            resetThreadContext(context);
        }
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        if (AnnotationSupport.isAnnotated(context.getRequiredTestMethod(), InitializesThreadContext.class)) {
            resetThreadContext(context);
        }
    }

    private void resetThreadContext(final ExtensionContext context) {
        ThreadContextUtilityClass.reset();
        // We use `CloseableResource` instead of `afterAll` to reset the
        // ThreadContextFactory
        // *after* the `@SetSystemProperty` extension has restored the properties
        ExtensionContextAnchor.setAttribute(
                ThreadContext.class,
                new CloseableResource() {
                    @Override
                    public void close() throws Throwable {
                        ThreadContextUtilityClass.reset();
                    }
                },
                context);
    }
}
