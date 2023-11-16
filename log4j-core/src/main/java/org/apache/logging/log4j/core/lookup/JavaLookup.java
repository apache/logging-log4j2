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
package org.apache.logging.log4j.core.lookup;

import java.util.Locale;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.util.Strings;

/**
 * Looks up keys related to Java: Java version, JRE version, VM version, and so on.
 */
@Plugin(name = "java", category = StrLookup.CATEGORY)
public class JavaLookup extends AbstractLookup {

    private final SystemPropertiesLookup spLookup = new SystemPropertiesLookup();

    /**
     * Accessible through the Lookup key {@code hw}.
     * @return hardware processor information.
     */
    public String getHardware() {
        return "processors: " + Runtime.getRuntime().availableProcessors() + ", architecture: "
                + getSystemProperty("os.arch") + this.getSystemProperty("-", "sun.arch.data.model")
                + this.getSystemProperty(", instruction sets: ", "sun.cpu.isalist");
    }

    /**
     * Accessible through the Lookup key {@code locale}.
     * @return system locale and file encoding information.
     */
    public String getLocale() {
        return "default locale: " + Locale.getDefault() + ", platform encoding: " + getSystemProperty("file.encoding");
    }

    /**
     * Accessible through the Lookup key {@code os}.
     * @return operating system information.
     */
    public String getOperatingSystem() {
        return getSystemProperty("os.name") + " " + getSystemProperty("os.version")
                + getSystemProperty(" ", "sun.os.patch.level") + ", architecture: " + getSystemProperty("os.arch")
                + getSystemProperty("-", "sun.arch.data.model");
    }

    /**
     * Accessible through the Lookup key {@code runtime}.
     * @return Java Runtime Environment information.
     */
    public String getRuntime() {
        return getSystemProperty("java.runtime.name") + " (build " + getSystemProperty("java.runtime.version")
                + ") from " + getSystemProperty("java.vendor");
    }

    private String getSystemProperty(final String name) {
        return spLookup.lookup(name);
    }

    private String getSystemProperty(final String prefix, final String name) {
        final String value = getSystemProperty(name);
        if (Strings.isEmpty(value)) {
            return Strings.EMPTY;
        }
        return prefix + value;
    }

    /**
     * Accessible through the Lookup key {@code vm}.
     * @return Java Virtual Machine information.
     */
    public String getVirtualMachine() {
        return getSystemProperty("java.vm.name") + " (build " + getSystemProperty("java.vm.version") + ", "
                + getSystemProperty("java.vm.info") + ")";
    }

    /**
     * Looks up the value of the environment variable.
     *
     * @param event
     *        The current LogEvent (is ignored by this StrLookup).
     * @param key
     *        the key to be looked up, may be null
     * @return The value of the environment variable.
     */
    @Override
    public String lookup(final LogEvent event, final String key) {
        switch (key) {
            case "version":
                return "Java version " + getSystemProperty("java.version");
            case "runtime":
                return getRuntime();
            case "vm":
                return getVirtualMachine();
            case "os":
                return getOperatingSystem();
            case "hw":
                return getHardware();
            case "locale":
                return getLocale();
            default:
                throw new IllegalArgumentException(key);
        }
    }
}
