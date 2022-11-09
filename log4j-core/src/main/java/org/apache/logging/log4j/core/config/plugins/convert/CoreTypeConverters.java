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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Provider;
import java.security.Security;
import java.time.ZoneId;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.rolling.action.Duration;
import org.apache.logging.log4j.core.util.CronExpression;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.convert.TypeConverters;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * General {@link TypeConverter} implementations.
 *
 * @since 2.1 Moved to the {@code convert} package.
 */
public final class CoreTypeConverters {

    private static final Base64.Decoder decoder = Base64.getDecoder();

    /**
     * Parses a {@link String} into a {@link BigDecimal}.
     */
    @TypeConverters
    @Plugin
    public static class BigDecimalConverter implements TypeConverter<BigDecimal> {
        @Override
        public BigDecimal convert(final String s) {
            return new BigDecimal(s);
        }
    }

    /**
     * Parses a {@link String} into a {@link BigInteger}.
     */
    @TypeConverters
    @Plugin
    public static class BigIntegerConverter implements TypeConverter<BigInteger> {
        @Override
        public BigInteger convert(final String s) {
            return new BigInteger(s);
        }
    }

    /**
     * Converts a {@link String} into a {@code byte[]}.
     * <p>
     * The supported formats are:
     * <ul>
     * <li>0x0123456789ABCDEF</li>
     * <li>Base64:ABase64String</li>
     * <li>String using {@link Charset#defaultCharset()} [TODO Should this be UTF-8 instead?]</li>
     * </ul>
     */
    @TypeConverters
    @Plugin
    public static class ByteArrayConverter implements TypeConverter<byte[]> {

        private static final String PREFIX_0x = "0x";
        private static final String PREFIX_BASE64 = "Base64:";

        @Override
        public byte[] convert(final String value) {
            byte[] bytes;
            if (value == null || value.isEmpty()) {
                bytes = new byte[0];
            } else if (value.startsWith(PREFIX_BASE64)) {
                final String lexicalXSDBase64Binary = value.substring(PREFIX_BASE64.length());
                bytes = decoder.decode(lexicalXSDBase64Binary);
            } else if (value.startsWith(PREFIX_0x)) {
                final String lexicalXSDHexBinary = value.substring(PREFIX_0x.length());
                bytes = HexConverter.parseHexBinary(lexicalXSDHexBinary);
            } else {
                bytes = value.getBytes(Charset.defaultCharset());
            }
            return bytes;
        }
    }

    /**
     * Converts a {@link String} into a {@code char[]}.
     */
    @TypeConverters
    @Plugin
    public static class CharArrayConverter implements TypeConverter<char[]> {
        @Override
        public char[] convert(final String s) {
            return s.toCharArray();
        }
    }

    /**
     * Converts a {@link String} into a {@link Charset}.
     */
    @TypeConverters
    @Plugin
    public static class CharsetConverter implements TypeConverter<Charset> {
        @Override
        public Charset convert(final String s) {
            return Charset.forName(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Class}.
     */
    @TypeConverters
    @Plugin
    public static class ClassConverter implements TypeConverter<Class<?>> {
        @Override
        public Class<?> convert(final String s) throws ClassNotFoundException {
            switch (s.toLowerCase()) {
                case "boolean":
                    return boolean.class;
                case "byte":
                    return byte.class;
                case "char":
                    return char.class;
                case "double":
                    return double.class;
                case "float":
                    return float.class;
                case "int":
                    return int.class;
                case "long":
                    return long.class;
                case "short":
                    return short.class;
                case "void":
                    return void.class;
                default:
                    return LoaderUtil.loadClass(s);
            }

        }
    }

    @TypeConverters
    @Plugin
    public static class CronExpressionConverter implements TypeConverter<CronExpression> {
        @Override
        public CronExpression convert(final String s) throws Exception {
            return new CronExpression(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Duration}.
     *
     * @since 2.5
     */
    @TypeConverters
    @Plugin
    public static class DurationConverter implements TypeConverter<Duration> {
        @Override
        public Duration convert(final String s) {
            return Duration.parse(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link File}.
     */
    @TypeConverters
    @Plugin
    public static class FileConverter implements TypeConverter<File> {
        @Override
        public File convert(final String s) {
            return new File(s);
        }
    }

    /**
     * Converts a {@link String} into an {@link InetAddress}.
     */
    @TypeConverters
    @Plugin
    public static class InetAddressConverter implements TypeConverter<InetAddress> {
        @Override
        public InetAddress convert(final String s) throws Exception {
            return InetAddress.getByName(s);
        }
    }

    /**
     * Converts a {@link String} into a Log4j {@link Level}. Returns {@code null} for invalid level names.
     */
    @TypeConverters
    @Plugin
    public static class LevelConverter implements TypeConverter<Level> {
        @Override
        public Level convert(final String s) {
            return Level.valueOf(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Path}.
     *
     * @since 2.8
     */
    @TypeConverters
    @Plugin
    public static class PathConverter implements TypeConverter<Path> {
        @Override
        public Path convert(final String s) throws Exception {
            return Paths.get(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Pattern}.
     */
    @TypeConverters
    @Plugin
    public static class PatternConverter implements TypeConverter<Pattern> {
        @Override
        public Pattern convert(final String s) {
            return Pattern.compile(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Provider}.
     */
    @TypeConverters
    @Plugin
    public static class SecurityProviderConverter implements TypeConverter<Provider> {
        @Override
        public Provider convert(final String s) {
            return Security.getProvider(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link URI}.
     */
    @TypeConverters
    @Plugin
    public static class UriConverter implements TypeConverter<URI> {
        @Override
        public URI convert(final String s) throws URISyntaxException {
            return new URI(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link URL}.
     */
    @TypeConverters
    @Plugin
    public static class UrlConverter implements TypeConverter<URL> {
        @Override
        public URL convert(final String s) throws MalformedURLException {
            return new URL(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link UUID}.
     *
     * @since 2.8
     */
    @TypeConverters
    @Plugin
    public static class UuidConverter implements TypeConverter<UUID> {
        @Override
        public UUID convert(final String s) throws Exception {
            return UUID.fromString(s);
        }
    }

    @TypeConverters
    @Plugin
    public static class ZoneIdConverter implements TypeConverter<ZoneId> {
        @Override
        public ZoneId convert(final String s) throws Exception {
            return ZoneId.of(s);
        }
    }
}
