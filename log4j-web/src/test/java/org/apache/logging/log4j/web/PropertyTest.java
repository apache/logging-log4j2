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
package org.apache.logging.log4j.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.util.Constants;
import org.junit.jupiter.api.Test;

/**
 *
 */
class PropertyTest {

    @Test
    void testShutdownHookDisabled() {
        assertFalse(
                ((Log4jContextFactory) LogManager.getFactory()).isShutdownHookEnabled(),
                "Shutdown hook should be disabled by default in web applications");
    }

    @Test
    void testIsWebApp() {
        assertTrue(Constants.IS_WEB_APP, "When servlet classes are available IS_WEB_APP should default to true");
    }
}
