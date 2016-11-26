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

import java.util.Map;

import org.apache.logging.log4j.util.EnglishEnums;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.StringBuilders;

/**
 * Represents a Message that conforms to an RFC 5424 StructuredData element along with the syslog message.
 * <p>
 * Thread-safety note: the contents of this message can be modified after construction.
 * When using asynchronous loggers and appenders it is not recommended to modify this message after the message is
 * logged, because it is undefined whether the logged message string will contain the old values or the modified
 * values.
 * </p>
 *
 * @see <a href="https://tools.ietf.org/html/rfc5424">RFC 5424</a>
 */
@AsynchronouslyFormattable
public class StructuredDataMessage extends MapMessage implements StringBuilderFormattable {

    private static final long serialVersionUID = 1703221292892071920L;
    private static final int MAX_LENGTH = 32;
    private static final int HASHVAL = 31;

    private StructuredDataId id;

    private String message;

    private String type;

    /**
     * Supported formats.
     */
    public enum Format {
        /** The map should be formatted as XML. */
        XML,
        /** Full message format includes the type and message. */
        FULL
    }

    /**
     * Creates a StructuredDataMessage using an ID (max 32 characters), message, and type (max 32 characters).
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
     * Creates a StructuredDataMessage using an ID (max 32 characters), message, type (max 32 characters), and an
     * initial map of structured data to include.
     * @param id The String id.
     * @param msg The message.
     * @param type The message type.
     * @param data The StructuredData map.
     */
    public StructuredDataMessage(final String id, final String msg, final String type,
                                 final Map<String, String> data) {
        super(data);
        this.id = new StructuredDataId(id, null, null);
        this.message = msg;
        this.type = type;
    }

    /**
     * Creates a StructuredDataMessage using a StructuredDataId, message, and type (max 32 characters).
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
     * Creates a StructuredDataMessage using a StructuredDataId, message, type (max 32 characters), and an initial map
     * of structured data to include.
     * @param id The StructuredDataId.
     * @param msg The message.
     * @param type The message type.
     * @param data The StructuredData map.
     */
    public StructuredDataMessage(final StructuredDataId id, final String msg, final String type,
                                 final Map<String, String> data) {
        super(data);
        this.id = id;
        this.message = msg;
        this.type = type;
    }


    /**
     * Constructor based on a StructuredDataMessage.
     * @param msg The StructuredDataMessage.
     * @param map The StructuredData map.
     */
    private StructuredDataMessage(final StructuredDataMessage msg, final Map<String, String> map) {
        super(map);
        this.id = msg.id;
        this.message = msg.message;
        this.type = msg.type;
    }


    /**
     * Basic constructor.
     */
    protected StructuredDataMessage() {

    }

    /**
     * Add an item to the data Map in fluent style.
     * @param key The name of the data item.
     * @param value The value of the data item.
     * @return {@code this}
     */
    @Override
    public StructuredDataMessage with(final String key, final String value) {
        put(key, value);
        return this;
    }

    /**
     * Returns the supported formats.
     * @return An array of the supported format names.
     */
    @Override
    public String[] getFormats() {
        final String[] formats = new String[Format.values().length];
        int i = 0;
        for (final Format format : Format.values()) {
            formats[i++] = format.name();
        }
        return formats;
    }

    /**
     * Returns this message id.
     * @return the StructuredDataId.
     */
    public StructuredDataId getId() {
        return id;
    }

    /**
     * Sets the id from a String. This ID can be at most 32 characters long.
     * @param id The String id.
     */
    protected void setId(final String id) {
        this.id = new StructuredDataId(id, null, null);
    }

    /**
     * Sets the id.
     * @param id The StructuredDataId.
     */
    protected void setId(final StructuredDataId id) {
        this.id = id;
    }

    /**
     * Returns this message type.
     * @return the type.
     */
    public String getType() {
        return type;
    }

    protected void setType(final String type) {
        if (type.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("structured data type exceeds maximum length of 32 characters: " + type);
        }
        this.type = type;
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        asString(Format.FULL, null, buffer);
    }

    /**
     * Returns the message.
     * @return the message.
     */
    @Override
    public String getFormat() {
        return message;
    }

    protected void setMessageFormat(final String msg) {
        this.message = msg;
    }


    @Override
    protected void validate(final String key, final String value) {
        validateKey(key);
    }

