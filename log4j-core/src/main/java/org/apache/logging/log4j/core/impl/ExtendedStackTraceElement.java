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
package org.apache.logging.log4j.core.impl;

import java.io.Serializable;

import org.apache.logging.log4j.util.Strings;

import sun.reflect.Reflection;

/**
 * Wraps and extends the concept of the JRE's final class {@link StackTraceElement} by adding more location information.
 * <p>
 * Complements a StackTraceElement with:
 * <ul>
 * <li>exact: whether the class was obtained via {@link Reflection#getCallerClass()}</li>
 * <li>location: a classpath element or a jar</li>
 * <li>version</li>
 * </ul>
 * </p>
 */
public class ExtendedStackTraceElement implements Serializable {

    private static final long serialVersionUID = -2171069569241280505L;

    private final boolean exact;

    private final String location;

    private final StackTraceElement stackTraceElement;

    private final String version;

    /**
     * Constructor that takes the location, version, and exact match flag.
     * 
     * @param stackTraceElement TODO
     * @param exact if true this is an exact package element.
     * @param location The location of the Class.
     * @param version The version of the component.
     */
    public ExtendedStackTraceElement(final StackTraceElement stackTraceElement, final boolean exact, final String location,
            final String version) {
        this.stackTraceElement = stackTraceElement;
        this.location = location;
        this.version = version;
        this.exact = exact;
    }

    public ExtendedStackTraceElement(final String declaringClass, final String methodName, final String fileName, final int lineNumber,
            final boolean exact, final String location, final String version) {
        this(new StackTraceElement(declaringClass, methodName, fileName, lineNumber), exact, location, version);
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
        if (this.exact != other.exact) {
            return false;
        }
        if (this.location == null) {
            if (other.location != null) {
                return false;
            }
        } else if (!this.location.equals(other.location)) {
            return false;
        }
        if (this.stackTraceElement == null) {
            if (other.stackTraceElement != null) {
                return false;
            }
        } else if (!this.stackTraceElement.equals(other.stackTraceElement)) {
            return false;
        }
        if (this.version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!this.version.equals(other.version)) {
            return false;
        }
        return true;
    }

    /**
     * Returns the indicator of whether this is an exact match.
     * 
     * @return true if the location was determined exactly.
     */
    public boolean getExact() {
        return this.exact;
    }

    /**
     * Returns the location of the element.
     * 
     * @return The location of the element.
     */
    public String getLocation() {
        return this.location;
    }

    public StackTraceElement getStackTraceElement() {
        return this.stackTraceElement;
    }

    /**
     * Returns the version of the element.
     * 
     * @return the version of the element.
     */
    public String getVersion() {
        return this.version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.exact ? 1231 : 1237);
        result = prime * result + (this.location == null ? 0 : this.location.hashCode());
        result = prime * result + (this.version == null ? 0 : this.version.hashCode());
        result = prime * result + (this.stackTraceElement == null ? 0 : this.stackTraceElement.hashCode());
        return result;
    }

    @Override
    public String toString() {
        final String exactStr = this.exact ? Strings.EMPTY : "~";
        return exactStr + '[' + this.location + ':' + this.version + ']';
    }

    public String getFileName() {
        return this.stackTraceElement.getFileName();
    }

    public int getLineNumber() {
        return this.stackTraceElement.getLineNumber();
    }

    public String getClassName() {
        return this.stackTraceElement.getClassName();
    }

    public String getMethodName() {
        return this.stackTraceElement.getMethodName();
    }

    public boolean isNativeMethod() {
        return this.stackTraceElement.isNativeMethod();
    }
}
