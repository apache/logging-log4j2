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

import org.junit.Before;
import org.junit.Test;

public class LoggerReaderCallerInfoTest extends LoggerStreamsCallerInfoTesting {

    LoggerReader logReader;
    
    @Test
    public void read() throws Exception {
        logReader.read();
        assertMessages("before read int size", 0, "read");
        logReader.read();
        assertMessages("after read int size", 1, "read");

        logReader.read(new char[2]);
        assertMessages("after read bytes size", 2, "read");

        logReader.read(new char[2], 0, 2);
        assertMessages("after read bytes offset size", 3, "read");

        logReader.read(CharBuffer.allocate(2));
        assertMessages("after read charBuffer size", 4, "read");

        logReader.read();
        assertMessages("before close size", 4, "read");
        logReader.close();
        assertMessages("after close size", 5, "read");
    }

    @Before
    public void setupReader() {
        final Reader srcReader = new StringReader("a\nb\nc\nd\ne");
        logReader = new LoggerReader(srcReader, getLogger(), LEVEL);
    }
}
