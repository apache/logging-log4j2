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
package org.apache.logging.log4j.core.selector;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.lang.reflect.Field;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClassLoaderContextSelectorTest {

    private static final String PKG =
            ClassLoaderContextSelectorTest.class.getPackage().getName();

    private ClassLoader loader1, loader2, loader3;

    @BeforeEach
    void setUp() {
        loader1 = new TestClassLoader();
        loader2 = new TestClassLoader();
        loader3 = new TestClassLoader();
        assertNotSame(loader1, loader2);
        assertNotSame(loader1, loader3);
        assertNotSame(loader2, loader3);
    }

    @Test
    void testMultipleClassLoaders() throws Exception {
        final Class<?> logging1 = loader1.loadClass(PKG + ".a.Logging1");
        final Field field1 = logging1.getDeclaredField("logger");
        final Logger logger1 = (Logger) ReflectionUtil.getStaticFieldValue(field1);
        assertNotNull(logger1);
        final Class<?> logging2 = loader2.loadClass(PKG + ".b.Logging2");
        final Field field2 = logging2.getDeclaredField("logger");
        final Logger logger2 = (Logger) ReflectionUtil.getStaticFieldValue(field2);
        assertNotNull(logger2);
        final Class<?> logging3 = loader3.loadClass(PKG + ".c.Logging3");
        final Field field3 = logging3.getDeclaredField("logger");
        final Logger logger3 = (Logger) ReflectionUtil.getStaticFieldValue(field3);
        assertNotNull(logger3);
        assertNotSame(logger1.getContext(), logger2.getContext());
        assertNotSame(logger1.getContext(), logger3.getContext());
        assertNotSame(logger2.getContext(), logger3.getContext());
    }
}
