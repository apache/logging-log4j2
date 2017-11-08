package org.apache.logging.log4j.util;

import org.apache.logging.log4j.message.MultiformatMessage;

/**
 * A Message that can render itself in more than one way. The format string is used by the
 * Message implementation as extra information that it may use to help it to determine how
 * to format itself. For example, MapMessage accepts a format of "XML" to tell it to render
 * the Map as XML instead of its default format of {key1="value1" key2="value2"}.
 *
 * @since 2.10
 */
public interface MultiFormatStringBuilderFormattable extends MultiformatMessage, StringBuilderFormattable {

    /**
     * Writes a text representation of this object into the specified {@code StringBuilder}, ideally without allocating
     * temporary objects.
     *
     * @param formats An array of Strings that provide extra information about how to format the message.
     * Each MultiFormatStringBuilderFormattable implementation is free to use the provided formats however they choose.
     * @param buffer the StringBuilder to write into
     */
    void formatTo(String[] formats, StringBuilder buffer);

}
