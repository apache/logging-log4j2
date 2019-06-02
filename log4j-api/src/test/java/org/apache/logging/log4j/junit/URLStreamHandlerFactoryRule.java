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

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Hashtable;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

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
        final Field handlersFields = URL.class.getDeclaredField("handlers");
        if (handlersFields != null) {
            if (!handlersFields.isAccessible()) {
                handlersFields.setAccessible(true);
            }
            @SuppressWarnings("unchecked")
            final
            Hashtable<String, URLStreamHandler> handlers = (Hashtable<String, URLStreamHandler>) handlersFields
                    .get(null);
            if (handlers != null) {
                handlers.clear();
            }
        }
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Field factoryField = null;
                int matches = 0;
                URLStreamHandlerFactory oldFactory = null;
                for (final Field field : URL.class.getDeclaredFields()) {
                    if (URLStreamHandlerFactory.class.equals(field.getType())) {
                        factoryField = field;
                        matches++;
                        factoryField.setAccessible(true);
                        oldFactory = (URLStreamHandlerFactory) factoryField.get(null);
                        break;
                    }
                }
                Assert.assertNotNull("java.net URL does not declare a java.net.URLStreamHandlerFactory field",
                        factoryField);
                Assert.assertEquals("java.net.URL declares multiple java.net.URLStreamHandlerFactory fields.", 1,
                        matches); // FIXME There is a break in the loop so always 0 or 1
                URL.setURLStreamHandlerFactory(newURLStreamHandlerFactory);
                try {
                    base.evaluate();
                } finally {
                    clearURLHandlers();
                    factoryField.set(null, null);
                    URL.setURLStreamHandlerFactory(oldFactory);
                }
            }
        };
    }
}
