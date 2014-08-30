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

import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;

import org.apache.logging.log4j.Level;
import org.junit.Before;
import org.junit.Test;

public class LoggerBufferedReaderCallerInfoTest extends LoggerStreamsCallerInfoTesting {

    LoggerBufferedReader logReader;
    
    @Before
    public void setupReader() {
        final Reader srcReader = new StringReader("a\nb\nc\nd");
        logReader = new LoggerBufferedReader(srcReader, getLogger(), Level.WARN);
    }

    @Test
    public void read() throws Exception {
        logReader.read();

        assertMessages("read", 3, "read");
        logReader.close();
    }

    @Test
    public void readCbuf() throws Exception {
        logReader.read(new char[2]);

        assertMessages("read", 3, "readCbuf");
        logReader.close();
    }

    @Test
    public void readCbufOffset() throws Exception {
        logReader.read(new char[2], 0, 2);

        assertMessages("read", 3, "readCbufOffset");
        logReader.close();
    }

    @Test
    public void readCharBuffer() throws Exception {
        logReader.read(CharBuffer.allocate(2));

        assertMessages("read", 3, "readCharBuffer");
        logReader.close();
    }

    @Test
    public void readLine() throws Exception {
        logReader.readLine();

        assertMessages("read", 3, "readLine");
        logReader.close();
    }

    @Test
    public void close() throws Exception {
        logReader.readLine();
        assertMessages("before close", 3, "close");
        logReader.close();
        assertMessages("after close", 4, "close");
    }
}
