/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.Provider;
import java.security.Security;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.GelfLayout;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.util.Assert;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.EnglishEnums;

/**
 * Collection of basic TypeConverter implementations. May be used to register additional TypeConverters or find
 * registered TypeConverters.
 */
public final class TypeConverters {

    /**
     * The {@link Plugin} category to use for {@link TypeConverter} plugins.
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
     * <li>String</li>
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
                bytes = new byte[0];
            } else if (value.startsWith(PREFIX_BASE64)) {
                final String lexicalXSDBase64Binary = value.substring(PREFIX_BASE64.length());
                bytes = DatatypeConverter.parseBase64Binary(lexicalXSDBase64Binary);
            } else if (value.startsWith(PREFIX_0x)) {
                final String lexicalXSDHexBinary = value.substring(PREFIX_0x.length());
                bytes = DatatypeConverter.parseHexBinary(lexicalXSDHexBinary);
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
            return Loader.loadClass(s);
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
     * Converts a {@link String} into a {@link Enum}. Returns {@code null} for invalid enum names.
     * 
     * @param <E>
     *        the enum class to parse.
     */
    public static class EnumConverter<E extends Enum<E>> implements TypeConverter<E> {
        private final Class<E> clazz;

        private EnumConverter(final Class<E> clazz) {
            this.clazz = clazz;
        }

        @Override
        public E convert(final String s) {
            return EnglishEnums.valueOf(clazz, s);
        }
    }

    /**
     * Converts a {@link String} into a {@link File}.
     */
    @Plugin(name = "File", category = CATEGORY)
    public static class FileConverter implements TypeConverter<File> {
        @Override
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

    private static final class Holder {
        private static final TypeConverters INSTANCE = new TypeConverters();
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
    public static Object convert(final String s, final Class<?> clazz, final Object defaultValue) {
        final TypeConverter<?> converter = findTypeConverter(Assert.requireNonNull(clazz,
                "No class specified to convert to."));
        if (converter == null) {
            throw new IllegalArgumentException("No type converter found for class: " + clazz.getName());
        }
        if (s == null) {
            // don't debug print here, resulting output is hard to understand
            // LOGGER.debug("Null string given to convert. Using default [{}].", defaultValue);
            return parseDefaultValue(converter, defaultValue);
        }
        try {
            return converter.convert(s);
        } catch (final Exception e) {
            LOGGER.warn("Error while converting string [{}] to type [{}]. Using default value [{}].", s, clazz,
                    defaultValue, e);
            return parseDefaultValue(converter, defaultValue);
        }
    }

    /**
     * Locates a TypeConverter for a specified class.
     * 
     * @param clazz
     *        the class to get a TypeConverter for
     * @return the associated TypeConverter for that class or {@code null} if none could be found
     */
    public static TypeConverter<?> findTypeConverter(final Class<?> clazz) {
        // TODO: what to do if there's no converter?
        // supplementary idea: automatically add type converters for enums using EnglishEnums
        // Idea 1: use reflection to see if the class has a static "valueOf" method and use that
        // Idea 2: reflect on class's declared methods to see if any methods look suitable (probably too complex)
        return Holder.INSTANCE.registry.get(clazz);
    }

    private static Object parseDefaultValue(final TypeConverter<?> converter, final Object defaultValue) {
        if (defaultValue == null) {
            return null;
        }
        if (!(defaultValue instanceof String)) {
            return defaultValue;
        }
        try {
            return converter.convert((String) defaultValue);
        } catch (final Exception e) {
            LOGGER.debug("Can't parse default value [{}] for type [{}].", defaultValue, converter.getClass(), e);
            return null;
        }
    }

    /**
     * Registers a TypeConverter for a specified class. This will overwrite any existing TypeConverter that may be
     * registered for the class.
     * 
     * @param clazz
     *        the class to register the TypeConverter for
     * @param converter
     *        the TypeConverter to register
     */
    public static void registerTypeConverter(final Class<?> clazz, final TypeConverter<?> converter) {
        Holder.INSTANCE.registry.put(clazz, converter);
    }

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Map<Class<?>, TypeConverter<?>> registry = new ConcurrentHashMap<Class<?>, TypeConverter<?>>();

    /**
     * Constructs default TypeConverter registry. Used solely by singleton instance.
     */
    private TypeConverters() {
        // Primitive wrappers
        registry.put(Boolean.class, new BooleanConverter());
        registry.put(Byte.class, new ByteConverter());
        registry.put(Character.class, new CharacterConverter());
        registry.put(Double.class, new DoubleConverter());
        registry.put(Float.class, new FloatConverter());
        registry.put(Integer.class, new IntegerConverter());
        registry.put(Long.class, new LongConverter());
        registry.put(Short.class, new ShortConverter());
        // Primitives
        registry.put(boolean.class, registry.get(Boolean.class));
        registry.put(byte.class, new ByteConverter());
        registry.put(char[].class, new CharArrayConverter());
        registry.put(double.class, registry.get(Double.class));
        registry.put(float.class, registry.get(Float.class));
        registry.put(int.class, registry.get(Integer.class));
        registry.put(long.class, registry.get(Long.class));
        registry.put(short.class, registry.get(Short.class));
        // Primitive arrays
        registry.put(byte[].class, new ByteArrayConverter());
        registry.put(char.class, new CharacterConverter());
        // Numbers
        registry.put(BigInteger.class, new BigIntegerConverter());
        registry.put(BigDecimal.class, new BigDecimalConverter());
        // JRE
        registry.put(String.class, new StringConverter());
        registry.put(Charset.class, new CharsetConverter());
        registry.put(File.class, new FileConverter());
        registry.put(URL.class, new UrlConverter());
        registry.put(URI.class, new UriConverter());
        registry.put(Class.class, new ClassConverter());
        registry.put(Pattern.class, new PatternConverter());
        registry.put(Provider.class, new SecurityProviderConverter());
        // Log4J
        registry.put(Level.class, new LevelConverter());
        registry.put(Filter.Result.class, new EnumConverter<Filter.Result>(Filter.Result.class));
        registry.put(Facility.class, new EnumConverter<Facility>(Facility.class));
        registry.put(Protocol.class, new EnumConverter<Protocol>(Protocol.class));
        registry.put(GelfLayout.CompressionType.class, new EnumConverter<GelfLayout.CompressionType>(
                GelfLayout.CompressionType.class));
        registry.put(HtmlLayout.FontSize.class, new EnumConverter<HtmlLayout.FontSize>(HtmlLayout.FontSize.class));
        registry.put(ConsoleAppender.Target.class, new EnumConverter<ConsoleAppender.Target>(
            ConsoleAppender.Target.class));
    }
}
