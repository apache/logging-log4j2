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
package org.apache.logging.log4j.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import java.io.File;
import java.io.FilePermission;
import java.lang.reflect.Method;
import java.lang.reflect.ReflectPermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Isolated
@ResourceLock(Resources.SYSTEM_PROPERTIES)
public class LoaderUtilTest {
    @BeforeEach
    @AfterEach
    public void reset() {
        LoaderUtil.forceTcclOnly = null;
    }

    @Test
    public void systemClassLoader() {
        final Thread thread = Thread.currentThread();
        final ClassLoader tccl = thread.getContextClassLoader();

        LoaderUtil.forceTcclOnly = true;
        final ClassLoader loader = new ClassLoader(tccl) {
            @Override
            public Enumeration<URL> getResources(final String name) {
                return Collections.emptyEnumeration();
            }
        };
        thread.setContextClassLoader(loader);
        try {
            assertEquals(0, LoaderUtil.findUrlResources("Log4j-charsets.properties").size());

            LoaderUtil.forceTcclOnly = false;
            assertEquals(1, LoaderUtil.findUrlResources("Log4j-charsets.properties").size());
        } finally {
            thread.setContextClassLoader(tccl);
        }
    }

    @Test
    @ResourceLock("java.lang.SecurityManager")
    public void securityManager() {
        assertDoesNotThrow(() -> {
            String separator = System.getProperty("path.separator");
            List<URL> classpath = new ArrayList<>();
            for (String entry : System.getProperty("java.class.path").split(separator)) {
                classpath.add(new File(entry).toURI().toURL());
            }
            for (String entry : System.getProperty("jdk.module.path").split(separator)) {
                classpath.add(new File(entry).toURI().toURL());
            }

            URLClassLoader classLoader = new URLClassLoader(classpath.toArray(new URL[0]), null);
            Class<?> thisClass = classLoader.loadClass("org.apache.logging.log4j.util.LoaderUtilTest");
            Method getClassLoaders = thisClass.getMethod("runUnderSecurityManager");
            getClassLoaders.invoke(null);
        });
    }

    @SuppressWarnings("unused")
    public static ClassLoader[] runUnderSecurityManager() {
        SecurityManager originalSM = System.getSecurityManager();
        CustomSecurityManager sm = new CustomSecurityManager();
        System.setSecurityManager(sm);
        try {
            return LoaderUtil.getClassLoaders();
        } finally {
            sm.enabled = false;
            System.setSecurityManager(originalSM);
        }
    }

    private static final class CustomSecurityManager extends SecurityManager {
        private boolean enabled = true;

        @Override
        public void checkPermission(Permission perm) {
            if (!enabled || perm instanceof FilePermission) {
                return;
            }

            // For some reason, these additional permissions are required in order to instantiate a lambda function
            // on JDK11, and LoaderUtil#isForceTccl uses lambda syntax
            if (perm instanceof RuntimePermission && "accessDeclaredMembers".equals((perm).getName())) {
                return;
            }
            if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                return;
            }
            super.checkPermission(perm);
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            if (!enabled || perm instanceof FilePermission) {
                return;
            }
            super.checkPermission(perm, context);
        }
    }
}
