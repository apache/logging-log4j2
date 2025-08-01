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
package org.apache.logging.log4j.core.script;

import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * Container for the language and body of a script.
 */
public abstract class AbstractScript {

    protected static final Logger LOGGER = StatusLogger.getLogger();
    protected static final String DEFAULT_LANGUAGE = "JavaScript";

    private final String language;
    private final String scriptText;
    private final String name;
    private final String id;

    public AbstractScript(final String name, final String language, final String scriptText) {
        this.language = language;
        this.scriptText = scriptText;
        this.name = name;
        this.id = Strings.isBlank(name) ? Integer.toHexString(Objects.hashCode(this)) : name;
    }

    public String getLanguage() {
        return this.language;
    }

    public String getScriptText() {
        return this.scriptText;
    }

    public String getName() {
        return this.name;
    }

    public String getId() {
        return this.id;
    }
}
