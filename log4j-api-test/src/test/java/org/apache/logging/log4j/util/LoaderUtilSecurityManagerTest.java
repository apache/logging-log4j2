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
package org.apache.logging.log4j.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.security.Permission;
import org.apache.logging.log4j.test.junit.SecurityManagerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("java.lang.SecurityManager")
public class LoaderUtilSecurityManagerTest {
    @RegisterExtension
    public final SecurityManagerExtension ext = new SecurityManagerExtension(new TestSecurityManager());

    private static class TestSecurityManager extends SecurityManager {
        // Resolved before installation: loading `LoaderUtil` from `checkPermission` recurses until the stack overflows
        private final Permission getClassLoader = LoaderUtil.GET_CLASS_LOADER;

        @Override
        public void checkPermission(final Permission perm) {
            if (perm.equals(getClassLoader) && !isPrivileged()) {
                throw new SecurityException("disabled");
            }
        }

        // Not `getClassContext()`: it omits native frames, and `doPrivileged` is native on Java 8
        private boolean isPrivileged() {
            for (final StackTraceElement element : new Throwable().getStackTrace()) {
                if ("java.security.AccessController".equals(element.getClassName())
                        && "doPrivileged".equals(element.getMethodName())) {
                    return true;
                }
            }
            return false;
        }
    }

    @Test
    public void canGetClassLoaderThroughPrivileges() {
        assertDoesNotThrow(() -> LoaderUtil.getClassLoader(LoaderUtilSecurityManagerTest.class, String.class));
    }
}
