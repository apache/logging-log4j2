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
package org.apache.logging.log4j.core.impl;

import java.io.Serializable;
import java.util.Objects;
import org.apache.logging.log4j.core.pattern.PlainTextRenderer;
import org.apache.logging.log4j.core.pattern.TextRenderer;

/**
 * Wraps and extends the concept of the JRE's final class {@link StackTraceElement} by adding more location information.
 * <p>
 * Complements a StackTraceElement with:
 * </p>
 * <ul>
 * <li>exact: whether the class was obtained via {@code sun.reflect.Reflection#getCallerClass(int)}</li>
 * <li>location: a classpath element or a jar</li>
 * <li>version</li>
 * </ul>
 */
public final class ExtendedStackTraceElement implements Serializable {

    static final ExtendedStackTraceElement[] EMPTY_ARRAY = {};

    private static final long serialVersionUID = -2171069569241280505L;

    private final ExtendedClassInfo extraClassInfo;

    private final StackTraceElement stackTraceElement;

    public ExtendedStackTraceElement(
            final StackTraceElement stackTraceElement, final ExtendedClassInfo extraClassInfo) {
        this.stackTraceElement = stackTraceElement;
        this.extraClassInfo = extraClassInfo;
    }

    /**
     * Called from Jackson for XML and JSON IO.
     */
    public ExtendedStackTraceElement(
            final String declaringClass,
            final String methodName,
            final String fileName,
            final int lineNumber,
            final boolean exact,
            final String location,
            final String version) {
        this(
                new StackTraceElement(declaringClass, methodName, fileName, lineNumber),
                new ExtendedClassInfo(exact, location, version));
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ExtendedStackTraceElement)) {
            return false;
        }
        final ExtendedStackTraceElement other = (ExtendedStackTraceElement) obj;
        if (!Objects.equals(this.extraClassInfo, other.extraClassInfo)) {
            return false;
        }
        if (!Objects.equals(this.stackTraceElement, other.stackTraceElement)) {
            return false;
        }
        return true;
    }

    public String getClassName() {
        return this.stackTraceElement.getClassName();
    }

    public boolean getExact() {
        return this.extraClassInfo.getExact();
    }

    public ExtendedClassInfo getExtraClassInfo() {
        return this.extraClassInfo;
    }

    public String getFileName() {
        return this.stackTraceElement.getFileName();
    }

    public int getLineNumber() {
        return this.stackTraceElement.getLineNumber();
    }

    public String getLocation() {
        return this.extraClassInfo.getLocation();
    }

    public String getMethodName() {
        return this.stackTraceElement.getMethodName();
    }

    public StackTraceElement getStackTraceElement() {
        return this.stackTraceElement;
    }

    public String getVersion() {
        return this.extraClassInfo.getVersion();
    }

    @Override
    public int hashCode() {
        return Objects.hash(extraClassInfo, stackTraceElement);
    }

    public boolean isNativeMethod() {
        return this.stackTraceElement.isNativeMethod();
    }

    void renderOn(final StringBuilder output, final TextRenderer textRenderer) {
        render(this.stackTraceElement, output, textRenderer);
        textRenderer.render(" ", output, "Text");
        this.extraClassInfo.renderOn(output, textRenderer);
    }

    private void render(
            final StackTraceElement stElement, final StringBuilder output, final TextRenderer textRenderer) {
        final String fileName = stElement.getFileName();
        final int lineNumber = stElement.getLineNumber();
        textRenderer.render(getClassName(), output, "StackTraceElement.ClassName");
        textRenderer.render(".", output, "StackTraceElement.ClassMethodSeparator");
        textRenderer.render(stElement.getMethodName(), output, "StackTraceElement.MethodName");
        if (stElement.isNativeMethod()) {
            textRenderer.render("(Native Method)", output, "StackTraceElement.NativeMethod");
        } else if (fileName != null && lineNumber >= 0) {
            textRenderer.render("(", output, "StackTraceElement.Container");
            textRenderer.render(fileName, output, "StackTraceElement.FileName");
            textRenderer.render(":", output, "StackTraceElement.ContainerSeparator");
            textRenderer.render(Integer.toString(lineNumber), output, "StackTraceElement.LineNumber");
            textRenderer.render(")", output, "StackTraceElement.Container");
        } else if (fileName != null) {
            textRenderer.render("(", output, "StackTraceElement.Container");
            textRenderer.render(fileName, output, "StackTraceElement.FileName");
            textRenderer.render(")", output, "StackTraceElement.Container");
        } else {
            textRenderer.render("(", output, "StackTraceElement.Container");
            textRenderer.render("Unknown Source", output, "StackTraceElement.UnknownSource");
            textRenderer.render(")", output, "StackTraceElement.Container");
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        renderOn(sb, PlainTextRenderer.getInstance());
        return sb.toString();
    }
}
