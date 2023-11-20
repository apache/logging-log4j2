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
package org.apache.logging.log4j.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public enum ExtensionLanguageMapping {
    JS("js", "JavaScript"),
    JAVASCRIPT("javascript", "JavaScript"),
    GVY("gvy", "Groovy"),
    GROOVY("groovy", "Groovy"),
    BSH("bsh", "beanshell"),
    BEANSHELL("beanshell", "beanshell"),
    JY("jy", "jython"),
    JYTHON("jython", "jython"),
    FTL("ftl", "freemarker"),
    FREEMARKER("freemarker", "freemarker"),
    VM("vm", "velocity"),
    VELOCITY("velocity", "velocity"),
    AWK("awk", "awk"),
    EJS("ejs", "ejs"),
    TCL("tcl", "tcl"),
    HS("hs", "jaskell"),
    JELLY("jelly", "jelly"),
    JEP("jep", "jep"),
    JEXL("jexl", "jexl"),
    JEXL2("jexl2", "jexl2"),
    RB("rb", "ruby"),
    RUBY("ruby", "ruby"),
    JUDO("judo", "judo"),
    JUDI("judi", "judo"),
    SCALA("scala", "scala"),
    CLJ("clj", "Clojure");

    private final String extension;
    private final String language;

    ExtensionLanguageMapping(final String extension, final String language) {
        this.extension = extension;
        this.language = language;
    }

    public String getExtension() {
        return this.extension;
    }

    public String getLanguage() {
        return this.language;
    }

    public static ExtensionLanguageMapping getByExtension(final String extension) {
        for (final ExtensionLanguageMapping mapping : values()) {
            if (mapping.extension.equals(extension)) {
                return mapping;
            }
        }
        return null;
    }

    public static List<ExtensionLanguageMapping> getByLanguage(final String language) {
        final List<ExtensionLanguageMapping> list = new ArrayList<>();
        for (final ExtensionLanguageMapping mapping : values()) {
            if (mapping.language.equals(language)) {
                list.add(mapping);
            }
        }
        return list;
    }
}
