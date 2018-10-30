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

import java.io.OutputStream;

import org.apache.logging.log4j.Level;
import org.junit.Before;
import org.junit.Test;

public class LoggerOutputStreamCallerInfoTest extends IoBuilderCallerInfoTesting {

    private OutputStream logOut;
    
    @Before
    public void setupStreams() {
        this.logOut = IoBuilder.forLogger(getExtendedLogger()).setLevel(Level.WARN).buildOutputStream();
    }
    
    @Test
    public void write() throws Exception {
        this.logOut.write('a');
        assertMessages("before write int", 0, "write");
        this.logOut.write('\n');
        assertMessages("after write int", 1, "write");
        
        this.logOut.write("b\n".getBytes());
        assertMessages("after write byte array", 2, "write");

        this.logOut.write("c\n".getBytes(), 0, 2);
        assertMessages("after write byte array offset size", 3, "write");

        this.logOut.write('d');
        assertMessages("before close size", 3, "write");
        this.logOut.close();
        assertMessages("after close size", 4, "write");
    }
}
