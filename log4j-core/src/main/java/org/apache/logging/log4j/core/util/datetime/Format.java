package org.apache.logging.log4j.core.util.datetime;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;

/**
 *
 */
public abstract class Format {

    public final String format (final Object obj) {
        return format(obj, new StringBuilder(), new FieldPosition(0)).toString();
    }

    public abstract StringBuilder format(Object obj, StringBuilder toAppendTo, FieldPosition pos);

    public abstract Object parseObject (String source, ParsePosition pos);

    public Object parseObject(final String source) throws ParseException {
        final ParsePosition pos = new ParsePosition(0);
        final Object result = parseObject(source, pos);
        if (pos.getIndex() == 0) {
            throw new ParseException("Format.parseObject(String) failed", pos.getErrorIndex());
        }
        return result;
    }
}
