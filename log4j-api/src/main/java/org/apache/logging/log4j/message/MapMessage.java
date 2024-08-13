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
package org.apache.logging.log4j.message;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.Chars;
import org.apache.logging.log4j.util.EnglishEnums;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;
import org.apache.logging.log4j.util.IndexedStringMap;
import org.apache.logging.log4j.util.MultiFormatStringBuilderFormattable;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.TriConsumer;

/**
 * Represents a Message that consists of a Map.
 * <p>
 * Thread-safety note: the contents of this message can be modified after construction.
 * When using asynchronous loggers and appenders it is not recommended to modify this message after the message is
 * logged, because it is undefined whether the logged message string will contain the old values or the modified
 * values.
 * </p>
 * <p>
 * This class was pulled up from {@link StringMapMessage} to allow for Objects as values.
 * </p>
 * @param <M> Allow subclasses to use fluent APIs and override methods that return instances of subclasses.
 * @param <V> The value type
 */
@PerformanceSensitive("allocation")
@AsynchronouslyFormattable
public class MapMessage<M extends MapMessage<M, V>, V> implements MultiFormatStringBuilderFormattable {

    private static final long serialVersionUID = -5031471831131487120L;

    /**
     * When set as the format specifier causes the Map to be formatted as XML.
     */
    public enum MapFormat {

        /** The map should be formatted as XML. */
        XML,

        /** The map should be formatted as JSON. */
        JSON,

        /** The map should be formatted the same as documented by {@link AbstractMap#toString()}. */
        JAVA,

        /**
         * The map should be formatted the same as documented by {@link AbstractMap#toString()} but without quotes.
         *
         * @since 2.11.2
         */
        JAVA_UNQUOTED;

        /**
         * Maps a format name to an {@link MapFormat} while ignoring case.
         *
         * @param format a MapFormat name
         * @return a MapFormat
         */
        public static MapFormat lookupIgnoreCase(final String format) {
            return XML.name().equalsIgnoreCase(format)
                    ? XML //
                    : JSON.name().equalsIgnoreCase(format)
                            ? JSON //
                            : JAVA.name().equalsIgnoreCase(format)
                                    ? JAVA //
                                    : JAVA_UNQUOTED.name().equalsIgnoreCase(format)
                                            ? JAVA_UNQUOTED //
                                            : null;
        }

        /**
         * All {@code MapFormat} names.
         *
         * @return All {@code MapFormat} names.
         */
        public static String[] names() {
            return new String[] {XML.name(), JSON.name(), JAVA.name(), JAVA_UNQUOTED.name()};
        }
    }

    private final IndexedStringMap data;

    /**
     * Constructs a new instance.
     */
    public MapMessage() {
        this.data = new SortedArrayStringMap();
    }

    /**
     * Constructs a new instance.
     *
     * @param  initialCapacity the initial capacity.
     */
    public MapMessage(final int initialCapacity) {
        this.data = new SortedArrayStringMap(initialCapacity);
    }

    /**
     * Constructs a new instance based on an existing {@link Map}.
     * @param map The Map.
     */
    public MapMessage(final Map<String, V> map) {
        this.data = new SortedArrayStringMap(map);
    }

    @Override
    public String[] getFormats() {
        return MapFormat.names();
    }

    /**
     * Returns the data elements as if they were parameters on the logging event.
     * @return the data elements.
     */
    @Override
    public Object[] getParameters() {
        final Object[] result = new Object[data.size()];
        for (int i = 0; i < data.size(); i++) {
            result[i] = data.getValueAt(i);
        }
        return result;
    }

    /**
     * Returns the message.
     * @return the message.
     */
    @Override
    public String getFormat() {
        return Strings.EMPTY;
    }

