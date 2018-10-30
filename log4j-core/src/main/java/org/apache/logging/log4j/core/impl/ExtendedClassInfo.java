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

import org.apache.logging.log4j.core.pattern.PlainTextRenderer;
import org.apache.logging.log4j.core.pattern.TextRenderer;

/**
 * Class and package data used with a {@link StackTraceElement} in a {@link ExtendedStackTraceElement}.
 */
public final class ExtendedClassInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean exact;

    private final String location;

    private final String version;

    /**
     * Constructs a new instance.
     * 
     * @param exact
     * @param location
     * @param version
     */
    public ExtendedClassInfo(final boolean exact, final String location, final String version) {
        super();
        this.exact = exact;
        this.location = location;
        this.version = version;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ExtendedClassInfo)) {
            return false;
        }
        final ExtendedClassInfo other = (ExtendedClassInfo) obj;
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
        if (this.version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!this.version.equals(other.version)) {
            return false;
        }
        return true;
    }

    public boolean getExact() {
        return this.exact;
    }

    public String getLocation() {
        return this.location;
    }

    public String getVersion() {
        return this.version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.exact ? 1231 : 1237);
        result = prime * result + ((this.location == null) ? 0 : this.location.hashCode());
        result = prime * result + ((this.version == null) ? 0 : this.version.hashCode());
        return result;
    }

    public void renderOn(final StringBuilder output, final TextRenderer textRenderer) {
        if (!this.exact) {
            textRenderer.render("~", output, "ExtraClassInfo.Inexact");
        }
        textRenderer.render("[", output, "ExtraClassInfo.Container");
        textRenderer.render(this.location, output, "ExtraClassInfo.Location");
        textRenderer.render(":", output, "ExtraClassInfo.ContainerSeparator");
        textRenderer.render(this.version, output, "ExtraClassInfo.Version");
        textRenderer.render("]", output, "ExtraClassInfo.Container");
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        renderOn(sb, PlainTextRenderer.getInstance());
        return sb.toString();
    }

}