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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.parallel.ResourceLock;

@EnabledForJreRange(max = JRE.JAVA_17)
@ResourceLock("java.lang.SecurityManager")
@ResourceLock("java.security.Policy")
class LoaderUtilSecurityManagerTest {
    private static class TestPolicy extends Policy {
        private final ProtectionDomain deniedDomain = LoaderUtilSecurityManagerTest.class.getProtectionDomain();
        private final Permission deniedPermission = LoaderUtil.GET_CLASS_LOADER;

        @Override
        public boolean implies(final ProtectionDomain domain, final Permission permission) {
            return domain != deniedDomain || !permission.equals(deniedPermission);
        }
    }

    @Test
    void canGetClassLoaderThroughPrivileges() {
        final ClassLoader expected = LoaderUtilSecurityManagerTest.class.getClassLoader();
        final Policy previousPolicy = Policy.getPolicy();
        final SecurityManager previousSecurityManager = System.getSecurityManager();
        final TestPolicy policy = new TestPolicy();
        final SecurityManager securityManager = new SecurityManager();
        try {
            Policy.setPolicy(policy);
            System.setSecurityManager(securityManager);
            LoaderUtil.GET_CLASS_LOADER_DISABLED.reset();
            assertThrows(SecurityException.class, () -> securityManager.checkPermission(LoaderUtil.GET_CLASS_LOADER));
            assertFalse(LoaderUtil.GET_CLASS_LOADER_DISABLED);
            assertSame(expected, LoaderUtil.getClassLoader(LoaderUtilSecurityManagerTest.class, String.class));
        } finally {
            System.setSecurityManager(previousSecurityManager);
            Policy.setPolicy(previousPolicy);
            LoaderUtil.GET_CLASS_LOADER_DISABLED.reset();
        }
    }
}
