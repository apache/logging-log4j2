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
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.Hashtable;
import java.util.stream.Stream;

public class URLStreamHandlerFactories {
    private static final Field FACTORY_FIELD = Stream.of(URL.class.getDeclaredFields())
            .filter(field -> URLStreamHandlerFactory.class.equals(field.getType()))
            .findFirst()
            .orElseThrow(() -> new TestAbortedException("java.net.URL does not declare a java.net.URLStreamHandlerFactory field"));
    private static final Field HANDLERS_FIELD = FieldUtils.getDeclaredField(URL.class, "handlers", true);

    public static URLStreamHandlerFactory getURLStreamHandlerFactory() {
        try {
            return (URLStreamHandlerFactory) FieldUtils.readStaticField(FACTORY_FIELD, true);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setURLStreamHandlerFactory(final URLStreamHandlerFactory factory) {
        try {
            final Object handlers = HANDLERS_FIELD.get(null);
            if (handlers instanceof Hashtable<?, ?>) {
                ((Hashtable<?, ?>) handlers).clear();
            }
            FieldUtils.writeStaticField(FACTORY_FIELD, null, true);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
        if (factory != null) {
            URL.setURLStreamHandlerFactory(factory);
        }
    }
}
