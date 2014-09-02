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

import java.util.Locale;

import org.apache.logging.log4j.Level;
import org.junit.Before;
import org.junit.Test;

public class LoggerPrintWriterCallerInfoTest extends LoggerStreamsCallerInfoTesting {

    private LoggerPrintWriter logOut;
    
    @Test
    public void close() throws Exception {
        logOut.print("a\nb");
        assertMessages("before close size", 1, "close");
        logOut.close();
        assertMessages("after close size", 2, "close");
    }
    
    @Test
    public void print_boolean() throws Exception {
        logOut.print(true);
        assertMessages("print", 0, "print_boolean");
        logOut.println(true);
        assertMessages("println", 1, "print_boolean");
    }
    
    @Test
    public void print_char() throws Exception {
        logOut.print('a');
        assertMessages("print", 0, "print_char");
        logOut.println('b');
        assertMessages("println", 1, "print_char");
    }
    
    @Test
    public void print_chararray() throws Exception {
        logOut.print("a".toCharArray());
        assertMessages("print", 0, "print_chararray");
        logOut.println("b".toCharArray());
        assertMessages("println", 1, "print_chararray");
    }
    
    @Test
    public void print_double() throws Exception {
        logOut.print(1D);
        assertMessages("print", 0, "print_double");
        logOut.println(2D);
        assertMessages("println", 1, "print_double");
    }
    
    @Test
    public void print_float() throws Exception {
        logOut.print(1f);
        assertMessages("print", 0, "print_float");
        logOut.println(2f);
        assertMessages("println", 1, "print_float");
    }
    
    @Test
    public void print_int() throws Exception {
        logOut.print(1);
        assertMessages("print", 0, "print_int");
        logOut.println(2);
        assertMessages("println", 1, "print_int");
    }
    
    @Test
    public void print_long() throws Exception {
        logOut.print(1L);
        assertMessages("print", 0, "print_long");
        logOut.println(2L);
        assertMessages("println", 1, "print_long");
    }
    
    @Test
    public void print_object() throws Exception {
        logOut.print((Object) 'a');
        assertMessages("print", 0, "print_object");
        logOut.println((Object) 'b');
        assertMessages("println", 1, "print_object");
    }
    
    @Test
    public void print_printf() throws Exception {
        logOut.printf("a\n");
        assertMessages("println", 1, "print_printf");
    }
    
    @Test
    public void print_printf_locale() throws Exception {
        logOut.printf(Locale.getDefault(), "a\n");
        assertMessages("println", 1, "print_printf_locale");
    }
    
    @Test
    public void print_string() throws Exception {
        logOut.print("a");
        assertMessages("print", 0, "print_string");
        logOut.println("b");
        assertMessages("println", 1, "print_string");
    }
    
    @Before
    public void setupStreams() {
        logOut = new LoggerPrintWriter(getLogger(), Level.WARN);
    }
    
    @Test
    public void write_bytes() throws Exception {
        logOut.write("b\n".toCharArray());
        assertMessages("write", 1, "write_bytes");
    }
    
    @Test
    public void write_bytes_offset() throws Exception {
        logOut.write("c\n".toCharArray(), 0, 2);
        assertMessages("write", 1, "write_bytes_offset");
    }
    
    @Test
    public void write_int() throws Exception {
        logOut.write('a');
        assertMessages("write int", 0, "write_int");
        logOut.write('\n');
        assertMessages("write newline", 1, "write_int");
    }
}
