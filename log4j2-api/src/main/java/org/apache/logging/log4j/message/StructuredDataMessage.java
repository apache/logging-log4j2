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
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * Represents a Message that conforms to RFC 5424 (http://tools.ietf.org/html/rfc5424).
 */
public class StructuredDataMessage implements FormattedMessage, Serializable {
    private static final long serialVersionUID = 1703221292892071920L;

    public static final String FULL = "full";

    private Map<String, String> data = new HashMap<String, String>();

    private StructuredDataId id;

    private String message;

    private String type;

    private String format = null;

    public StructuredDataMessage(final String id, final String msg, final String type) {
        this.id = new StructuredDataId(id, null, null);
        this.message = msg;
        this.type = type;
    }

    public StructuredDataMessage(final StructuredDataId id, final String msg, final String type) {
        this.id = id;
        this.message = msg;
        this.type = type;
    }

    protected StructuredDataMessage() {

    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return this.format;
    }

    public StructuredDataId getId() {
        return id;
    }

    protected void setId(String id) {
        this.id = new StructuredDataId(id, null, null);
    }

    protected void setId(StructuredDataId id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    protected void setType(String type) {
        if (type.length() > 32) {
            throw new IllegalArgumentException("Structured data type exceeds maximum length of 32 characters: " + type);
        }
        this.type = type;
    }

    public Object[] getParameters() {
        return data.values().toArray();
    }

    public String getMessageFormat() {
        return message;
    }

    protected void setMessageFormat(String msg) {
        this.message = msg;
    }

    public Map<String, String> getData() {
        return Collections.unmodifiableMap(data);
    }

    public void clear() {
        data.clear();
    }

    public void put(String key, String value) {
        if (value == null) {
            throw new IllegalArgumentException("No value provided for key " + key);
        }
        if (value.length() > 32) {
            throw new IllegalArgumentException("Structured data values are limited to 32 characters. key: " + key +
                " value: " + value);
        }
        data.put(key, value);
    }

    public void putAll(Map map) {
        data.putAll(map);
    }

    public String get(String key) {
        return data.get(key);
    }

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
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }
}
