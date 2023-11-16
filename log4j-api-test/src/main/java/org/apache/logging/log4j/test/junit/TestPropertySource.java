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

import org.apache.logging.log4j.test.TestProperties;
import org.apache.logging.log4j.util.PropertySource;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

public class TestPropertySource implements PropertySource {

    private static final String PREFIX = "log4j2.";
    private static final Namespace NAMESPACE = ExtensionContextAnchor.LOG4J2_NAMESPACE.append("properties");
    private static final TestProperties EMPTY_PROPERTIES = new EmptyTestProperties();

    @Override
    public int getPriority() {
        // Highest priority
        return Integer.MIN_VALUE;
    }

    public static TestProperties createProperties(final ExtensionContext context) {
        TestProperties props = getProperties(context);
        // Make sure that the properties do not come from the parent ExtensionContext
        if (props instanceof JUnitTestProperties && context.equals(((JUnitTestProperties) props).getContext())) {
            return props;
        }
        props = new JUnitTestProperties(context);
        ExtensionContextAnchor.setAttribute(TestProperties.class, props, context);
        return props;
    }

    public static TestProperties getProperties() {
        return getProperties(null);
    }

    private static TestProperties getProperties(final ExtensionContext context) {
        final ExtensionContext actualContext = context != null ? context : ExtensionContextAnchor.getContext();
        if (actualContext != null) {
            final TestProperties props =
                    ExtensionContextAnchor.getAttribute(TestProperties.class, TestProperties.class, actualContext);
            if (props != null) {
                return props;
            }
        }
        return EMPTY_PROPERTIES;
    }

    @Override
    public CharSequence getNormalForm(final Iterable<? extends CharSequence> tokens) {
        final CharSequence camelCase = Util.joinAsCamelCase(tokens);
        // Do not use Strings to prevent recursive initialization
        return camelCase.length() > 0 ? PREFIX + camelCase.toString() : null;
    }

    @Override
    public String getProperty(final String key) {
        return getProperties().getProperty(key);
    }

    @Override
    public boolean containsProperty(final String key) {
        return getProperties().containsProperty(key);
    }

    private static class JUnitTestProperties implements TestProperties {

        private final ExtensionContext context;
        private final Store store;

        public JUnitTestProperties(final ExtensionContext context) {
            this.context = context;
            this.store = context.getStore(NAMESPACE);
        }

        public ExtensionContext getContext() {
            return context;
        }

        @Override
        public String getProperty(final String key) {
            return store.get(key, String.class);
        }

        @Override
        public boolean containsProperty(final String key) {
            return getProperty(key) != null;
        }

        @Override
        public void setProperty(final String key, final String value) {
            store.put(key, value);
        }

        @Override
        public void clearProperty(final String key) {
            store.remove(key, String.class);
        }
    }

    private static class EmptyTestProperties implements TestProperties {

        @Override
        public String getProperty(final String key) {
            return null;
        }

        @Override
        public boolean containsProperty(final String key) {
            return false;
        }

        @Override
        public void setProperty(final String key, final String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clearProperty(final String key) {
            throw new UnsupportedOperationException();
        }
    }
}
