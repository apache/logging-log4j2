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
 * The SD-ID, the MSGID and every key are checked against the syntax of
 * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6">RFC 5424</a>, and an invalid value throws an
 * {@link IllegalArgumentException}.
 * Prefer the constructors that take a {@link StructuredDataId}: when it declares required or optional keys, this
 * message accepts only those keys.
 * </p>
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

    private StructuredDataId sdId;

    private String message;

    private String msgId;

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
     * Creates a StructuredDataMessage using an SD-ID (max 32 characters), message, and MSGID (max 32 characters).
     * @param sdId The SD-ID, as described in RFC 5424 section 6.3.2.
     * @param msg The message.
     * @param msgId The MSGID, as described in RFC 5424 section 6.2.7.
     * @throws IllegalArgumentException if {@code sdId} or {@code msgId} is not valid.
     */
    public StructuredDataMessage(final String sdId, final String msg, final String msgId) {
        this(sdId, msg, msgId, null, MAX_LENGTH);
    }

    /**
     * Creates a StructuredDataMessage using an SD-ID (user specified max characters), message, and MSGID (max 32
     * characters).
     * @param sdId The SD-ID, as described in RFC 5424 section 6.3.2.
     * @param msg The message.
     * @param msgId The MSGID, as described in RFC 5424 section 6.2.7.
     * @param maxLength The maximum length of the SD-ID and of keys;
     * @throws IllegalArgumentException if {@code sdId} or {@code msgId} is not valid.
     * @since 2.9.0
     */
    public StructuredDataMessage(final String sdId, final String msg, final String msgId, final int maxLength) {
        this(sdId, msg, msgId, null, maxLength);
    }

    /**
     * Creates a StructuredDataMessage using an SD-ID (max 32 characters), message, MSGID (max 32 characters), and an
     * initial map of structured data to include.
     * @param sdId The SD-ID, as described in RFC 5424 section 6.3.2.
     * @param msg The message.
     * @param msgId The MSGID, as described in RFC 5424 section 6.2.7.
     * @param data The StructuredData map.
     * @throws IllegalArgumentException if {@code sdId}, {@code msgId} or a key of {@code data} is not valid.
     */
    public StructuredDataMessage(
            final String sdId, final String msg, final String msgId, final Map<String, String> data) {
        this(sdId, msg, msgId, data, MAX_LENGTH);
    }

    /**
     * Creates a StructuredDataMessage using an SD-ID (user specified max characters), message, MSGID (max 32
     * characters), and an initial map of structured data to include.
     * @param sdId The SD-ID, as described in RFC 5424 section 6.3.2.
     * @param msg The message.
     * @param msgId The MSGID, as described in RFC 5424 section 6.2.7.
     * @param data The StructuredData map.
     * @param maxLength The maximum length of the SD-ID and of keys;
     * @throws IllegalArgumentException if {@code sdId}, {@code msgId} or a key of {@code data} is not valid.
     * @since 2.9.0
     */
    public StructuredDataMessage(
            final String sdId,
            final String msg,
            final String msgId,
            final Map<String, String> data,
            final int maxLength) {
        this(toStructuredDataId(sdId, maxLength), msg, msgId, data, maxLength);
    }

    /**
     * Creates a StructuredDataMessage using a StructuredDataId, message, and MSGID (max 32 characters).
     * @param sdId The StructuredDataId.
     * @param msg The message.
     * @param msgId The MSGID, as described in RFC 5424 section 6.2.7.
     * @throws IllegalArgumentException if {@code sdId} is null or {@code msgId} is not valid.
     */
    public StructuredDataMessage(final StructuredDataId sdId, final String msg, final String msgId) {
        this(sdId, msg, msgId, null, MAX_LENGTH);
    }

    /**
     * Creates a StructuredDataMessage using a StructuredDataId, message, and MSGID (max 32 characters).
     * @param sdId The StructuredDataId.
     * @param msg The message.
     * @param msgId The MSGID, as described in RFC 5424 section 6.2.7.
     * @param maxLength The maximum length of keys;
     * @throws IllegalArgumentException if {@code sdId} is null or {@code msgId} is not valid.
     * @since 2.9.0
     */
    public StructuredDataMessage(
            final StructuredDataId sdId, final String msg, final String msgId, final int maxLength) {
        this(sdId, msg, msgId, null, maxLength);
    }

    /**
     * Creates a StructuredDataMessage using a StructuredDataId, message, MSGID (max 32 characters), and an initial map
     * of structured data to include.
     * @param sdId The StructuredDataId.
     * @param msg The message.
     * @param msgId The MSGID, as described in RFC 5424 section 6.2.7.
     * @param data The StructuredData map.
     * @throws IllegalArgumentException if {@code sdId} is null, or {@code msgId} or a key of {@code data} is not valid.
     */
    public StructuredDataMessage(
            final StructuredDataId sdId, final String msg, final String msgId, final Map<String, String> data) {
        this(sdId, msg, msgId, data, MAX_LENGTH);
    }

    /**
     * Creates a StructuredDataMessage using a StructuredDataId, message, MSGID (max 32 characters), and an initial map
     * of structured data to include.
     * <p>
     * All other public constructors delegate to this one.
     * The MSGID must be 1 to 32 printable US-ASCII characters, as described in
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.2.7">RFC 5424 section 6.2.7</a>.
     * Each key must be a valid PARAM-NAME and, if {@code sdId} declares required or optional keys, one of those keys.
     * </p>
     * @param sdId The StructuredDataId.
     * @param msg The message.
     * @param msgId The MSGID.
     * @param data The StructuredData map, may be null.
     * @param maxLength The maximum length of keys;
     * @throws IllegalArgumentException if {@code sdId} is null, or {@code msgId} or a key of {@code data} is not valid.
     * @since 2.9.0
     */
    public StructuredDataMessage(
            final StructuredDataId sdId,
            final String msg,
            final String msgId,
            final Map<String, String> data,
            final int maxLength) {
        if (sdId == null) {
            throw new IllegalArgumentException("No SD-ID was supplied");
        }
        validateMsgId(msgId);
        this.sdId = sdId;
        this.message = msg;
        this.msgId = msgId;
        this.maxLength = maxLength;
        if (data != null) {
            putAll(data);
        }
    }

    /**
     * Constructor based on a StructuredDataMessage.
     * @param msg The StructuredDataMessage.
     * @param map The StructuredData map.
     */
    private StructuredDataMessage(final StructuredDataMessage msg, final Map<String, String> map) {
        this.sdId = msg.sdId;
        this.message = msg.message;
        this.msgId = msg.msgId;
        this.maxLength = msg.maxLength;
        putAll(map);
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
     * Returns the SD-ID of this message.
     * @return the StructuredDataId.
     */
    public StructuredDataId getId() {
        return sdId;
    }

    /**
     * Sets the SD-ID from a String. This SD-ID can be at most 32 characters long.
     * @param sdId The SD-ID.
     * @throws IllegalArgumentException if {@code sdId} is not valid.
     */
    protected void setId(final String sdId) {
        setId(toStructuredDataId(sdId, MAX_LENGTH));
    }

    /**
     * Sets the SD-ID.
     * @param sdId The StructuredDataId.
     * @throws IllegalArgumentException if {@code sdId} is null or does not declare a key of this message.
     */
    protected void setId(final StructuredDataId sdId) {
        if (sdId == null) {
            throw new IllegalArgumentException("No SD-ID was supplied");
        }
        for (final String key : getData().keySet()) {
            validateDeclaredKey(sdId, key);
        }
        this.sdId = sdId;
    }

    /**
     * Returns the MSGID of this message.
     * @return the MSGID.
     */
    public String getType() {
        return msgId;
    }

    protected void setType(final String msgId) {
        validateMsgId(msgId);
        this.msgId = msgId;
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
     * @since 2.8
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

        // Encode type
        sb.append("<type>");
        int start = sb.length();
        sb.append(msgId);
        StringBuilders.escapeXml(sb, start);
        sb.append("</type>\n");

        // Encode ID
        sb.append("<id>");
        start = sb.length();
        structuredDataId.formatTo(sb);
        StringBuilders.escapeXml(sb, start);
        sb.append("</id>\n");

        // Encode message as its own element (distinct from a map entry keyed "message")
        if (message != null) {
            sb.append("<message>");
            start = sb.length();
            sb.append(message);
            StringBuilders.escapeXml(sb, start);
            sb.append("</message>\n");
        }

        // Encode the rest
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
        if (msgId != null ? !msgId.equals(that.msgId) : that.msgId != null) {
            return false;
        }
        if (sdId != null ? !sdId.equals(that.sdId) : that.sdId != null) {
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
        result = HASHVAL * result + (msgId != null ? msgId.hashCode() : 0);
        result = HASHVAL * result + (sdId != null ? sdId.hashCode() : 0);
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

    /**
     * @since 2.9.0
     */
    protected void validateKey(final String key) {
        if (key == null) {
            throw new IllegalArgumentException("Structured data keys cannot be null");
        }
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
        if (sdId != null) {
            validateDeclaredKey(sdId, key);
        }
    }

    private static StructuredDataId toStructuredDataId(final String sdId, final int maxLength) {
        if (sdId == null) {
            throw new IllegalArgumentException("No SD-ID was supplied");
        }
        return new StructuredDataId(sdId, null, null, maxLength);
    }

    private static void validateDeclaredKey(final StructuredDataId sdId, final String key) {
        final String[] required = sdId.getRequired();
        final String[] optional = sdId.getOptional();
        if ((required == null && optional == null) || contains(required, key) || contains(optional, key)) {
            return;
        }
        throw new IllegalArgumentException(
                "Structured data key " + key + " is not declared by SD-ID " + sdId.getName());
    }

    private static boolean contains(final String[] keys, final String key) {
        if (keys != null) {
            for (final String candidate : keys) {
                if (key.equals(candidate)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void validateMsgId(final String msgId) {
        if (msgId == null || msgId.isEmpty()) {
            throw new IllegalArgumentException("No MSGID was supplied");
        }
        if (msgId.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "MSGID exceeds maximum length of " + MAX_LENGTH + " characters: " + msgId);
        }
        for (int i = 0; i < msgId.length(); i++) {
            final char c = msgId.charAt(i);
            if (c < '!' || c > '~') {
                throw new IllegalArgumentException(
                        "MSGID must contain printable US ASCII characters and may not contain a space: " + msgId);
            }
        }
    }
}
