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
package org.apache.logging.log4j.core.appender.db.jpa.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.apache.logging.log4j.util.Strings;

/**
 * A JPA 2.1 attribute converter for {@link StackTraceElement}s in {@link org.apache.logging.log4j.core.LogEvent}s. This
 * converter is capable of converting both to and from {@link String}s.
 */
@Converter(autoApply = false)
public class StackTraceElementAttributeConverter implements AttributeConverter<StackTraceElement, String> {
    private static final int UNKNOWN_SOURCE = -1;

    private static final int NATIVE_METHOD = -2;

    @Override
    public String convertToDatabaseColumn(final StackTraceElement element) {
        if (element == null) {
            return null;
        }

        return element.toString();
    }

    @Override
    public StackTraceElement convertToEntityAttribute(final String s) {
        if (Strings.isEmpty(s)) {
            return null;
        }

        return StackTraceElementAttributeConverter.convertString(s);
    }

    static StackTraceElement convertString(final String s) {
        final int open = s.indexOf("(");

        final String classMethod = s.substring(0, open);
        final String className = classMethod.substring(0, classMethod.lastIndexOf("."));
        final String methodName = classMethod.substring(classMethod.lastIndexOf(".") + 1);

        final String parenthesisContents = s.substring(open + 1, s.indexOf(")"));

        String fileName = null;
        int lineNumber = UNKNOWN_SOURCE;
        if ("Native Method".equals(parenthesisContents)) {
            lineNumber = NATIVE_METHOD;
        } else if (!"Unknown Source".equals(parenthesisContents)) {
            final int colon = parenthesisContents.indexOf(":");
            if (colon > UNKNOWN_SOURCE) {
                fileName = parenthesisContents.substring(0, colon);
                try {
                    lineNumber = Integer.parseInt(parenthesisContents.substring(colon + 1));
                } catch (final NumberFormatException ignore) {
                    // we don't care
                }
            } else {
                fileName = parenthesisContents.substring(0);
            }
        }

        return new StackTraceElement(className, methodName, fileName, lineNumber);
    }
}
