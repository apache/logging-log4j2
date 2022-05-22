/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.layout.template.json.util;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

class UrisTest {

    private static final Logger LOGGER = StatusLogger.getLogger();

    @Test
    void testClassPathResource() {
        final String content = Uris.readUri(
                "classpath:JsonLayout.json",
                StandardCharsets.US_ASCII);
        Assertions.assertThat(content).startsWith("{");
    }

    @Test
    void testFilePathResource() throws IOException {
        final String nonAsciiUtfText = "அஆஇฬ๘";
        final File file = Files.createTempFile("log4j-UriUtilTest-", ".txt").toFile();
        try {
            try (final OutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(nonAsciiUtfText.getBytes(StandardCharsets.UTF_8));
            }
            final URI uri = file.toURI();
            final String content = Uris.readUri(uri, StandardCharsets.UTF_8);
            Assertions.assertThat(content).isEqualTo(nonAsciiUtfText);
        } finally {
            final boolean deleted = file.delete();
            if (!deleted) {
                LOGGER.warn("could not delete temporary file: " + file);
            }
        }
    }

}
