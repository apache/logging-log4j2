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

package org.apache.logging.log4j.test.junit;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

class PropertyTester implements BeforeAllCallback, BeforeEachCallback {
    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        final TestProperties testProperties = context.getRequiredTestClass().getAnnotation(TestProperties.class);
        if (testProperties != null) {
            context.getStore(ExtensionContext.Namespace.create(TestProperties.class, context.getRequiredTestClass()))
                    .put(PropertiesSession.class, new PropertiesSession(testProperties));
        }
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        final TestProperties testProperties = context.getRequiredTestMethod().getAnnotation(TestProperties.class);
        if (testProperties != null) {
            context.getStore(ExtensionContext.Namespace.create(TestProperties.class, context.getRequiredTestInstance()))
                    .put(PropertiesSession.class, new PropertiesSession(testProperties));
        }
    }

    private static class PropertiesSession implements ExtensionContext.Store.CloseableResource {
        private final Properties properties = new Properties();

        private PropertiesSession(final TestProperties testProperties) throws IOException {
            for (final String testProperty : testProperties.value()) {
                properties.load(new StringReader(testProperty));
            }
            for (final String propertyName : properties.stringPropertyNames()) {
                System.setProperty(propertyName, properties.getProperty(propertyName));
            }
        }

        @Override
        public void close() throws Throwable {
            for (final String propertyName : properties.stringPropertyNames()) {
                System.clearProperty(propertyName);
            }
            properties.clear();
        }
    }
}
