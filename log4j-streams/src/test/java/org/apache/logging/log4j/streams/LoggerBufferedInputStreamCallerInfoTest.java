/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.streams;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

public class LoggerBufferedInputStreamCallerInfoTest extends LoggerStreamsCallerInfoTesting {

    private LoggerBufferedInputStream logIn;
    
    @Test
    public void close() throws Exception {
        logIn.read();
        assertMessages("before close", 3, "close");
        logIn.close();
        assertMessages("after close", 4, "close");
    }
    
    @Test
    public void read() throws Exception {
        logIn.read();

        assertMessages("read", 3, "read");
        logIn.close();
    }

    @Test
    public void readBytes() throws Exception {
        logIn.read(new byte[2]);

        assertMessages("read", 3, "readBytes");
        logIn.close();
    }

    @Test
    public void readBytesOffsetLen() throws Exception {
        logIn.read(new byte[2], 0, 2);

        assertMessages("read", 3, "readBytesOffsetLen");
        logIn.close();
    }

    @Before
    public void setupStreams() {
        final InputStream srcInputStream = new ByteArrayInputStream("a\nb\nc\nd".getBytes());
        logIn = new LoggerBufferedInputStream(srcInputStream, getLogger(), LEVEL);
    }
}
