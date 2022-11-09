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
package org.apache.logging.log4j.core.test.junit;

import java.util.stream.Stream;

import org.apache.logging.log4j.core.test.AvailablePortFinder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

class PortAllocatorCallback implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback {
    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        AnnotationSupport.findAnnotation(context.getTestClass(), AllocatePorts.class)
                .map(AllocatePorts::value)
                .stream()
                .flatMap(Stream::of)
                .forEach(PortAllocatorCallback::setSystemPropertyToAllocatedPort);
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        AnnotationSupport.findAnnotation(context.getTestMethod(), AllocatePorts.class)
                .map(AllocatePorts::value)
                .stream()
                .flatMap(Stream::of)
                .forEach(PortAllocatorCallback::setSystemPropertyToAllocatedPort);
    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        AnnotationSupport.findAnnotation(context.getTestMethod(), AllocatePorts.class)
                .map(AllocatePorts::value)
                .stream()
                .flatMap(Stream::of)
                .forEach(System::clearProperty);
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        AnnotationSupport.findAnnotation(context.getTestClass(), AllocatePorts.class)
                .map(AllocatePorts::value)
                .stream()
                .flatMap(Stream::of)
                .forEach(System::clearProperty);
    }

    private static void setSystemPropertyToAllocatedPort(final String key) {
        int port = AvailablePortFinder.getNextAvailable();
        System.setProperty(key, Integer.toString(port));
    }
}
