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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A JUnit TestRule to discover an available port and save it in a system property. Useful for setting up tests using
 * Apache Active MQ.
 */
public class AvailablePortSystemPropertyTestRule implements TestRule {

    public static AvailablePortSystemPropertyTestRule create(final String name) {
        return new AvailablePortSystemPropertyTestRule(name);
    }

    protected final String name;
    protected int port;

    protected AvailablePortSystemPropertyTestRule(final String name) {
        this.name = name;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                final String oldValue = System.getProperty(name);
                try {
                    port = AvailablePortFinder.getNextAvailable();
                    System.setProperty(name, Integer.toString(port));
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

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("AvailablePortSystemPropertyRule [name=");
        builder.append(name);
        builder.append(", port=");
        builder.append(port);
        builder.append("]");
        return builder.toString();
    }

}
