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
package org.apache.logging.log4j.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

public class LoggerBufferedInputStreamCallerInfoTest extends IoBuilderCallerInfoTesting {

    private BufferedInputStream logIn;

    @Test
    public void close() throws Exception {
        this.logIn.read();
        assertMessages("before close", 3, "close");
        this.logIn.close();
        assertMessages("after close", 4, "close");
    }
    
    @Test
    public void read() throws Exception {
        this.logIn.read();

        assertMessages("read", 3, "read");
        this.logIn.close();
    }

    @Test
    public void readBytes() throws Exception {
        this.logIn.read(new byte[2]);

        assertMessages("read", 3, "readBytes");
        this.logIn.close();
    }

    @Test
    public void readBytesOffsetLen() throws Exception {
        this.logIn.read(new byte[2], 0, 2);

        assertMessages("read", 3, "readBytesOffsetLen");
        this.logIn.close();
    }

    @Before
    public void setupStreams() {
        final InputStream srcInputStream = new ByteArrayInputStream("a\nb\nc\nd".getBytes());
        this.logIn = (BufferedInputStream)
            IoBuilder.forLogger(getLogger())
                .filter(srcInputStream)
                .setLevel(LEVEL)
                .setBuffered(true)
                .buildInputStream();
    }
}
