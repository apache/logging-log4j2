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
package org.apache.logging.log4j.junit;

import javax.naming.Context;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

/**
 * JUnit rule to create a mock {@link Context} and bind an object to a name.
 *
 * @since 2.8
 */
public class JndiRule implements TestRule {

    private final String name;
    private final Object value;

    public JndiRule(final String name, final Object value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                SimpleNamingContextBuilder.emptyActivatedContextBuilder().bind(name, value);
                base.evaluate();
            }
        };
    }

}
