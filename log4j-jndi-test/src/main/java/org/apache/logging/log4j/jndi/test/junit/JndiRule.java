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

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import org.apache.logging.log4j.jndi.JndiManager;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.osjava.sj.jndi.MemoryContext;

/**
 * JUnit rule to create a mock {@link Context} and bind an object to a name.
 *
 * @since 2.8
 */
@SuppressWarnings("BanJNDI")
public class JndiRule implements TestRule {

    static {
        final InitialContextFactoryBuilder factoryBuilder =
                factoryBuilderEnv -> factoryEnv -> new MemoryContext(new Hashtable<>()) {};
        try {
            NamingManager.setInitialContextFactoryBuilder(factoryBuilder);
        } catch (final NamingException error) {
            throw new RuntimeException(error);
        }
    }

    @Nullable
    private final String managerName;

    private final Map<String, Object> bindings;

    public JndiRule(final String name, final Object value) {
        this(null, Collections.singletonMap(name, value));
    }

    public JndiRule(@Nullable final String managerName, final String name, final Object value) {
        this(managerName, Collections.singletonMap(name, value));
    }

    public JndiRule(final Map<String, Object> bindings) {
        this(null, bindings);
    }

    public JndiRule(@Nullable final String managerName, final Map<String, Object> bindings) {
        this.managerName = managerName;
        this.bindings = requireNonNull(bindings, "bindings");
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                resetJndiManager();
                base.evaluate();
            }
        };
    }

    private void resetJndiManager() throws NamingException {
        if (JndiManager.isJndiEnabled()) {
            final Context context = getContext();
            clearBindings(context);
            addBindings(context);
        }
    }

    private Context getContext() {
        final JndiManager manager =
                managerName == null ? JndiManager.getDefaultManager() : JndiManager.getDefaultManager(managerName);
        @Nullable final Context context = manager.getContext();
        assertNotNull(context);
        return context;
    }

    private static void clearBindings(final Context context) throws NamingException {
        final Set<NameClassPair> existingBindings = StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(context.list("").asIterator(), 0), false)
                .collect(Collectors.toSet());
        existingBindings.forEach(binding -> {
            try {
                context.unbind(binding.getName());
            } catch (NamingException error) {
                throw new RuntimeException(error);
            }
        });
    }

    private void addBindings(Context context) throws NamingException {
        for (final Map.Entry<String, Object> entry : bindings.entrySet()) {
            final String name = entry.getKey();
            final Object object = entry.getValue();
            context.bind(name, object);
        }
    }
}
