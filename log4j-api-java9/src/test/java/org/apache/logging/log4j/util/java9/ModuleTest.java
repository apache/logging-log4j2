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
package org.apache.logging.log4j.util.java9;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModuleTest {

    @Test
    public void testOnModulePath() {
        final boolean onModulePath = (boolean) assertDoesNotThrow(() -> {
            final Class<?> moduleUtil = Class.forName("org.apache.logging.log4j.util.java9.ModuleUtil");
            final Method isOnModulePath = moduleUtil.getDeclaredMethod("isOnModulePath");
            return isOnModulePath.invoke(null);
        });
        assertTrue(onModulePath);
    }
}
