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
package org.apache.logging.log4j.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatusLoggerTest {
    private final PrintStream origOut = System.out;
    private final PrintStream origErr = System.err;
    private ByteArrayOutputStream outBuf;
    private ByteArrayOutputStream errBuf;

    @BeforeEach
    void setupStreams() {
        outBuf = new ByteArrayOutputStream();
        errBuf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outBuf));
        System.setErr(new PrintStream(errBuf));
    }

    @AfterEach
    void resetStreams() {
        System.setOut(origOut);
        System.setErr(origErr);
    }

    @Test
    void status_logger_writes_to_stderr_by_default() {
        StatusLogger statusLogger = new StatusLogger();
        statusLogger.error("Test message");

        assertEquals("", outBuf.toString());
        assertThat(errBuf.toString()).contains("Test message");
    }
}
