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
package org.apache.logging.log4j.tojul;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

class JULLoggerTest {

    @Test
    void testNotNullEffectiveLevel() {
        // Emulates the root logger found in Tomcat, with a null level
        // See: https://bz.apache.org/bugzilla/show_bug.cgi?id=66184
        final java.util.logging.Logger julLogger = new java.util.logging.Logger("", null) {};
        final JULLogger logger = new JULLogger("", julLogger);
        assertEquals(Level.INFO, logger.getLevel());
    }
}
