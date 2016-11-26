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
package org.apache.logging.log4j.message;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.util.EnglishEnums;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;
import org.apache.logging.log4j.util.IndexedStringMap;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.Strings;

/**
 * Represents a Message that consists of a Map.
 * <p>
 * Thread-safety note: the contents of this message can be modified after construction.
 * When using asynchronous loggers and appenders it is not recommended to modify this message after the message is
 * logged, because it is undefined whether the logged message string will contain the old values or the modified
 * values.
 */
@PerformanceSensitive("allocation")
@AsynchronouslyFormattable
public class MapMessage implements MultiformatMessage, StringBuilderFormattable {

    /**
     * When set as the format specifier causes the Map to be formatted as XML.
     */

    public enum MapFormat {
        /** The map should be formatted as XML. */
        XML,
        /** The map should be formatted as JSON. */
        JSON,
        /** The map should be formatted the same as documented by java.util.AbstractMap.toString(). */
        JAVA;

        public static MapFormat lookupIgnoreCase(final String format) {
            return XML.name().equalsIgnoreCase(format) ? XML //
                    : JSON.name().equalsIgnoreCase(format) ? JSON //
                    : JAVA.name().equalsIgnoreCase(format) ? JAVA //
                    : null;
        }

        public static String[] names() {
            return new String[] {XML.name(), JSON.name(), JAVA.name()};
        }
    }

    private static final long serialVersionUID = -5031471831131487120L;

    private final IndexedStringMap data;

    /**
     * Constructor.
     */
    public MapMessage() {
        data = new SortedArrayStringMap();
    }

    /**
     * Constructor based on an existing Map.
     * @param map The Map.
     */
    public MapMessage(final Map<String, String> map) {
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
    public Map<String, String> getData() {
        final TreeMap<String, String> result = new TreeMap<>(); // returned map must be sorted
        for (int i = 0; i < data.size(); i++) {
            result.put(data.getKeyAt(i), (String) data.getValueAt(i));
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
     * Add an item to the data Map in fluent style.
     * @param key The name of the data item.
     * @param value The value of the data item.
     * @return {@code this}
     */
    public MapMessage with(final String key, final String value) {
        put(key, value);
        return this;
    }

    /**
     * Add an item to the data Map.
     * @param key The name of the data item.
     * @param value The value of the data item.
     */
    public void put(final String key, final String value) {
        if (value == null) {
            throw new IllegalArgumentException("No value provided for key " + key);
        }
        validate(key, value);
        data.putValue(key, value);
    }

    protected void validate(final String key, final String value) {

    }

    /**
     * Add all the elements from the specified Map.
     * @param map The Map to add.
     */
    public void putAll(final Map<String, String> map) {
        for (final Map.Entry<String, ?> entry : map.entrySet()) {
            data.putValue(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Retrieve the value of the element with the specified key or null if the key is not present.
     * @param key The name of the element.
     * @return The value of the element or null if the key is not present.
     */
    public String get(final String key) {
        return data.getValue(key);
    }

    /**
     * Remove the element with the specified name.
     * @param key The name of the element.
     * @return The previous value of the element.
     */
    public String remove(final String key) {
        final String result = data.getValue(key);
        data.remove(key);
        return result;
    }

    /**
     * Format the Structured data as described in RFC 5424.
     *
     * @return The formatted String.
     */
    public String asString() {
        return format((MapFormat) null, new StringBuilder()).toString();
    }

    public String asString(final String format) {
        try {
            return format(EnglishEnums.valueOf(MapFormat.class, format), new StringBuilder()).toString();
        } catch (final IllegalArgumentException ex) {
            return asString();
        }
    }
    /**
     * Format the Structured data as described in RFC 5424.
     *
     * @param format The format identifier. Ignored in this implementation.
     * @return The formatted String.
     */
    private StringBuilder format(final MapFormat format, final StringBuilder sb) {
        if (format == null) {
            appendMap(sb);
        } else {
            switch (format) {
                case XML : {
                    asXml(sb);
                    break;
                }
                case JSON : {
                    asJson(sb);
                    break;
                }
                case JAVA : {
                    asJava(sb);
                    break;
                }
                default : {
                    appendMap(sb);
                }
            }
        }
        return sb;
    }

    public void asXml(final StringBuilder sb) {
        sb.append("<Map>\n");
        for (int i = 0; i < data.size(); i++) {
            sb.append("  <Entry key=\"").append(data.getKeyAt(i)).append("\">").append(data.getValueAt(i))
                    .append("</Entry>\n");
        }
        sb.append("</Map>");
    }

    /**
     * Format the message and return it.
     * @return the formatted message.
     */
    @Override
    public String getFormattedMessage() {
        return asString();
    }

    /**
     *
     * @param formats An array of Strings that provide extra information about how to format the message.
     * MapMessage uses the first format specifier it recognizes. The supported formats are XML, JSON, and
     * JAVA. The default format is key1="value1" key2="value2" as required by RFC 5424 messages.
     *
     * @return The formatted message.
     */
    @Override
    public String getFormattedMessage(final String[] formats) {
        if (formats == null || formats.length == 0) {
            return asString();
        }
        for (int i = 0; i < formats.length; i++) {
            final MapFormat mapFormat = MapFormat.lookupIgnoreCase(formats[i]);
            if (mapFormat != null) {
                return format(mapFormat, new StringBuilder()).toString();
            }
        }
        return asString();

    }

    protected void appendMap(final StringBuilder sb) {
        for (int i = 0; i < data.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            StringBuilders.appendKeyDqValue(sb, data.getKeyAt(i), data.getValueAt(i));
        }
    }

    protected void asJson(final StringBuilder sb) {
        sb.append('{');
        for (int i = 0; i < data.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            StringBuilders.appendDqValue(sb, data.getKeyAt(i)).append(':');
            StringBuilders.appendDqValue(sb, data.getValueAt(i));
        }
        sb.append('}');
    }


    protected void asJava(final StringBuilder sb) {
        sb.append('{');
        for (int i = 0; i < data.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            StringBuilders.appendKeyDqValue(sb, data.getKeyAt(i), data.getValueAt(i));
        }
        sb.append('}');
    }

    public MapMessage newInstance(final Map<String, String> map) {
        return new MapMessage(map);
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
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        final MapMessage that = (MapMessage) o;

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
}
