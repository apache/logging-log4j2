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

package org.apache.logging.log4j.core.config.plugins.convert;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.rolling.action.Duration;
import org.apache.logging.log4j.core.layout.GelfLayout;
import org.apache.logging.log4j.core.net.Facility;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 * Tests {@link TypeConverters}.
 */
@RunWith(Parameterized.class)
public class TypeConvertersTest {

    @SuppressWarnings("boxing")
    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        final byte[] byteArray = {
                (byte) 0xc7,
                (byte) 0x73,
                (byte) 0x21,
                (byte) 0x8c,
                (byte) 0x7e,
                (byte) 0xc8,
                (byte) 0xee,
                (byte) 0x99 };
        return Arrays.asList(
            // Array format: value, expected, default, type
            new Object[][]{
                // boolean
                { "true", true, null, Boolean.class },
                { "false", false, null, Boolean.class },
                { "True", true, null, Boolean.class },
                { "TRUE", true, null, Boolean.class },
                { "blah", false, null, Boolean.class }, // TODO: is this acceptable? it's how Boolean.parseBoolean works
                { null, null, null, Boolean.class },
                { null, true, "true", Boolean.class },
                { "no", false, null, Boolean.class }, // TODO: see above
                { "true", true, "false", boolean.class },
                { "FALSE", false, "true", boolean.class },
                { null, false, "false", boolean.class },
                { "invalid", false, "false", boolean.class },
                // byte
                { "42", (byte)42, null, Byte.class },
                { "53", (byte)53, null, Byte.class },
                // char
                { "A", 'A', null, char.class },
                { "b", 'b', null, char.class },
                { "b0", null, null, char.class },
                // integer
                { "42", 42, null, Integer.class },
                { "53", 53, null, Integer.class },
                { "-16", -16, null, Integer.class },
                { "0", 0, null, Integer.class },
                { "n", null, null, Integer.class },
                { "n", 5, "5", Integer.class },
                { "4.2", null, null, Integer.class },
                { "4.2", 0, "0", Integer.class },
                { null, null, null, Integer.class },
                { "75", 75, "0", int.class },
                { "-30", -30, "0", int.class },
                { "0", 0, "10", int.class },
                { null, 10, "10", int.class },
                // longs
                { "55", 55L, null, Long.class },
                { "1234567890123456789", 1234567890123456789L, null, Long.class },
                { "123123123L", null, null, Long.class },
                { "123123123123", 123123123123L, null, Long.class },
                { "-987654321", -987654321L, null, Long.class },
                { "-45l", null, null, Long.class },
                { "0", 0L, null, Long.class },
                { "asdf", null, null, Long.class },
                { "3.14", null, null, Long.class },
                { "3.14", 0L, "0", Long.class },
                { "*3", 1000L, "1000", Long.class },
                { null, null, null, Long.class },
                { "3000", 3000L, "0", long.class },
                { "-543210", -543210L, "0", long.class },
                { "22.7", -53L, "-53", long.class },
                // short
                { "42", (short)42, null, short.class },
                { "53", (short)53, null, short.class },
                { "-16", (short)-16, null, Short.class },
                // Log4j
                // levels
                { "ERROR", Level.ERROR, null, Level.class },
                { "WARN", Level.WARN, null, Level.class },
                { "FOO", null, null, Level.class },
                { "FOO", Level.DEBUG, "DEBUG", Level.class },
                { "OFF", Level.OFF, null, Level.class },
                { null, null, null, Level.class },
                { null, Level.INFO, "INFO", Level.class },
                // results
                { "ACCEPT", Filter.Result.ACCEPT, null, Filter.Result.class },
                { "NEUTRAL", Filter.Result.NEUTRAL, null, Filter.Result.class },
                { "DENY", Filter.Result.DENY, null, Filter.Result.class },
                { "NONE", null, null, Filter.Result.class },
                { "NONE", Filter.Result.NEUTRAL, "NEUTRAL", Filter.Result.class },
                { null, null, null, Filter.Result.class },
                { null, Filter.Result.ACCEPT, "ACCEPT", Filter.Result.class },
                // syslog facilities
                { "KERN", Facility.KERN, "USER", Facility.class },
                { "mail", Facility.MAIL, "KERN", Facility.class },
                { "Cron", Facility.CRON, null, Facility.class },
                { "not a real facility", Facility.AUTH, "auth", Facility.class },
                { null, null, null, Facility.class },
                // GELF compression types
                { "GZIP", GelfLayout.CompressionType.GZIP, "GZIP", GelfLayout.CompressionType.class },
                { "ZLIB", GelfLayout.CompressionType.ZLIB, "GZIP", GelfLayout.CompressionType.class },
                { "OFF", GelfLayout.CompressionType.OFF, "GZIP", GelfLayout.CompressionType.class },
                // arrays
                { "123", "123".toCharArray(), null, char[].class },
                { "123", "123".getBytes(Charset.defaultCharset()), null, byte[].class },
                { "0xC773218C7EC8EE99", byteArray, null, byte[].class },
                { "0xc773218c7ec8ee99", byteArray, null, byte[].class },
                { "Base64:cGxlYXN1cmUu", "pleasure.".getBytes("US-ASCII"), null, byte[].class },
                // JRE
                // JRE Charset
                { "UTF-8", StandardCharsets.UTF_8, null, Charset.class },
                { "ASCII", Charset.forName("ASCII"), "UTF-8", Charset.class },
                { "Not a real charset", StandardCharsets.UTF_8, "UTF-8", Charset.class },
                { null, StandardCharsets.UTF_8, "UTF-8", Charset.class },
                { null, null, null, Charset.class },
                // JRE File
                { "c:/temp", new File("c:/temp"), null, File.class },
                // JRE Class
                { TypeConvertersTest.class.getName(), TypeConvertersTest.class, null, Class.class },
                { "boolean", boolean.class, null, Class.class },
                { "byte", byte.class, null, Class.class },
                { "char", char.class, null, Class.class },
                { "double", double.class, null, Class.class },
                { "float", float.class, null, Class.class },
                { "int", int.class, null, Class.class },
                { "long", long.class, null, Class.class },
                { "short", short.class, null, Class.class },
                { "void", void.class, null, Class.class },
                { "\t", Object.class, Object.class.getName(), Class.class },
                { "\n", null, null, Class.class },
                // JRE URL
                { "http://locahost", new URL("http://locahost"), null, URL.class },
                { "\n", null, null, URL.class },
                // JRE URI
                { "http://locahost", new URI("http://locahost"), null, URI.class },
                { "\n", null, null, URI.class },
                // JRE BigInteger
                { "9223372036854775817000", new BigInteger("9223372036854775817000"), null, BigInteger.class },
                { "\n", null, null, BigInteger.class },
                // JRE BigInteger
                { "9223372036854775817000.99999", new BigDecimal("9223372036854775817000.99999"), null, BigDecimal.class },
                { "\n", null, null, BigDecimal.class },
                // JRE Security Provider
                { Security.getProviders()[0].getName(), Security.getProviders()[0], null, Provider.class },
                { "\n", null, null, Provider.class },
                // Duration
                { "P7DT10H", Duration.parse("P7DT10H"), null, Duration.class },
                // JRE InetAddress
                { "127.0.0.1", InetAddress.getByName("127.0.0.1"), null, InetAddress.class },
                // JRE Path
                { "/path/to/file", Paths.get("/path", "to", "file"), null, Path.class },
                // JRE UUID
                { "8fd389fb-9154-4096-b52e-435bde4a1835", UUID.fromString("8fd389fb-9154-4096-b52e-435bde4a1835"), null, UUID.class },
            }
        );
    }

    private final String value;
    private final Object expected;
    private final String defaultValue;
    private final Class<?> clazz;

    public TypeConvertersTest(final String value, final Object expected, final String defaultValue, final Class<?> clazz) {
        this.value = value;
        this.expected = expected;
        this.defaultValue = defaultValue;
        this.clazz = clazz;
    }

    @Test
    public void testConvert() throws Exception {
        final Object actual = TypeConverters.convert(value, clazz, defaultValue);
        final String assertionMessage = "\nGiven: " + value + "\nDefault: " + defaultValue;
        if (expected != null && expected instanceof char[]) {
            assertArrayEquals(assertionMessage, (char[]) expected, (char[]) actual);
        } else if (expected != null && expected instanceof byte[]) {
            assertArrayEquals(assertionMessage, (byte[]) expected, (byte[]) actual);
        } else {
            assertEquals(assertionMessage, expected, actual);
        }}
}
