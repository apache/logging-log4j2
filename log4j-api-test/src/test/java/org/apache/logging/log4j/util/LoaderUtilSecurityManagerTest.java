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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.security.Permission;
import org.apache.logging.log4j.test.junit.SecurityManagerTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("java.lang.SecurityManager")
public class LoaderUtilSecurityManagerTest {
    @Rule
    public final SecurityManagerTestRule rule = new SecurityManagerTestRule(new TestSecurityManager());

    private static class TestSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(final Permission perm) {
            if (perm.equals(LoaderUtil.GET_CLASS_LOADER)) {
                throw new SecurityException("disabled");
            }
        }
    }

    @Test
    public void canGetClassLoaderThroughPrivileges() {
        assertFalse(LoaderUtil.GET_CLASS_LOADER_DISABLED);
        assertDoesNotThrow(() -> LoaderUtil.getClassLoader(LoaderUtilSecurityManagerTest.class, String.class));
    }
}
