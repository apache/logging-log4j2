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
public class StructuredDataMessage implements FormattedMessage, Serializable {
    /**
     * Full message format includes the type and message.
     */
    public static final String FULL = "full";

    private static final long serialVersionUID = 1703221292892071920L;
    private static final int MAX_LENGTH = 32;
    private static final int HASHVAL = 31;

    private Map<String, String> data = new HashMap<String, String>();

    private StructuredDataId id;

    private String message;

    private String type;

    private String format = null;

    /**
     * Constructor based on a String id.
     * @param id The String id.
     * @param msg The message.
     * @param type The message type.
     */
    public StructuredDataMessage(final String id, final String msg, final String type) {
        this.id = new StructuredDataId(id, null, null);
        this.message = msg;
        this.type = type;
    }

    /**
     * Constructor based on a StructuredDataId.
     * @param id The StructuredDataId.
     * @param msg The message.
     * @param type The message type.
     */
    public StructuredDataMessage(final StructuredDataId id, final String msg, final String type) {
        this.id = id;
        this.message = msg;
        this.type = type;
    }

    /**
     * Basic constructor.
     */
    protected StructuredDataMessage() {

    }

    /**
     * The format String. Specifying "full" will cause the type and message to be included.
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
     * Return the id.
     * @return the StructuredDataId.
     */
    public StructuredDataId getId() {
        return id;
    }

    /**
     * Set the id from a String.
     * @param id The String id.
     */
    protected void setId(String id) {
        this.id = new StructuredDataId(id, null, null);
    }

    /**
     * Set the id.
     * @param id The StructuredDataId.
     */
    protected void setId(StructuredDataId id) {
        this.id = id;
    }

    /**
     * Set the type.
     * @return the type.
     */
    public String getType() {
        return type;
    }

    protected void setType(String type) {
        if (type.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Structured data type exceeds maximum length of 32 characters: " + type);
        }
        this.type = type;
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
        return message;
    }

    protected void setMessageFormat(String msg) {
        this.message = msg;
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
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Structured data values are limited to 32 characters. key: " + key +
                " value: " + value);
        }
        data.put(key, value);
    }

    /**
     * Add all the elements from the specified Map.
     * @param map The Map to add.
     */
    public void putAll(Map map) {
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
    public final String asString() {
        return asString(FULL, null);
    }

    /**
     * Format the Structured data as described in RFC 5424.
     *
     * @param format The format identifier. Ignored in this implementation.
     * @return The formatted String.
     */
    public String asString(String format) {
        return asString(format, null);
    }

    /**
     * Format the Structured data as described in RFC 5424.
     *
     * @param format           "full" will include the type and message. null will return only the STRUCTURED-DATA as
     *                         described in RFC 5424
     * @param structuredDataId The SD-ID as described in RFC 5424. If null the value in the StructuredData
     *                         will be used.
     * @return The formatted String.
     */
    public final String asString(String format, StructuredDataId structuredDataId) {
        StringBuffer sb = new StringBuffer();
        boolean full = FULL.equals(format);
        if (full) {
            String type = getType();
            if (type == null) {
                return sb.toString();
            }
            sb.append(getType()).append(" ");
        }
        StructuredDataId id = getId();
        if (id != null) {
            id = id.makeId(structuredDataId);
        } else {
            id = structuredDataId;
        }
        if (id == null || id.getName() == null) {
            return sb.toString();
        }
        sb.append("[");
        sb.append(id);
        appendMap(getData(), sb);
        sb.append("]");
        if (full) {
            String msg = getMessageFormat();
            if (msg != null) {
                sb.append(" ").append(msg);
            }
        }
        return sb.toString();
    }

    /**
     * Format the message and return it.
     * @return the formatted message.
     */
    public String getFormattedMessage() {
        return asString(FULL, null);
    }

    private void appendMap(Map map, StringBuffer sb) {
        SortedMap<String, Object> sorted = new TreeMap<String, Object>(map);
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            sb.append(" ");
            sb.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
        }
    }

    public String toString() {
        return asString((String) null);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StructuredDataMessage that = (StructuredDataMessage) o;

        if (data != null ? !data.equals(that.data) : that.data != null) {
            return false;
        }
        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (message != null ? !message.equals(that.message) : that.message != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = data != null ? data.hashCode() : 0;
        result = HASHVAL * result + (type != null ? type.hashCode() : 0);
        result = HASHVAL * result + (id != null ? id.hashCode() : 0);
        result = HASHVAL * result + (message != null ? message.hashCode() : 0);
        return result;
    }
}
