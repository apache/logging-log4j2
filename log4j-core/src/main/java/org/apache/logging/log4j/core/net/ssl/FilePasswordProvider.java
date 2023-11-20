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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * PasswordProvider that reads password from a file.
 * <p>
 * This is a relatively secure way to handle passwords:
 * <ul>
 *     <li>Managing file access privileges can be delegated to the operating system.</li>
 *     <li>The password file can be in a separate location from the logging configuration.
 *       This gives flexibility to have different passwords in different environments while
 *       using the same logging configuration. It also allows for separation of responsibilities:
 *       developers don't need to know the password that is used in the production environment.</li>
 *     <li>There is only a small window of opportunity for attackers to obtain the password from a memory
 *       dump: the password data is only resident in memory from the moment the caller calls the
 *       {@link #getPassword()} method and the password file is read until the moment that the caller
 *       completes authentication and overwrites the password char[] array.</li>
 * </ul>
 * </p><p>
 * Less secure implementations are {@link MemoryPasswordProvider} and {@link EnvironmentPasswordProvider}.
 * </p>
 */
class FilePasswordProvider implements PasswordProvider {
    private final Path passwordPath;

    /**
     * Constructs a new FilePasswordProvider with the specified path.
     * @param passwordFile the path to the password file
     * @throws NoSuchFileException if the password file does not exist when this FilePasswordProvider is constructed
     */
    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "The file name comes from a configuration option.")
    public FilePasswordProvider(final String passwordFile) throws NoSuchFileException {
        this.passwordPath = Paths.get(passwordFile);
        if (!Files.exists(passwordPath)) {
            throw new NoSuchFileException("PasswordFile '" + passwordFile + "' does not exist");
        }
    }

    @Override
    public char[] getPassword() {
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(passwordPath);
            final ByteBuffer bb = ByteBuffer.wrap(bytes);
            final CharBuffer decoded = Charset.defaultCharset().decode(bb);
            final char[] result = new char[decoded.limit()];
            decoded.get(result, 0, result.length);
            decoded.rewind();
            decoded.put(new char[result.length]); // erase decoded CharBuffer
            return result;
        } catch (final IOException e) {
            throw new IllegalStateException("Could not read password from " + passwordPath + ": " + e, e);
        } finally {
            if (bytes != null) {
                Arrays.fill(bytes, (byte) 0x0);
            }
        }
    }
}
