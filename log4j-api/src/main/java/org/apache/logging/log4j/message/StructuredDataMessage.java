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

import java.util.Map;
import org.apache.logging.log4j.util.EnglishEnums;
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
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5424">RFC 5424</a>
 */
@AsynchronouslyFormattable
public class StructuredDataMessage extends MapMessage<StructuredDataMessage, String> {

    private static final long serialVersionUID = 1703221292892071920L;
    private static final int MAX_LENGTH = 32;
    private static final int HASHVAL = 31;

    private StructuredDataId id;

    private String message;

    private String type;

    private final int maxLength;

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
        this(id, msg, type, MAX_LENGTH);
    }

    /**
     * Creates a StructuredDataMessage using an ID (user specified max characters), message, and type (user specified
     * maximum number of characters).
     * @param id The String id.
     * @param msg The message.
     * @param type The message type.
     * @param maxLength The maximum length of keys;
     * @since 2.9
     */
    public StructuredDataMessage(final String id, final String msg, final String type, final int maxLength) {
        this.id = new StructuredDataId(id, null, null, maxLength);
        this.message = msg;
        this.type = type;
        this.maxLength = maxLength;
    }

    /**
     * Creates a StructuredDataMessage using an ID (max 32 characters), message, type (max 32 characters), and an
     * initial map of structured data to include.
     * @param id The String id.
     * @param msg The message.
     * @param type The message type.
     * @param data The StructuredData map.
     */
    public StructuredDataMessage(final String id, final String msg, final String type, final Map<String, String> data) {
        this(id, msg, type, data, MAX_LENGTH);
    }

    /**
     * Creates a StructuredDataMessage using an (user specified max characters), message, and type (user specified
     * maximum number of characters, and an initial map of structured data to include.
     * @param id The String id.
     * @param msg The message.
     * @param type The message type.
     * @param data The StructuredData map.
     * @param maxLength The maximum length of keys;
     * @since 2.9
     */
    public StructuredDataMessage(
            final String id, final String msg, final String type, final Map<String, String> data, final int maxLength) {
        super(data);
        this.id = new StructuredDataId(id, null, null, maxLength);
        this.message = msg;
        this.type = type;
        this.maxLength = maxLength;
    }

    /**
     * Creates a StructuredDataMessage using a StructuredDataId, message, and type (max 32 characters).
     * @param id The StructuredDataId.
     * @param msg The message.
     * @param type The message type.
     */
    public StructuredDataMessage(final StructuredDataId id, final String msg, final String type) {
        this(id, msg, type, MAX_LENGTH);
    }

    /**
     * Creates a StructuredDataMessage using a StructuredDataId, message, and type (max 32 characters).
     * @param id The StructuredDataId.
     * @param msg The message.
     * @param type The message type.
     * @param maxLength The maximum length of keys;
     * @since 2.9
     */
    public StructuredDataMessage(final StructuredDataId id, final String msg, final String type, final int maxLength) {
        this.id = id;
        this.message = msg;
        this.type = type;
        this.maxLength = maxLength;
    }

    /**
     * Creates a StructuredDataMessage using a StructuredDataId, message, type (max 32 characters), and an initial map
     * of structured data to include.
     * @param id The StructuredDataId.
     * @param msg The message.
     * @param type The message type.
     * @param data The StructuredData map.
     */
    public StructuredDataMessage(
            final StructuredDataId id, final String msg, final String type, final Map<String, String> data) {
        this(id, msg, type, data, MAX_LENGTH);
    }

    /**
     * Creates a StructuredDataMessage using a StructuredDataId, message, type (max 32 characters), and an initial map
     * of structured data to include.
     * @param id The StructuredDataId.
     * @param msg The message.
     * @param type The message type.
     * @param data The StructuredData map.
     * @param maxLength The maximum length of keys;
     * @since 2.9
     */
    public StructuredDataMessage(
            final StructuredDataId id,
            final String msg,
            final String type,
            final Map<String, String> data,
            final int maxLength) {
        super(data);
        this.id = id;
        this.message = msg;
        this.type = type;
        this.maxLength = maxLength;
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
        this.maxLength = MAX_LENGTH;
    }

    /**
     * Basic constructor.
     */
    protected StructuredDataMessage() {
        maxLength = MAX_LENGTH;
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

    @Override
    public void formatTo(final String[] formats, final StringBuilder buffer) {
        asString(getFormat(formats), null, buffer);
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
        if (Format.XML.equals(format)) {
            asXml(sdId, sb);
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

    private void asXml(final StructuredDataId structuredDataId, final StringBuilder sb) {
        sb.append("<StructuredData>\n");
        sb.append("<type>").append(type).append("</type>\n");
        sb.append("<id>").append(structuredDataId).append("</id>\n");
        super.asXml(sb);
        sb.append("\n</StructuredData>\n");
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
     * Formats the message according to the specified format.
     * @param formats An array of Strings that provide extra information about how to format the message.
     * StructuredDataMessage accepts only a format of "FULL" which will cause the event type to be
     * prepended and the event message to be appended. Specifying any other value will cause only the
     * StructuredData to be included. The default is "FULL".
     *
     * @return the formatted message.
     */
    @Override
    public String getFormattedMessage(final String[] formats) {
        return asString(getFormat(formats), null);
    }

    private Format getFormat(final String[] formats) {
        if (formats != null && formats.length > 0) {
            for (int i = 0; i < formats.length; i++) {
                final String format = formats[i];
                if (Format.XML.name().equalsIgnoreCase(format)) {
                    return Format.XML;
                } else if (Format.FULL.name().equalsIgnoreCase(format)) {
                    return Format.FULL;
                }
            }
            return null;
        }
        return Format.FULL;
    }

    @Override
    public String toString() {
        return asString(null, null);
    }

    @Override
    public StructuredDataMessage newInstance(final Map<String, String> map) {
        return new StructuredDataMessage(this, map);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StructuredDataMessage)) {
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

    @Override
    protected void validate(final String key, final boolean value) {
        validateKey(key);
    }

    /**
     * @since 2.9
     */
    @Override
    protected void validate(final String key, final byte value) {
        validateKey(key);
    }

    /**
     * @since 2.9
     */
    @Override
    protected void validate(final String key, final char value) {
        validateKey(key);
    }

    /**
     * @since 2.9
     */
    @Override
    protected void validate(final String key, final double value) {
        validateKey(key);
    }

    /**
     * @since 2.9
     */
    @Override
    protected void validate(final String key, final float value) {
        validateKey(key);
    }

    /**
     * @since 2.9
     */
    @Override
    protected void validate(final String key, final int value) {
        validateKey(key);
    }

    /**
     * @since 2.9
     */
    @Override
    protected void validate(final String key, final long value) {
        validateKey(key);
    }

    /**
     * @since 2.9
     */
    @Override
    protected void validate(final String key, final Object value) {
        validateKey(key);
    }

    /**
     * @since 2.9
     */
    @Override
    protected void validate(final String key, final short value) {
        validateKey(key);
    }

    @Override
    protected void validate(final String key, final String value) {
        validateKey(key);
    }

    protected void validateKey(final String key) {
        if (maxLength > 0 && key.length() > maxLength) {
            throw new IllegalArgumentException(
                    "Structured data keys are limited to " + maxLength + " characters. key: " + key);
        }
        for (int i = 0; i < key.length(); i++) {
            final char c = key.charAt(i);
            if (c < '!' || c > '~' || c == '=' || c == ']' || c == '"') {
                throw new IllegalArgumentException("Structured data keys must contain printable US ASCII characters"
                        + "and may not contain a space, =, ], or \"");
            }
        }
    }
}
