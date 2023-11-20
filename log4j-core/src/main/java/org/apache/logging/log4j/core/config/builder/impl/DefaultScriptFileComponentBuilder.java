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
package org.apache.logging.log4j.core.config.builder.impl;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.ScriptFileComponentBuilder;

/**
 * Creates a ScriptFile ComponentBuilder.
 *
 * @since 2.5
 */
class DefaultScriptFileComponentBuilder extends DefaultComponentAndConfigurationBuilder<ScriptFileComponentBuilder>
        implements ScriptFileComponentBuilder {

    public DefaultScriptFileComponentBuilder(
            final DefaultConfigurationBuilder<? extends Configuration> builder, final String name, final String path) {
        super(builder, name != null ? name : path, "ScriptFile");
        addAttribute("path", path);
    }

    @Override
    public DefaultScriptFileComponentBuilder addLanguage(final String language) {
        addAttribute("language", language);
        return this;
    }

    @Override
    public DefaultScriptFileComponentBuilder addIsWatched(final boolean isWatched) {
        addAttribute("isWatched", Boolean.toString(isWatched));
        return this;
    }

    @Override
    public DefaultScriptFileComponentBuilder addIsWatched(final String isWatched) {
        addAttribute("isWatched", isWatched);
        return this;
    }

    @Override
    public DefaultScriptFileComponentBuilder addCharset(final String charset) {
        addAttribute("charset", charset);
        return this;
    }
}
