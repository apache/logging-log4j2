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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Represents a Message that consists of a Map.
 */
public class MapMessage implements MultiformatMessage, Serializable {
    /**
     * When set as the format specifier causes the Map to be formatted as XML.
     */

    public enum MapFormat {
        /** The map should be formatted as XML. */
        XML,
        /** The map should be formatted as JSON. */
        JSON,
        /** The map should be formatted the same as documented by java.util.AbstractMap.toString(). */
        JAVA
    }

    private static final long serialVersionUID = -5031471831131487120L;

    private final SortedMap<String, String> data;

    /**
     * Constructor.
     */
    public MapMessage() {
        data = new TreeMap<String, String>();
    }

    /**
     * Constructor based on an existing Map.
     * @param map The Map.
     */
    public MapMessage(Map<String, String> map) {
        this.data = map instanceof SortedMap ? (SortedMap<String, String>) map : new TreeMap<String, String>(map);
    }

    public String[] getFormats() {
        String[] formats = new String[MapFormat.values().length];
        int i = 0;
        for (MapFormat format : MapFormat.values()) {
            formats[i++] = format.name();
        }
        return formats;
    }

    /**
     * Return the data elements as if they were parameters on the logging event.
     * @return the data elements.
     */
    public Object[] getParameters() {
        return data.values().toArray();
    }

    /**
     * Return the message.
     * @return the message.
     */
    public String getFormat() {
        return "";
    }

    /**
     * Return the message data as an unmodifiable Map.
     * @return the message data as an unmodifiable map.
     */
    public Map<String, String> getData() {
        return Collections.unmodifiableMap(data);
    }

    /**
     * Clear the data.
     */
    public void clear() {
        data.clear();
    }

    /**
     * Add an item to the data Map.
     * @param key The name of the data item.
     * @param value The value of the data item.
     */
    public void put(String key, String value) {
        if (value == null) {
            throw new IllegalArgumentException("No value provided for key " + key);
        }
        validate(key, value);
        data.put(key, value);
    }

    protected void validate(String key, String value) {

    }

    /**
     * Add all the elements from the specified Map.
     * @param map The Map to add.
     */
    public void putAll(Map<String, String> map) {
        data.putAll(map);
    }

    /**
     * Retrieve the value of the element with the specified key or null if the key is not present.
     * @param key The name of the element.
     * @return The value of the element or null if the key is not present.
     */
    public String get(String key) {
        return data.get(key);
    }

    /**
     * Remove the element with the specified name.
     * @param key The name of the element.
     * @return The previous value of the element.
     */
    public String remove(String key) {
        return data.remove(key);
    }

    /**
     * Format the Structured data as described in RFC 5424.
     *
     * @return The formatted String.
     */
    public String asString() {
        return asString((MapFormat) null);
    }

    public String asString(String format) {
        try {
            return asString(MapFormat.valueOf(format.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return asString();
        }
    }
    /**
     * Format the Structured data as described in RFC 5424.
     *
     * @param format The format identifier. Ignored in this implementation.
     * @return The formatted String.
     */
    private String asString(MapFormat format) {
        StringBuilder sb = new StringBuilder();
        if (format == null) {
            appendMap(sb);
        } else {
            switch (format) {
                case XML : {
                    asXML(sb);
                    break;
                }
                case JSON : {
                    asJSON(sb);
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
        return sb.toString();
    }

    public void asXML(StringBuilder sb) {
        sb.append("<Map>\n");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            sb.append("  <Entry key=").append(entry.getKey()).append(">").append(entry.getValue()).append("</Entry>\n");
        }
        sb.append("</Map>");
    }

    /**
     * Format the message and return it.
     * @return the formatted message.
     */
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
    public String getFormattedMessage(String[] formats) {
        if (formats == null || formats.length == 0) {
            return asString();
        } else {
            for (String format : formats) {
                for (MapFormat f : MapFormat.values()) {
                    if (f.name().equalsIgnoreCase(format)) {
                        return asString(f);
                    }
                }
            }
            return asString();
        }

    }

    protected void appendMap(StringBuilder sb) {
        boolean first = true;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!first) {
                sb.append(" ");
            }
            first = false;
            sb.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
        }
    }

    protected void asJSON(StringBuilder sb) {
        boolean first = true;
        sb.append("{");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append("\"").append(entry.getValue()).append("\"");
        }
        sb.append("}");
    }


    protected void asJava(StringBuilder sb) {
        boolean first = true;
        sb.append("{");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
        }
        sb.append("}");
    }

    public MapMessage newInstance(Map<String, String> map) {
        return new MapMessage(map);
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        MapMessage that = (MapMessage) o;

        return this.data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }
}
