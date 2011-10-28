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
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Represents a Message that conforms to RFC 5424 (http://tools.ietf.org/html/rfc5424).
 */
public class MapMessage implements FormattedMessage, Serializable {

    public static final String XML = "XML";
    private static final int HASHVAL = 31;
    private static final long serialVersionUID = -5031471831131487120L;

    private final Map<String, String> data;

    private String format = null;

    /**
     * Constructor.
     */
    public MapMessage() {
        data = new HashMap<String, String>();
    }

    /**
     * Constructor based on an existing Map.
     * @param map The Map.
     */
    public MapMessage(Map<String, String> map) {
        this.data = map;
    }

    /**
     * The format String. Specifying "xml" will cause the message to be XML.
     * @param format The message format.
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Return the format String.
     * @return the format String.
     */
    public String getFormat() {
        return this.format;
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
    public String getMessageFormat() {
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
        return asString("");
    }

    /**
     * Format the Structured data as described in RFC 5424.
     *
     * @param format The format identifier. Ignored in this implementation.
     * @return The formatted String.
     */
    public String asString(String format) {
        StringBuilder sb = new StringBuilder();
        if (format.equalsIgnoreCase(XML)) {
            asXML(sb);
        } else {
            appendMap(sb);
        }
        return sb.toString();
    }

    public void asXML(StringBuilder sb) {

    }

    /**
     * Format the message and return it.
     * @return the formatted message.
     */
    public String getFormattedMessage() {
        return asString();
    }

    protected void appendMap(StringBuilder sb) {
        SortedMap<String, String> sorted = new TreeMap<String, String>(data);
        boolean first = true;
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (!first) {
                sb.append(" ");
            }
            first = false;
            sb.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
        }
    }

    public String toString() {
        return asString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MapMessage that = (MapMessage) o;

        return this.data.equals(that.data);
    }

    public int hashCode() {
        return data.hashCode();
    }
}