    /**
     * Returns the message data as an unmodifiable Map.
     * @return the message data as an unmodifiable map.
     */
    @SuppressWarnings("unchecked")
    public Map<String, V> getData() {
        final TreeMap<String, V> result = new TreeMap<>(); // returned map must be sorted
        for (int i = 0; i < data.size(); i++) {
            // The Eclipse compiler does not need the typecast to V, but the Oracle compiler sure does.
            result.put(data.getKeyAt(i), (V) data.getValueAt(i));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns a read-only view of the message data.
     * @return the read-only message data.
     */
    public IndexedReadOnlyStringMap getIndexedReadOnlyStringMap() {
        return data;
    }

    /**
     * Clear the data.
     */
    public void clear() {
        data.clear();
    }

    /**
     * Returns {@code true} if this data structure contains the specified key, {@code false} otherwise.
     *
     * @param key the key whose presence to check. May be {@code null}.
     * @return {@code true} if this data structure contains the specified key, {@code false} otherwise
     * @since 2.9
     */
    public boolean containsKey(final String key) {
        return data.containsKey(key);
    }

    /**
     * Adds an item to the data Map.
     * @param candidateKey The name of the data item.
     * @param value The value of the data item.
     */
    public void put(final String candidateKey, final String value) {
        if (value == null) {
            throw new IllegalArgumentException("No value provided for key " + candidateKey);
        }
        final String key = toKey(candidateKey);
        validate(key, value);
        data.putValue(key, value);
    }

    /**
     * Adds all the elements from the specified Map.
     * @param map The Map to add.
     */
    public void putAll(final Map<String, String> map) {
        for (final Map.Entry<String, String> entry : map.entrySet()) {
            data.putValue(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Retrieves the value of the element with the specified key or null if the key is not present.
     * @param key The name of the element.
     * @return The value of the element or null if the key is not present.
     */
    public String get(final String key) {
        final Object result = data.getValue(key);
        return ParameterFormatter.deepToString(result);
    }

    /**
     * Removes the element with the specified name.
     * @param key The name of the element.
     * @return The previous value of the element.
     */
    public String remove(final String key) {
        final String result = get(key);
        data.remove(key);
        return result;
    }

    /**
     * Formats the Structured data as described in <a href="https://datatracker.ietf.org/doc/html/rfc5424">RFC 5424</a>.
     *
     * @return The formatted String.
     */
    public String asString() {
        return format((MapFormat) null, new StringBuilder()).toString();
    }

    /**
     * Formats the Structured data as described in <a href="https://datatracker.ietf.org/doc/html/rfc5424">RFC 5424</a>.
     *
     * @param format The format identifier.
     * @return The formatted String.
     */
    public String asString(final String format) {
        try {
            return format(EnglishEnums.valueOf(MapFormat.class, format), new StringBuilder())
                    .toString();
        } catch (final IllegalArgumentException ex) {
            return asString();
        }
    }

    /**
     * Performs the given action for each key-value pair in this data structure
     * until all entries have been processed or the action throws an exception.
     * <p>
     * Some implementations may not support structural modifications (adding new elements or removing elements) while
     * iterating over the contents. In such implementations, attempts to add or remove elements from the
     * {@code BiConsumer}'s {@link BiConsumer#accept(Object, Object)} accept} method may cause a
     * {@code ConcurrentModificationException} to be thrown.
     * </p>
     *
     * @param action The action to be performed for each key-value pair in this collection
     * @param <CV> type of the consumer value
     * @throws java.util.ConcurrentModificationException some implementations may not support structural modifications
     *          to this data structure while iterating over the contents with {@link #forEach(BiConsumer)} or
     *          {@link #forEach(TriConsumer, Object)}.
     * @see ReadOnlyStringMap#forEach(BiConsumer)
     * @since 2.9
     */
    public <CV> void forEach(final BiConsumer<String, ? super CV> action) {
        data.forEach(action);
    }

    /**
     * Performs the given action for each key-value pair in this data structure
     * until all entries have been processed or the action throws an exception.
     * <p>
     * The third parameter lets callers pass in a stateful object to be modified with the key-value pairs,
     * so the TriConsumer implementation itself can be stateless and potentially reusable.
     * </p>
     * <p>
     * Some implementations may not support structural modifications (adding new elements or removing elements) while
     * iterating over the contents. In such implementations, attempts to add or remove elements from the
     * {@code TriConsumer}'s {@link TriConsumer#accept(Object, Object, Object) accept} method may cause a
     * {@code ConcurrentModificationException} to be thrown.
     * </p>
     *
     * @param action The action to be performed for each key-value pair in this collection
     * @param state the object to be passed as the third parameter to each invocation on the specified
     *          triconsumer
     * @param <CV> type of the consumer value
     * @param <S> type of the third parameter
     * @throws java.util.ConcurrentModificationException some implementations may not support structural modifications
     *          to this data structure while iterating over the contents with {@link #forEach(BiConsumer)} or
     *          {@link #forEach(TriConsumer, Object)}.
     * @see ReadOnlyStringMap#forEach(TriConsumer, Object)
     * @since 2.9
     */
    public <CV, S> void forEach(final TriConsumer<String, ? super CV, S> action, final S state) {
        data.forEach(action, state);
    }

    /**
     * Formats the Structured data as described in <a href="https://datatracker.ietf.org/doc/html/rfc5424">RFC 5424</a>.
     *
     * @param format The format identifier.
     * @return The formatted String.
     */
    private StringBuilder format(final MapFormat format, final StringBuilder sb) {
        if (format == null) {
            appendMap(sb);
        } else {
            switch (format) {
                case XML: {
                    asXml(sb);
                    break;
                }
                case JSON: {
                    asJson(sb);
                    break;
                }
                case JAVA: {
                    asJava(sb);
                    break;
                }
                case JAVA_UNQUOTED:
                    asJavaUnquoted(sb);
                    break;
                default: {
                    appendMap(sb);
                }
            }
        }
        return sb;
    }

    /**
     * Formats this message as an XML fragment String into the given builder.
     *
     * @param sb format into this builder.
     */
    public void asXml(final StringBuilder sb) {
        sb.append("<Map>\n");
        for (int i = 0; i < data.size(); i++) {
            sb.append("  <Entry key=\"").append(data.getKeyAt(i)).append("\">");
            final int size = sb.length();
            ParameterFormatter.recursiveDeepToString(data.getValueAt(i), sb);
            StringBuilders.escapeXml(sb, size);
            sb.append("</Entry>\n");
        }
        sb.append("</Map>");
    }

    /**
     * Formats the message and return it.
     * @return the formatted message.
     */
    @Override
    public String getFormattedMessage() {
        return asString();
    }

    /**
     *
     * @param formats
     *            An array of Strings that provide extra information about how to format the message. MapMessage uses
     *            the first format specifier it recognizes. The supported formats are XML, JSON, and JAVA. The default
     *            format is key1="value1" key2="value2" as required by
     *            <a href="https://datatracker.ietf.org/doc/html/rfc5424">RFC 5424</a>
     *            messages.
     *
     * @return The formatted message.
     */
    @Override
    public String getFormattedMessage(final String[] formats) {
        return format(getFormat(formats), new StringBuilder()).toString();
    }

    private MapFormat getFormat(final String[] formats) {
        if (formats == null || formats.length == 0) {
            return null;
        }
        for (int i = 0; i < formats.length; i++) {
            final MapFormat mapFormat = MapFormat.lookupIgnoreCase(formats[i]);
            if (mapFormat != null) {
                return mapFormat;
            }
        }
        return null;
    }

    protected void appendMap(final StringBuilder sb) {
        for (int i = 0; i < data.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(data.getKeyAt(i)).append(Chars.EQ).append(Chars.DQUOTE);
            ParameterFormatter.recursiveDeepToString(data.getValueAt(i), sb);
            sb.append(Chars.DQUOTE);
        }
    }

    protected void asJson(final StringBuilder sb) {
        MapMessageJsonFormatter.format(sb, data);
    }

    protected void asJavaUnquoted(final StringBuilder sb) {
        asJava(sb, false);
    }

    protected void asJava(final StringBuilder sb) {
        asJava(sb, true);
    }

    private void asJava(final StringBuilder sb, final boolean quoted) {
        sb.append('{');
        for (int i = 0; i < data.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(data.getKeyAt(i)).append(Chars.EQ);
            if (quoted) {
                sb.append(Chars.DQUOTE);
            }
            ParameterFormatter.recursiveDeepToString(data.getValueAt(i), sb);
            if (quoted) {
                sb.append(Chars.DQUOTE);
            }
        }
        sb.append('}');
    }

    /**
     * Constructs a new instance based on an existing Map.
     * @param map The Map.
     * @return A new MapMessage
     */
    @SuppressWarnings("unchecked")
    public M newInstance(final Map<String, V> map) {
        return (M) new MapMessage<>(map);
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        format((MapFormat) null, buffer);
    }

    @Override
    public void formatTo(final String[] formats, final StringBuilder buffer) {
        format(getFormat(formats), buffer);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MapMessage)) {
            return false;
        }

        final MapMessage<?, ?> that = (MapMessage<?, ?>) o;

        return this.data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    /**
     * Always returns null.
     *
     * @return null
     */
    @Override
    public Throwable getThrowable() {
        return null;
    }

    /**
     * Default implementation does nothing.
     *
     * @since 2.9
     */
    protected void validate(final String key, final boolean value) {
        // do nothing
    }

    /**
     * Default implementation does nothing.
     *
     * @since 2.9
     */
    protected void validate(final String key, final byte value) {
        // do nothing
    }

    /**
     * Default implementation does nothing.
     *
     * @since 2.9
     */
    protected void validate(final String key, final char value) {
        // do nothing
    }

    /**
     * Default implementation does nothing.
     *
     * @since 2.9
     */
    protected void validate(final String key, final double value) {
        // do nothing
    }

    /**
     * Default implementation does nothing.
     *
     * @since 2.9
     */
    protected void validate(final String key, final float value) {
        // do nothing
    }

    /**
     * Default implementation does nothing.
     *
     * @since 2.9
     */
    protected void validate(final String key, final int value) {
        // do nothing
    }

    /**
     * Default implementation does nothing.
     *
     * @since 2.9
     */
    protected void validate(final String key, final long value) {
        // do nothing
    }

    /**
     * Default implementation does nothing.
     *
     * @since 2.9
     */
    protected void validate(final String key, final Object value) {
        // do nothing
    }

    /**
     * Default implementation does nothing.
     *
     * @since 2.9
     */
    protected void validate(final String key, final short value) {
        // do nothing
    }

    /**
     * Default implementation does nothing.
     *
     * @since 2.9
     */
    protected void validate(final String key, final String value) {
        // do nothing
    }

    /**
     * Allows subclasses to change a candidate key to an actual key.
     *
     * @param candidateKey The candidate key.
     * @return The candidate key.
     * @since 2.12
     */
    protected String toKey(final String candidateKey) {
        return candidateKey;
    }

    /**
     * Adds an item to the data Map.
     * @param candidateKey The name of the data item.
     * @param value The value of the data item.
     * @return this object
     * @since 2.9
     */
    @SuppressWarnings("unchecked")
    public M with(final String candidateKey, final boolean value) {
        final String key = toKey(candidateKey);
        validate(key, value);
        data.putValue(key, value);
        return (M) this;
    }

    /**
     * Adds an item to the data Map.
     * @param candidateKey The name of the data item.
     * @param value The value of the data item.
     * @return this object
     * @since 2.9
     */
    @SuppressWarnings("unchecked")
    public M with(final String candidateKey, final byte value) {
        final String key = toKey(candidateKey);
        validate(key, value);
        data.putValue(key, value);
        return (M) this;
    }

    /**
     * Adds an item to the data Map.
     * @param candidateKey The name of the data item.
     * @param value The value of the data item.
     * @return this object
     * @since 2.9
     */
    @SuppressWarnings("unchecked")
    public M with(final String candidateKey, final char value) {
        final String key = toKey(candidateKey);
        validate(key, value);
        data.putValue(key, value);
        return (M) this;
    }

    /**
     * Adds an item to the data Map.
     * @param candidateKey The name of the data item.
     * @param value The value of the data item.
     * @return this object
     * @since 2.9
     */
    @SuppressWarnings("unchecked")
    public M with(final String candidateKey, final double value) {
        final String key = toKey(candidateKey);
        validate(key, value);
        data.putValue(key, value);
        return (M) this;
    }

    /**
     * Adds an item to the data Map.
     * @param candidateKey The name of the data item.
     * @param value The value of the data item.
     * @return this object
     * @since 2.9
     */
    @SuppressWarnings("unchecked")
    public M with(final String candidateKey, final float value) {
        final String key = toKey(candidateKey);
        validate(key, value);
        data.putValue(key, value);
        return (M) this;
    }

    /**
     * Adds an item to the data Map.
     * @param candidateKey The name of the data item.
     * @param value The value of the data item.
     * @return this object
     * @since 2.9
     */
    @SuppressWarnings("unchecked")
    public M with(final String candidateKey, final int value) {
        final String key = toKey(candidateKey);
        validate(key, value);
        data.putValue(key, value);
        return (M) this;
    }

    /**
     * Adds an item to the data Map.
     * @param candidateKey The name of the data item.
     * @param value The value of the data item.
     * @return this object
     * @since 2.9
     */
    @SuppressWarnings("unchecked")
    public M with(final String candidateKey, final long value) {
        final String key = toKey(candidateKey);
        validate(key, value);
        data.putValue(key, value);
        return (M) this;
    }

    /**
     * Adds an item to the data Map.
     * @param candidateKey The name of the data item.
     * @param value The value of the data item.
     * @return this object
     * @since 2.9
     */
    @SuppressWarnings("unchecked")
    public M with(final String candidateKey, final Object value) {
        final String key = toKey(candidateKey);
        validate(key, value);
        data.putValue(key, value);
        return (M) this;
    }

    /**
     * Adds an item to the data Map.
     * @param candidateKey The name of the data item.
     * @param value The value of the data item.
     * @return this object
     * @since 2.9
     */
    @SuppressWarnings("unchecked")
    public M with(final String candidateKey, final short value) {
        final String key = toKey(candidateKey);
        validate(key, value);
        data.putValue(key, value);
        return (M) this;
    }

    /**
     * Adds an item to the data Map in fluent style.
     * @param candidateKey The name of the data item.
     * @param value The value of the data item.
     * @return {@code this}
     */
    @SuppressWarnings("unchecked")
    public M with(final String candidateKey, final String value) {
        final String key = toKey(candidateKey);
        put(key, value);
        return (M) this;
    }
}