    private void validateKey(final String key) {
        if (key.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Structured data keys are limited to 32 characters. key: " + key);
        }
        for (int i = 0; i < key.length(); i++) {
            final char c = key.charAt(i);
            if (c < '!' || c > '~' || c == '=' || c == ']' || c == '"') {
                throw new IllegalArgumentException("Structured data keys must contain printable US ASCII characters" +
                        "and may not contain a space, =, ], or \"");
            }
        }
    }

    /**
     * Formats the structured data as described in RFC 5424.
     *
     * @return The formatted String.
     */
    @Override
    public String asString() {
        return asString(Format.FULL, null);
    }

    /**
     * Formats the structured data as described in RFC 5424.
     *
     * @param format The format identifier. Ignored in this implementation.
     * @return The formatted String.
     */

    @Override
    public String asString(final String format) {
        try {
            return asString(EnglishEnums.valueOf(Format.class, format), null);
        } catch (final IllegalArgumentException ex) {
            return asString();
        }
    }

    /**
     * Formats the structured data as described in RFC 5424.
     *
     * @param format           "full" will include the type and message. null will return only the STRUCTURED-DATA as
     *                         described in RFC 5424
     * @param structuredDataId The SD-ID as described in RFC 5424. If null the value in the StructuredData
     *                         will be used.
     * @return The formatted String.
     */
    public final String asString(final Format format, final StructuredDataId structuredDataId) {
        final StringBuilder sb = new StringBuilder();
        asString(format, structuredDataId, sb);
        return sb.toString();
    }

    /**
     * Formats the structured data as described in RFC 5424.
     *
     * @param format           "full" will include the type and message. null will return only the STRUCTURED-DATA as
     *                         described in RFC 5424
     * @param structuredDataId The SD-ID as described in RFC 5424. If null the value in the StructuredData
     *                         will be used.
     * @param sb The StringBuilder to append the formatted message to.
     */
    public final void asString(final Format format, final StructuredDataId structuredDataId, final StringBuilder sb) {
        final boolean full = Format.FULL.equals(format);
        if (full) {
            final String myType = getType();
            if (myType == null) {
                return;
            }
            sb.append(getType()).append(' ');
        }
        StructuredDataId sdId = getId();
        if (sdId != null) {
            sdId = sdId.makeId(structuredDataId); // returns sdId if structuredDataId is null
        } else {
            sdId = structuredDataId;
        }
        if (sdId == null || sdId.getName() == null) {
            return;
        }
        sb.append('[');
        StringBuilders.appendValue(sb, sdId); // avoids toString if implements StringBuilderFormattable
        sb.append(' ');
        appendMap(sb);
        sb.append(']');
        if (full) {
            final String msg = getFormat();
            if (msg != null) {
                sb.append(' ').append(msg);
            }
        }
    }

    /**
     * Formats the message and return it.
     * @return the formatted message.
     */
    @Override
    public String getFormattedMessage() {
        return asString(Format.FULL, null);
    }

    /**
     * Formats the message according the the specified format.
     * @param formats An array of Strings that provide extra information about how to format the message.
     * StructuredDataMessage accepts only a format of "FULL" which will cause the event type to be
     * prepended and the event message to be appended. Specifying any other value will cause only the
     * StructuredData to be included. The default is "FULL".
     *
     * @return the formatted message.
     */
    @Override
    public String getFormattedMessage(final String[] formats) {
        if (formats != null && formats.length > 0) {
            for (int i = 0; i < formats.length; i++) {
                final String format = formats[i];
                if (Format.XML.name().equalsIgnoreCase(format)) {
                    return asXml();
                } else if (Format.FULL.name().equalsIgnoreCase(format)) {
                    return asString(Format.FULL, null);
                }
            }
            return asString(null, null);
        }
        return asString(Format.FULL, null);
    }

    private String asXml() {
        final StringBuilder sb = new StringBuilder();
        final StructuredDataId sdId = getId();
        if (sdId == null || sdId.getName() == null || type == null) {
            return sb.toString();
        }
        sb.append("<StructuredData>\n");
        sb.append("<type>").append(type).append("</type>\n");
        sb.append("<id>").append(sdId).append("</id>\n");
        super.asXml(sb);
        sb.append("</StructuredData>\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return asString(null, null);
    }


    @Override
    public MapMessage newInstance(final Map<String, String> map) {
        return new StructuredDataMessage(this, map);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final StructuredDataMessage that = (StructuredDataMessage) o;

        if (!super.equals(o)) {
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

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = HASHVAL * result + (type != null ? type.hashCode() : 0);
        result = HASHVAL * result + (id != null ? id.hashCode() : 0);
        result = HASHVAL * result + (message != null ? message.hashCode() : 0);
        return result;
    }
}
