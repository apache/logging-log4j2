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
package org.apache.log4j.spi;

import java.io.Serializable;
import java.util.Objects;
import org.apache.logging.log4j.core.util.Integers;

/**
 * The internal representation of caller location information.
 *
 * @since 0.8.3
 */
public class LocationInfo implements Serializable {

    /**
     * When location information is not available the constant <code>NA</code> is returned. Current value of this string
     * constant is <b>?</b>.
     */
    public static final String NA = "?";

    static final long serialVersionUID = -1325822038990805636L;

    private final StackTraceElement stackTraceElement;

    public String fullInfo;

    /**
     * Constructs a new instance.
     */
    public LocationInfo(final StackTraceElement stackTraceElement) {
        this.stackTraceElement = Objects.requireNonNull(stackTraceElement, "stackTraceElement");
        this.fullInfo = stackTraceElement.toString();
    }

    /**
     * Constructs a new instance.
     *
     * @param file source file name
     * @param declaringClass class name
     * @param methodName method
     * @param line source line number
     *
     * @since 1.2.15
     */
    public LocationInfo(final String file, final String declaringClass, final String methodName, final String line) {
        this(new StackTraceElement(declaringClass, methodName, file, Integer.parseInt(line)));
    }

    /**
     * Constructs a new instance.
     */
    public LocationInfo(final Throwable throwable, final String fqnOfCallingClass) {
        String declaringClass = null, methodName = null, file = null, line = null;
        if (throwable != null && fqnOfCallingClass != null) {
            final StackTraceElement[] elements = throwable.getStackTrace();
            String prevClass = NA;
            for (int i = elements.length - 1; i >= 0; i--) {
                final String thisClass = elements[i].getClassName();
                if (fqnOfCallingClass.equals(thisClass)) {
                    final int caller = i + 1;
                    if (caller < elements.length) {
                        declaringClass = prevClass;
                        methodName = elements[caller].getMethodName();
                        file = elements[caller].getFileName();
                        if (file == null) {
                            file = NA;
                        }
                        final int lineNo = elements[caller].getLineNumber();
                        if (lineNo < 0) {
                            line = NA;
                        } else {
                            line = String.valueOf(lineNo);
                        }
                        final StringBuilder builder = new StringBuilder();
                        builder.append(declaringClass);
                        builder.append(".");
                        builder.append(methodName);
                        builder.append("(");
                        builder.append(file);
                        builder.append(":");
                        builder.append(line);
                        builder.append(")");
                        this.fullInfo = builder.toString();
                    }
                    break;
                }
                prevClass = thisClass;
            }
        }
        if (declaringClass != null && methodName != null) {
            this.stackTraceElement = new StackTraceElement(declaringClass, methodName, file, Integers.parseInt(line));
            this.fullInfo = stackTraceElement.toString();
        } else {
            this.stackTraceElement = null;
            this.fullInfo = null;
        }
    }

    /**
     * Gets the fully qualified class name of the caller making the logging request.
     */
    public String getClassName() {
        return stackTraceElement != null ? stackTraceElement.getClassName() : NA;
    }

    /**
     * Gets the file name of the caller.
     */
    public String getFileName() {
        return stackTraceElement != null ? stackTraceElement.getFileName() : NA;
    }

    /**
     * Gets the line number of the caller.
     */
    public String getLineNumber() {
        return stackTraceElement != null ? Integer.toString(stackTraceElement.getLineNumber()) : NA;
    }

    /**
     * Gets the method name of the caller.
     */
    public String getMethodName() {
        return stackTraceElement != null ? stackTraceElement.getMethodName() : NA;
    }
}
