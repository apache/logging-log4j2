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
 * Represents a Message that conforms to an RFC 5424 StructuredData element
 * along with the syslog message.
 * <p>
 * Thread-safety note: the contents of this message can be modified after
 * construction.
 * When using asynchronous loggers and appenders it is not recommended to modify
 * this message after the message is
 * logged, because it is undefined whether the logged message string will
 * contain the old values or the modified
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
     * Creates a StructuredDataMessage using an SD-ID (max 32 characters), message,
     * and
     * MSGID (max 32 characters).
     * <p>
     * The {@code sdId} parameter represents the syslog {@code SD-ID} and is
     * expected
     * to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.3.2">RFC
     * 5424 Section 6.3.2</a>.
     * It is recommended to use {@link StructuredDataId} instead of a raw
     * {@link String} where possible,
     * as it allows specifying a set of allowed keys for structured data elements.
     * </p>
     * <p>
     * The {@code msgId} parameter represents the syslog {@code MSGID} and is
     * expected to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.2.7">RFC
     * 5424 Section 6.2.7</a>.
     * </p>
     * <p>
     * Both {@code sdId} and {@code msgId} are considered trusted inputs (typically
     * compile-time constants).
     * If these values are derived from external or untrusted sources, it is the
     * caller's responsibility
     * to validate and sanitize them to ensure RFC-compliant output, especially when
     * used with
     * {@code Rfc5424Layout}.
     * </p>
     *
     * @param sdId  The String SD-ID.
     * @param msg   The message.
     * @param msgId The identifier MSGID.
     */
    public StructuredDataMessage(final String sdId, final String msg, final String msgId) {
        this(sdId, msg, msgId, MAX_LENGTH);
    }

    /**
     * Creates a StructuredDataMessage using an SD-ID (user specified max
     * characters),
     * message, and MSGID (user specified
     * maximum number of characters).
     * <p>
     * The {@code sdId} parameter represents the syslog {@code SD-ID} and is
     * expected
     * to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.3.2">RFC
     * 5424 Section 6.3.2</a>.
     * It is recommended to use {@link StructuredDataId} instead of a raw
     * {@link String} where possible,
     * as it allows specifying a set of allowed keys for structured data elements.
     * </p>
     * <p>
     * The {@code msgId} parameter represents the syslog {@code MSGID} and is
     * expected to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.2.7">RFC
     * 5424 Section 6.2.7</a>.
     * </p>
     * <p>
     * Both {@code sdId} and {@code msgId} are considered trusted inputs (typically
     * compile-time constants).
     * If these values are derived from external or untrusted sources, it is the
     * caller's responsibility
     * to validate and sanitize them to ensure RFC-compliant output, especially when
     * used with
     * {@code Rfc5424Layout}.
     * </p>
     *
     * @param sdId      The String SD-ID.
     * @param msg       The message.
     * @param msgId     The message identifier MSGID.
     * @param maxLength The maximum length of keys;
     * @since 2.9.0
     */
    public StructuredDataMessage(final String sdId, final String msg, final String msgId, final int maxLength) {
        validateSdId(sdId);
        validateMsgId(msgId);

        this.sdId = new StructuredDataId(sdId, null, null, maxLength);
        this.message = msg;
        this.msgId = msgId;
        this.maxLength = maxLength;
    }

    /**
     * Creates a StructuredDataMessage using an SD-ID (max 32 characters), message,
     * MSGID (max 32 characters), and an
     * initial map of structured data to include.
     * <p>
     * The {@code sdId} parameter represents the syslog {@code SD-ID} and is
     * expected
     * to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.3.2">RFC
     * 5424 Section 6.3.2</a>.
     * It is recommended to use {@link StructuredDataId} instead of a raw
     * {@link String} where possible,
     * as it allows specifying a set of allowed keys for structured data elements.
     * </p>
     * <p>
     * The {@code msgId} parameter represents the syslog {@code MSGID} and is
     * expected to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.2.7">RFC
     * 5424 Section 6.2.7</a>.
     * </p>
     * <p>
     * Both {@code sdId} and {@code msgId} are considered trusted inputs (typically
     * compile-time constants).
     * If these values are derived from external or untrusted sources, it is the
     * caller's responsibility
     * to validate and sanitize them to ensure RFC-compliant output, especially when
     * used with
     * {@code Rfc5424Layout}.
     * </p>
     *
     * @param sdId  The String SD-ID.
     * @param msg   The message.
     * @param msgId The message identifier MSGID.
     * @param data  The StructuredData map.
     */
    public StructuredDataMessage(
            final String sdId, final String msg, final String msgId, final Map<String, String> data) {
        this(sdId, msg, msgId, data, MAX_LENGTH);
    }

    /**
     * Creates a StructuredDataMessage using an (user specified max characters),
     * message, and MSGID (user specified
     * maximum number of characters, and an initial map of structured data to
     * include.
     * <p>
     * The {@code sdId} parameter represents the syslog {@code SD-ID} and is
     * expected
     * to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.3.2">RFC
     * 5424 Section 6.3.2</a>.
     * It is recommended to use {@link StructuredDataId} instead of a raw
     * {@link String} where possible,
     * as it allows specifying a set of allowed keys for structured data elements.
     * </p>
     * <p>
     * The {@code msgId} parameter represents the syslog {@code MSGID} and is
     * expected to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.2.7">RFC
     * 5424 Section 6.2.7</a>.
     * </p>
     * <p>
     * Both {@code sdId} and {@code msgId} are considered trusted inputs (typically
     * compile-time constants).
     * If these values are derived from external or untrusted sources, it is the
     * caller's responsibility
     * to validate and sanitize them to ensure RFC-compliant output, especially when
     * used with
     * {@code Rfc5424Layout}.
     * </p>
     *
     * @param sdId      The String SD-ID.
     * @param msg       The message.
     * @param msgId     The message identifier.
     * @param data      The StructuredData map.
     * @param maxLength The maximum length of keys;
     * @since 2.9.0
     */
    public StructuredDataMessage(
            final String sdId,
            final String msg,
            final String msgId,
            final Map<String, String> data,
            final int maxLength) {
        super(data);

        validateSdId(sdId);
        validateMsgId(msgId);

        this.sdId = new StructuredDataId(sdId, null, null, maxLength);
        this.message = msg;
        this.msgId = msgId;
        this.maxLength = maxLength;
    }

    /**
     * Creates a StructuredDataMessage using a StructuredDataId, message, and MSGID
     * (max 32 characters).
     * <p>
     * The {@link StructuredDataId} parameter represents the syslog {@code SD-ID}
     * and is expected to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.3.2">RFC
     * 5424 Section 6.3.2</a>.
     * </p>
     * <p>
     * The {@code msgId} parameter represents the syslog {@code MSGID} and is
     * expected to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.2.7">RFC
     * 5424 Section 6.2.7</a>.
     * </p>
     * <p>
     * Both {@code sdId} and {@code msgId} are considered trusted inputs (typically
     * compile-time constants).
     * If these values are derived from external or untrusted sources, it is the
     * caller's responsibility
     * to validate and sanitize them to ensure RFC-compliant output, especially when
     * used with
     * {@code Rfc5424Layout}.
     * </p>
     *
     * @param sdId  The StructuredDataId.
     * @param msg   The message.
     * @param msgId The message identifier MSGID.
     */
    public StructuredDataMessage(final StructuredDataId sdId, final String msg, final String msgId) {
        this(sdId, msg, msgId, MAX_LENGTH);
    }

    /**
     * Creates a StructuredDataMessage using a StructuredDataId, message, and MSGID
     * (max 32 characters).
     * <p>
     * The {@link StructuredDataId} parameter represents the syslog {@code SD-ID}
     * and is expected to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.3.2">RFC
     * 5424 Section 6.3.2</a>.
     * </p>
     * <p>
     * The {@code msgId} parameter represents the syslog {@code MSGID} and is
     * expected to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.2.7">RFC
     * 5424 Section 6.2.7</a>.
     * </p>
     * <p>
     * Both {@code sdId} and {@code msgId} are considered trusted inputs (typically
     * compile-time constants).
     * If these values are derived from external or untrusted sources, it is the
     * caller's responsibility
     * to validate and sanitize them to ensure RFC-compliant output, especially when
     * used with
     * {@code Rfc5424Layout}.
     * </p>
     *
     * @param sdId      The StructuredDataId.
     * @param msg       The message.
     * @param msgId     The message identifier MSGID.
     * @param maxLength The maximum length of keys;
     * @since 2.9.0
     */
    public StructuredDataMessage(
            final StructuredDataId sdId, final String msg, final String msgId, final int maxLength) {

        if (sdId == null) {
            throw new IllegalArgumentException("SD-ID cannot be null");
        }
        validateMsgId(msgId);

        this.sdId = sdId;
        this.message = msg;
        this.msgId = msgId;
        this.maxLength = maxLength;
    }

    /**
     * Creates a StructuredDataMessage using a StructuredDataId, message, MSGID (max
     * 32 characters), and an initial map
     * of structured data to include.
     * <p>
     * The {@link StructuredDataId} parameter represents the syslog {@code SD-ID}
     * and is expected to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.3.2">RFC
     * 5424 Section 6.3.2</a>.
     * </p>
     * <p>
     * The {@code msgId} parameter represents the syslog {@code MSGID} and is
     * expected to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.2.7">RFC
     * 5424 Section 6.2.7</a>.
     * </p>
     * <p>
     * Both {@code sdId} and {@code msgId} are considered trusted inputs (typically
     * compile-time constants).
     * If these values are derived from external or untrusted sources, it is the
     * caller's responsibility
     * to validate and sanitize them to ensure RFC-compliant output, especially when
     * used with
     * {@code Rfc5424Layout}.
     * </p>
     *
     * @param sdId  The StructuredDataId.
     * @param msg   The message.
     * @param msgId The message identifier MSGID.
     * @param data  The StructuredData map.
     */
    public StructuredDataMessage(
            final StructuredDataId sdId, final String msg, final String msgId, final Map<String, String> data) {
        this(sdId, msg, msgId, data, MAX_LENGTH);
    }

    /**
     * Creates a StructuredDataMessage using a StructuredDataId, message, MSGID (max
     * 32 characters), and an initial map
     * of structured data to include.
     * <p>
     * The {@link StructuredDataId} parameter represents the syslog {@code SD-ID}
     * and is expected to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.3.2">RFC
     * 5424 Section 6.3.2</a>.
     * </p>
     * <p>
     * The {@code msgId} parameter represents the syslog {@code MSGID} and is
     * expected to conform to
     * <a href="https://datatracker.ietf.org/doc/html/rfc5424#section-6.2.7">RFC
     * 5424 Section 6.2.7</a>.
     * </p>
     * <p>
     * Both {@code sdId} and {@code msgId} are considered trusted inputs (typically
     * compile-time constants).
     * If these values are derived from external or untrusted sources, it is the
     * caller's responsibility
     * to validate and sanitize them to ensure RFC-compliant output, especially when
     * used with
     * {@code Rfc5424Layout}.
     * </p>
     *
     * @param sdId      The StructuredDataId.
     * @param msg       The message.
     * @param msgId     The message identifier MSGID.
     * @param data      The StructuredData map.
     * @param maxLength The maximum length of keys;
     * @since 2.9.0
     */
    public StructuredDataMessage(
            final StructuredDataId sdId,
            final String msg,
            final String msgId,
            final Map<String, String> data,
            final int maxLength) {
        super(data);

        if (sdId == null) {
            throw new IllegalArgumentException("SD-ID cannot be null");
        }

        validateMsgId(msgId);

        this.sdId = sdId;
        this.message = msg;
        this.msgId = msgId;
        this.maxLength = maxLength;
    }

    /**
     * Constructor based on a StructuredDataMessage.
     *
     * @param msg The StructuredDataMessage.
     * @param map The StructuredData map.
     */
    private StructuredDataMessage(final StructuredDataMessage msg, final Map<String, String> map) {
        super(map);
        this.sdId = msg.sdId;
        this.message = msg.message;
        this.msgId = msg.msgId;
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
     *
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
     * Returns the Structured Data ID (SD-ID) of this message.
     *
     * @return the StructuredDataId.
     */
    public StructuredDataId getId() {
        return sdId;
    }

    /**
     * Sets the sdId from a String. This sdId can be at most 32 characters long.
     *
     * @param sdId The String sdId.
     */
    protected void setId(final String sdId) {
        validateSdId(sdId);
        this.sdId = new StructuredDataId(sdId, null, null);
    }

    /**
     * Sets the sdId.
     *
     * @param sdId The StructuredDataId.
     */
    protected void setId(final StructuredDataId sdId) {
        if (sdId == null) {
            throw new IllegalArgumentException("SD-ID cannot be null");
        }
        this.sdId = sdId;
    }

    /**
     * Returns the message identifier (MSGID).
     *
     * @return the msgId.
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
     *
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
     * @param format           "full" will include the type and message. null will
     *                         return only the STRUCTURED-DATA as
     *                         described in RFC 5424
     * @param structuredDataId The SD-ID as described in RFC 5424. If null the value
     *                         in the StructuredData
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
     * @param format           "full" will include the type and message. null will
     *                         return only the STRUCTURED-DATA as
     *                         described in RFC 5424
     * @param structuredDataId The SD-ID as described in RFC 5424. If null the value
     *                         in the StructuredData
     *                         will be used.
     * @param sb               The StringBuilder to append the formatted message to.
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
        sb.append("<type>").append(msgId).append("</type>\n");
        sb.append("<id>").append(structuredDataId).append("</id>\n");
        super.asXml(sb);
        sb.append("\n</StructuredData>\n");
    }

    /**
     * Formats the message and return it.
     *
     * @return the formatted message.
     */
    @Override
    public String getFormattedMessage() {
        return asString(Format.FULL, null);
    }

    /**
     * Formats the message according to the specified format.
     *
     * @param formats An array of Strings that provide extra information about how
     *                to format the message.
     *                StructuredDataMessage accepts only a format of "FULL" which
     *                will cause the event type to be
     *                prepended and the event message to be appended. Specifying any
     *                other value will cause only the
     *                StructuredData to be included. The default is "FULL".
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

    private void validateSdId(final String sdId) {
        if (sdId == null) {
            throw new IllegalArgumentException("SD-ID cannot be null");
        }
        validateKey(sdId);
    }

    private void validateMsgId(final String msgId) {
        if (msgId == null) {
            throw new IllegalArgumentException("MSGID cannot be null");
        }
        if (msgId.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("MSGID exceeds maximum length of 32 characters: " + msgId);
        }
        for (int i = 0; i < msgId.length(); i++) {
            final char c = msgId.charAt(i);
            if (c < '!' || c > '~') {
                throw new IllegalArgumentException("MSGID must contain printable US ASCII characters: " + msgId);
            }
        }
    }
}
