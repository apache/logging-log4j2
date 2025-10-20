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
package org.apache.logging.log4j.core.appender;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

class DefaultLayoutTest {
    @Test
    void testDefaultLayout() {
        PrintStream standardOutput = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        System.setOut(new PrintStream(baos));
        try {
            Logger log = LogManager.getLogger(getClass());
            log.fatal("This is a fatal message");
            log.error("This is an error message");
            log.warn("This is a warning message");

            String actualOutput = new String(baos.toByteArray(), Charset.defaultCharset());
            assertTrue(actualOutput.contains("FATAL This is a fatal message" + System.lineSeparator()));
            assertTrue(actualOutput.contains("ERROR This is an error message" + System.lineSeparator()));
            assertFalse(actualOutput.contains("This is a warning message"));
        } finally {
            System.setOut(standardOutput);
        }
    }
}
