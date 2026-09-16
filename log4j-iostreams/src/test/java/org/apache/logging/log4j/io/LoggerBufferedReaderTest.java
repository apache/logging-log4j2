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
package org.apache.logging.log4j.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.Reader;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.Test;

public class LoggerBufferedReaderTest extends LoggerReaderTest {
    private BufferedReader bufferedReader;

    LoggerBufferedReaderTest(LoggerContext context) {
        super(context);
    }

    @Override
    protected Reader createReader() {
        return this.bufferedReader = (BufferedReader) IoBuilder.forLogger(getExtendedLogger())
                .filter(this.wrapped)
                .setLevel(LEVEL)
                .setBuffered(true)
                .buildReader();
    }

    @Test
    public void testReadLine() throws Exception {
        assertEquals(FIRST, this.bufferedReader.readLine(), "first line");
        assertMessages(FIRST);
        assertEquals(LAST, this.bufferedReader.readLine(), "second line");
        assertMessages(FIRST, LAST);
    }
}
