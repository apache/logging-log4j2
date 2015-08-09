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
package org.apache.logging.log4j.core.util.datetime;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;

/**
 * The basic methods for performing date formatting.
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
