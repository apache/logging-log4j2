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
package org.apache.logging.log4j.core.test;

import java.util.Objects;
import java.util.function.Supplier;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A JUnit TestRule to set and reset a system property during a test.
 */
public class SystemPropertyTestRule implements TestRule {

    public static SystemPropertyTestRule create(final String name, final String value) {
        return new SystemPropertyTestRule(name, value);
    }

    private final String name;
    private final Supplier<String> valueSupplier;
    private String value;

    protected SystemPropertyTestRule(final String name, final String value) {
        this(name, () -> value);
    }

    protected SystemPropertyTestRule(final String name, final Supplier<String> value) {
        this.name = Objects.requireNonNull(name, "name");
        this.valueSupplier = value;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                final String oldValue = System.getProperty(name);
                try {
                    value = valueSupplier.get();
                    System.setProperty(name, value);
                    base.evaluate();
                } finally {
                    // Restore if previously set
                    if (oldValue != null) {
                        System.setProperty(name, oldValue);
                    }
                }
            }
        };
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public Supplier<String> getValueSupplier() {
        return valueSupplier;
    }

    @Override
    public String toString() {
        // Value might be a secret...
        return "SystemPropertyTestRule [name=" + name + "]";
    }
}
