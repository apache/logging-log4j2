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

package org.apache.logging.log4j.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.util.PropertySource;

@Plugin(name = "test", category = StrLookup.CATEGORY)
public class TestPropertySource implements PropertySource, StrLookup {

    private static final ThreadLocal<PropertiesStack> STACK = ThreadLocal.withInitial(PropertiesStack::new);
    private static final String PREFIX = "log4j2.";

    @Override
    public int getPriority() {
        // Highest priority
        return Integer.MIN_VALUE;
    }

    private static Properties getProperties(boolean create) {
        final PropertiesStack stack = STACK.get();
        Properties props = stack.peek();
        if (create && props == null) {
            props = new Properties();
            stack.push(props);
        }
        return props;
    }

    public static void setProperty(final String key, final String value) {
        getProperties(true).setProperty(key, value);
    }

    public static void setProperty(final String key, final boolean value) {
        setProperty(key, value ? "true" : "false");
    }

    public static void setProperty(final String key, final int value) {
        setProperty(key, Integer.toString(value));
    }

    public static void clearProperty(final String key) {
        final Properties props = getProperties(false);
        if (props != null) {
            props.remove(key);
        }
    }
    public static Properties peek() {
        return STACK.get().peek();
    }

    public static void push(Properties props) {
        STACK.get().push(props);
    }

    public static Properties pop() {
        final PropertiesStack stack = STACK.get();
        final Properties props = stack.pop();
        if (stack.isEmpty()) {
            STACK.remove();
        }
        return props;
    }

    @Override
    public CharSequence getNormalForm(Iterable<? extends CharSequence> tokens) {
        return PREFIX + Util.joinAsCamelCase(tokens);
    }

    @Override
    public String getProperty(String key) {
        final Properties props = getProperties(false);
        return props != null ? props.getProperty(key) : null;
    }

    @Override
    public boolean containsProperty(String key) {
        final Properties props = getProperties(false);
        return props != null ? props.containsKey(key) : false;
    }

    @Override
    public String lookup(String key) {
        return getProperty(key);
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return lookup(key);
    }

    private static class PropertiesStack {

        private final List<Properties> stack = new ArrayList<>();

        public Properties peek() {
            if (stack.isEmpty()) {
                return null;
            }
            final int last = stack.size() - 1;
            return stack.get(last);
        }

        public Properties pop() {
            if (stack.isEmpty()) {
                return null;
            }
            final int last = stack.size() - 1;
            return stack.remove(last);
        }

        public void push(final Properties props) {
            stack.add(props);
        }

        public boolean isEmpty() {
            return stack.isEmpty();
        }
    }
}
