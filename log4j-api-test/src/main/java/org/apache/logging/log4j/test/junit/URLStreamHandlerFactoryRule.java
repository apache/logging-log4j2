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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.Hashtable;
import java.util.stream.Stream;

/**
 * Installs and restores the URL URLStreamHandlerFactory before and after tests.
 * <p>
 * Might need tweaking for different JREs.
 * </p>
 */
public class URLStreamHandlerFactoryRule implements TestRule {

    public URLStreamHandlerFactoryRule() {
        this(null);
    }

    public URLStreamHandlerFactoryRule(final URLStreamHandlerFactory newURLStreamHandlerFactory) {
        this.newURLStreamHandlerFactory = newURLStreamHandlerFactory;
    }

    private final URLStreamHandlerFactory newURLStreamHandlerFactory;

    void clearURLHandlers() throws Exception {
        final Object handlers = FieldUtils.readDeclaredStaticField(URL.class, "handlers", true);
        if (handlers instanceof Hashtable<?, ?>) {
            ((Hashtable<?, ?>) handlers).clear();
        }
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final Field factoryField = Stream.of(URL.class.getDeclaredFields())
                        .filter(field -> URLStreamHandlerFactory.class.equals(field.getType()))
                        .findFirst()
                        .orElseThrow(() -> new TestAbortedException("java.net URL does not declare a java.net.URLStreamHandlerFactory field"));
                URLStreamHandlerFactory oldFactory = (URLStreamHandlerFactory) FieldUtils.readStaticField(factoryField, true);
                URL.setURLStreamHandlerFactory(newURLStreamHandlerFactory);
                try {
                    base.evaluate();
                } finally {
                    clearURLHandlers();
                    FieldUtils.writeStaticField(factoryField, null);
                    URL.setURLStreamHandlerFactory(oldFactory);
                }
            }
        };
    }
}
