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
package org.apache.logging.log4j.core.config.plugins.convert;

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.rolling.action.Duration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.CronExpression;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Collection of basic TypeConverter implementations. May be used to register additional TypeConverters or find
 * registered TypeConverters.
 *
 * @since 2.1 Moved to the {@code convert} package.
 */
public final class TypeConverters {

    /**
     * The {@link Plugin#category() Plugin Category} to use for {@link TypeConverter} plugins.
     *
     * @since 2.1
     */
    public static final String CATEGORY = "TypeConverter";

    /**
     * Parses a {@link String} into a {@link BigDecimal}.
     */
    @Plugin(name = "BigDecimal", category = CATEGORY)
    public static class BigDecimalConverter implements TypeConverter<BigDecimal> {
        @Override
        public BigDecimal convert(final String s) {
            return new BigDecimal(s);
        }
    }

    /**
     * Parses a {@link String} into a {@link BigInteger}.
     */
    @Plugin(name = "BigInteger", category = CATEGORY)
    public static class BigIntegerConverter implements TypeConverter<BigInteger> {
        @Override
        public BigInteger convert(final String s) {
            return new BigInteger(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Boolean}.
     */
    @Plugin(name = "Boolean", category = CATEGORY)
    public static class BooleanConverter implements TypeConverter<Boolean> {
        @Override
        public Boolean convert(final String s) {
            return Boolean.valueOf(s);
        }
    }

    /**
     * Converts a {@link String} into a {@code byte[]}.
     *
     * The supported formats are:
     * <ul>
     * <li>0x0123456789ABCDEF</li>
     * <li>Base64:ABase64String</li>
     * <li>String using {@link Charset#defaultCharset()} [TODO Should this be UTF-8 instead?]</li>
     * </ul>
     */
    @Plugin(name = "ByteArray", category = CATEGORY)
    public static class ByteArrayConverter implements TypeConverter<byte[]> {

        private static final String PREFIX_0x = "0x";
        private static final String PREFIX_BASE64 = "Base64:";

        @Override
        public byte[] convert(final String value) {
            byte[] bytes;
            if (value == null || value.isEmpty()) {
                bytes = Constants.EMPTY_BYTE_ARRAY;
            } else if (value.startsWith(PREFIX_BASE64)) {
                final String lexicalXSDBase64Binary = value.substring(PREFIX_BASE64.length());
                bytes = Base64Converter.parseBase64Binary(lexicalXSDBase64Binary);
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
     * Converts a {@link String} into a {@link Byte}.
     */
    @Plugin(name = "Byte", category = CATEGORY)
    public static class ByteConverter implements TypeConverter<Byte> {
        @Override
        public Byte convert(final String s) {
            return Byte.valueOf(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Character}.
     */
    @Plugin(name = "Character", category = CATEGORY)
    public static class CharacterConverter implements TypeConverter<Character> {
        @Override
        public Character convert(final String s) {
            if (s.length() != 1) {
                throw new IllegalArgumentException("Character string must be of length 1: " + s);
            }
            return Character.valueOf(s.toCharArray()[0]);
        }
    }

    /**
     * Converts a {@link String} into a {@code char[]}.
     */
    @Plugin(name = "CharacterArray", category = CATEGORY)
    public static class CharArrayConverter implements TypeConverter<char[]> {
        @Override
        public char[] convert(final String s) {
            return s.toCharArray();
        }
    }

    /**
     * Converts a {@link String} into a {@link Charset}.
     */
    @Plugin(name = "Charset", category = CATEGORY)
    public static class CharsetConverter implements TypeConverter<Charset> {
        @Override
        public Charset convert(final String s) {
            return Charset.forName(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Class}.
     */
    @Plugin(name = "Class", category = CATEGORY)
    public static class ClassConverter implements TypeConverter<Class<?>> {
        @Override
        public Class<?> convert(final String s) throws ClassNotFoundException {
            switch (toRootLowerCase(s)) {
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

    @Plugin(name = "CronExpression", category = CATEGORY)
    public static class CronExpressionConverter implements TypeConverter<CronExpression> {
        @Override
        public CronExpression convert(final String s) throws Exception {
            return new CronExpression(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Double}.
     */
    @Plugin(name = "Double", category = CATEGORY)
    public static class DoubleConverter implements TypeConverter<Double> {
        @Override
        public Double convert(final String s) {
            return Double.valueOf(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Duration}.
     * @since 2.5
     */
    @Plugin(name = "Duration", category = CATEGORY)
    public static class DurationConverter implements TypeConverter<Duration> {
        @Override
        public Duration convert(final String s) {
            return Duration.parse(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link File}.
     */
    @Plugin(name = "File", category = CATEGORY)
    public static class FileConverter implements TypeConverter<File> {
        @Override
        @SuppressFBWarnings(
                value = "PATH_TRAVERSAL_IN",
                justification = "The name of the accessed file is based on a configuration value.")
        public File convert(final String s) {
            return new File(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Float}.
     */
    @Plugin(name = "Float", category = CATEGORY)
    public static class FloatConverter implements TypeConverter<Float> {
        @Override
        public Float convert(final String s) {
            return Float.valueOf(s);
        }
    }

    /**
     * Converts a {@link String} into an {@link InetAddress}.
     */
    @Plugin(name = "InetAddress", category = CATEGORY)
    public static class InetAddressConverter implements TypeConverter<InetAddress> {
        @Override
        public InetAddress convert(final String s) throws Exception {
            return InetAddress.getByName(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Integer}.
     */
    @Plugin(name = "Integer", category = CATEGORY)
    public static class IntegerConverter implements TypeConverter<Integer> {
        @Override
        public Integer convert(final String s) {
            return Integer.valueOf(s);
        }
    }

    /**
     * Converts a {@link String} into a Log4j {@link Level}. Returns {@code null} for invalid level names.
     */
    @Plugin(name = "Level", category = CATEGORY)
    public static class LevelConverter implements TypeConverter<Level> {
        @Override
        public Level convert(final String s) {
            return Level.valueOf(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Long}.
     */
    @Plugin(name = "Long", category = CATEGORY)
    public static class LongConverter implements TypeConverter<Long> {
        @Override
        public Long convert(final String s) {
            return Long.valueOf(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Path}.
     * @since 2.8
     */
    @Plugin(name = "Path", category = CATEGORY)
    public static class PathConverter implements TypeConverter<Path> {
        @Override
        @SuppressFBWarnings(
                value = "PATH_TRAVERSAL_IN",
                justification = "The name of the accessed file is based on a configuration value.")
        public Path convert(final String s) throws Exception {
            return Paths.get(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Pattern}.
     */
    @Plugin(name = "Pattern", category = CATEGORY)
    public static class PatternConverter implements TypeConverter<Pattern> {
        @Override
        public Pattern convert(final String s) {
            return Pattern.compile(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Provider}.
     */
    @Plugin(name = "SecurityProvider", category = CATEGORY)
    public static class SecurityProviderConverter implements TypeConverter<Provider> {
        @Override
        public Provider convert(final String s) {
            return Security.getProvider(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link Short}.
     */
    @Plugin(name = "Short", category = CATEGORY)
    public static class ShortConverter implements TypeConverter<Short> {
        @Override
        public Short convert(final String s) {
            return Short.valueOf(s);
        }
    }

    /**
     * Returns the given {@link String}, no conversion takes place.
     */
    @Plugin(name = "String", category = CATEGORY)
    public static class StringConverter implements TypeConverter<String> {
        @Override
        public String convert(final String s) {
            return s;
        }
    }

    /**
     * Converts a {@link String} into a {@link URI}.
     */
    @Plugin(name = "URI", category = CATEGORY)
    public static class UriConverter implements TypeConverter<URI> {
        @Override
        public URI convert(final String s) throws URISyntaxException {
            return new URI(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link URL}.
     */
    @Plugin(name = "URL", category = CATEGORY)
    public static class UrlConverter implements TypeConverter<URL> {
        @Override
        public URL convert(final String s) throws MalformedURLException {
            return new URL(s);
        }
    }

    /**
     * Converts a {@link String} into a {@link UUID}.
     * @since 2.8
     */
    @Plugin(name = "UUID", category = CATEGORY)
    public static class UuidConverter implements TypeConverter<UUID> {
        @Override
        public UUID convert(final String s) throws Exception {
            return UUID.fromString(s);
        }
    }

    /**
     * Converts a String to a given class if a TypeConverter is available for that class. Falls back to the provided
     * default value if the conversion is unsuccessful. However, if the default value is <em>also</em> invalid, then
     * {@code null} is returned (along with a nasty status log message).
     *
     * @param s
     *        the string to convert
     * @param clazz
     *        the class to try to convert the string to
     * @param defaultValue
     *        the fallback object to use if the conversion is unsuccessful
     * @return the converted object which may be {@code null} if the string is invalid for the given type
     * @throws NullPointerException
     *         if {@code clazz} is {@code null}
     * @throws IllegalArgumentException
     *         if no TypeConverter exists for the given class
     */
    public static <T> T convert(final String s, final Class<? extends T> clazz, final Object defaultValue) {
        @SuppressWarnings("unchecked")
        final TypeConverter<T> converter =
                (TypeConverter<T>) TypeConverterRegistry.getInstance().findCompatibleConverter(clazz);
        if (s == null) {
            // don't debug print here, resulting output is hard to understand
            // LOGGER.debug("Null string given to convert. Using default [{}].", defaultValue);
            return parseDefaultValue(converter, defaultValue);
        }
        try {
            return converter.convert(s);
        } catch (final Exception e) {
            LOGGER.warn(
                    "Error while converting string [{}] to type [{}]. Using default value [{}].",
                    s,
                    clazz,
                    defaultValue,
                    e);
            return parseDefaultValue(converter, defaultValue);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T parseDefaultValue(final TypeConverter<T> converter, final Object defaultValue) {
        if (defaultValue == null) {
            return null;
        }
        if (!(defaultValue instanceof String)) {
            return (T) defaultValue;
        }
        try {
            return converter.convert((String) defaultValue);
        } catch (final Exception e) {
            LOGGER.debug("Can't parse default value [{}] for type [{}].", defaultValue, converter.getClass(), e);
            return null;
        }
    }

    private static final Logger LOGGER = StatusLogger.getLogger();
}
