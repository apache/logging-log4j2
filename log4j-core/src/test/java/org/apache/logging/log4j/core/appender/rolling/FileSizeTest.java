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
package org.apache.logging.log4j.core.appender.rolling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link FileSize}.
 */
public class FileSizeTest {

    void testFileSizeNull() {
        Exception ex = assertThrows(NullPointerException.class, () -> FileSize.parse(null));
        assertEquals("File size expression required", ex.getMessage());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"", "abc", "x GB", "x TB"})
    void testFileSizeInvalid(String expr) {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> FileSize.parse(expr));
        assertEquals("Unsupported file size expression '" + expr + "'", ex.getMessage());
    }

    @Test
    void testFileSizeZero() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> FileSize.parse("0"));
        assertEquals("File size must be > 0", ex.getMessage());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource(delimiter = ':', value = {
            "10:10",
            "10KB:10240",
            "10 KB:10240",
            "10 kb:10240",
            " 10 kb :10240",
            "0,1 MB:104857",
            "1 MB:1048576",
            "10 MB:10485760",
            "10.45 MB:10957619",
            "10.75 MB:11272192",
            "10,75 MB:11272192",
            "1 GB:1073741824",
            "0.51 GB:547608330"
    })
    void testValidFileSizes(String expr, long expected) {
        assertEquals(expected, FileSize.parse(expr));
    }

}
