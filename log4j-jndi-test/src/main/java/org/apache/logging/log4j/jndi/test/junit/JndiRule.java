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
package org.apache.logging.log4j.jndi.test.junit;

import java.util.Collections;
import java.util.Map;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.NamingManager;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit rule to create a mock {@link Context} and bind an object to a name.
 *
 * @since 2.8
 */
public class JndiRule implements TestRule {

    private final Map<String, Object> initialBindings;

    static {
        try {
            NamingManager.setInitialContextFactoryBuilder(JndiFactoryBuilder.INSTANCE);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public JndiRule(final String name, final Object value) {
        this(Collections.singletonMap(name, value));
    }

    public JndiRule(final Map<String, Object> initialBindings) {
        this.initialBindings = initialBindings;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            @SuppressWarnings("BanJNDI")
            public void evaluate() throws Throwable {
                final InitialContextFactory factory = JndiFactoryBuilder.INSTANCE.createInitialContextFactory(null);
                final Context context = factory.getInitialContext(null);
                final Name javaComp = new CompositeName("java:comp/env");

                try {
                    createSubcontexts(context, javaComp);
                    for (final Map.Entry<String, Object> entry : initialBindings.entrySet()) {
                        recursiveBind(context, entry.getKey(), entry.getValue());
                    }
                    base.evaluate();
                } finally {
                    context.unbind(javaComp);
                }
            }
        };
    }

    @SuppressWarnings("BanJNDI")
    private static Context createSubcontexts(final Context initialContext, final Name name) throws NamingException {
        Context currentContext = initialContext;
        for (int i = 0; i < name.size(); i++) {
            try {
                currentContext = currentContext.createSubcontext(name.get(i));
            } catch (NamingException e) {
                // Silent catch. Probably an object is already bound in the context.
                currentContext = (javax.naming.Context) currentContext.lookup(name.get(i));
            }
        }
        return currentContext;
    }

    /**
     * Binds to object to the JNDI context and creates subcontexts if necessary.
     * @param initialContext initial JNDI context.
     * @param key full name of the entry to add.
     * @param value object to bind
     * @throws NamingException if an object is already bound to that name
     * @see Context#bind(String, Object)
     */
    @SuppressWarnings("BanJNDI")
    public static void recursiveBind(final Context initialContext, final String key, final Object value)
            throws NamingException {
        final Name name = new CompositeName(key);
        final int lastIdx = name.size() - 1;
        final Context subcontext = createSubcontexts(initialContext, name.getPrefix(lastIdx));
        subcontext.bind(name.get(lastIdx), value);
    }
}
