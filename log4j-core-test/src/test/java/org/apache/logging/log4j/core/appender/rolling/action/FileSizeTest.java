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
package org.apache.logging.log4j.core.appender.rolling.action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.apache.logging.log4j.core.appender.rolling.FileSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

class FileSizeTest {

    @Test
    void testParse() {
        assertEquals(5 * 1024, FileSize.parse("5k", 0));
    }

    @Test
    @ResourceLock(Resources.LOCALE)
    void testParseInEurope() {
        // Caveat: Breaks the ability for this test to run in parallel with other tests :(
        final Locale previousDefault = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("de", "DE"));
            assertEquals(1000, FileSize.parse("1,000", 0));
        } finally {
            Locale.setDefault(previousDefault);
        }
    }
}
