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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.logging.log4j.Level;
import org.junit.Before;
import org.junit.Test;

public class LoggerInputStreamCallerInfoTest extends IoBuilderCallerInfoTesting {

    private InputStream logIn;

    @Test
    public void read() throws Exception {
        this.logIn.read();
        assertMessages("before read int size", 0, "read");
        this.logIn.read();
        assertMessages("after read int size", 1, "read");

        this.logIn.read(new byte[2]);
        assertMessages("after read bytes size", 2, "read");

        this.logIn.read(new byte[2], 0, 2);
        assertMessages("after read bytes offset size", 3, "read");

        this.logIn.read();
        assertMessages("before close size", 3, "read");
        this.logIn.close();
        assertMessages("after close size", 4, "read");
    }
    
    @Before
    public void setupStreams() {
        final InputStream srcInputStream = new ByteArrayInputStream("a\nb\nc\nd".getBytes());
        this.logIn = IoBuilder.forLogger(getLogger())
            .filter(srcInputStream)
            .setLevel(Level.WARN)
            .buildInputStream();
    }
}
