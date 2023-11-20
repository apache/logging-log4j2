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
package org.apache.logging.log4j.core.net.ssl;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class FilePasswordProviderTest {

    @Test
    public void testGetPassword() throws Exception {
        final String PASSWORD = "myPass123";
        final Path path = Files.createTempFile("testPass", ".txt");
        Files.write(path, PASSWORD.getBytes(Charset.defaultCharset()));

        final char[] actual = new FilePasswordProvider(path.toString()).getPassword();
        Files.delete(path);
        assertArrayEquals(PASSWORD.toCharArray(), actual);
    }

    @Test
    public void testConstructorDisallowsNull() throws Exception {
        assertThrows(NullPointerException.class, () -> new FilePasswordProvider(null));
    }

    @Test
    public void testConstructorFailsIfFileDoesNotExist() throws Exception {
        assertThrows(NoSuchFileException.class, () -> new FilePasswordProvider("nosuchfile"));
    }
}
