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
package org.apache.logging.log4j.core.config.builder.api;

import org.apache.logging.log4j.core.script.ScriptFile;
import org.jspecify.annotations.Nullable;

/**
 * A builder interface for constructing and configuring {@link ScriptFile} components in a Log4j configuration.
 *
 * <p>
 *   Instances of this builder are designed for single-threaded use and are not thread-safe. Developers
 *   should avoid sharing instances between threads.
 * </p>
 *
 * @since 2.5
 */
public interface ScriptFileComponentBuilder extends ComponentBuilder<ScriptFileComponentBuilder> {

    /**
     * Sets the '{@code language}' attribute for the script-file component.
     * <p>
     *   If the {@code language} argument is {@code null} the attribute will be removed.
     * </p>
     * @param language the language
     * @return this builder (for chaining)
     */
    default ScriptFileComponentBuilder setLanguageAttribute(@Nullable String language) {
        return setAttribute("language", language);
    }

    /**
     * Sets the '{@code isWatched}' attribute for the script-file component.
     * @param isWatched {@code true} to watch the script file; otherwise, {@code false}
     * @return this builder (for chaining)
     */
    default ScriptFileComponentBuilder setIsWatchedAttribute(boolean isWatched) {
        return setAttribute("isWatched", isWatched);
    }

    /**
     * Sets the '{@code isWatched}' attribute for the script-file component.
     * <p>
     *   If the {@code isWatched} argument is {@code null} the attribute will be removed.
     * </p>
     * @param isWatched the string value of the flag
     * @return this builder (for chaining)
     */
    default ScriptFileComponentBuilder setIsWatchedAttribute(@Nullable String isWatched) {
        return setAttribute("isWatched", isWatched);
    }

    /**
     * Sets the '{@code charset}' attribute for the script-file component.
     * <p>
     *   If the {@code charset} argument is {@code null} the attribute will be removed.
     * </p>
     * @param charset the charset
     * @return this builder (for chaining)
     */
    default ScriptFileComponentBuilder setCharsetAttribute(@Nullable String charset) {
        return setAttribute("charset", charset);
    }

    /**
     * Sets the '{@code path}' attribute for the script-file component.
     * <p>
     *   If the {@code path} argument is {@code null} the attribute will be removed.
     * </p>
     * @param path the script file path
     * @return this builder (for chaining)
     */
    default ScriptFileComponentBuilder setPathAttribute(@Nullable String path) {
        return setAttribute("path", path);
    }

    /**
     * Adds the '{@code language}' attribute for the script-file component.
     * <p>
     *   If the {@code language} argument is {@code null} the attribute will be removed.
     * </p>
     * @param language the language
     * @return this builder (for chaining)
     * @deprecated use {@link #setLanguageAttribute(String)}
     */
    @Deprecated
    default ScriptFileComponentBuilder addLanguage(@Nullable String language) {
        return setLanguageAttribute(language);
    }

    /**
     * Adds the '{@code isWatched}' attribute for the script-file component.
     * <p>
     *   If the {@code isWatched} argument is {@code null} the attribute will be removed.
     * </p>
     * @param isWatched the string value of the flag
     * @return this builder (for chaining)
     * @deprecated use {@link #setIsWatchedAttribute(boolean)} (String)}
     */
    @Deprecated
    default ScriptFileComponentBuilder addIsWatched(boolean isWatched) {
        return setIsWatchedAttribute(isWatched);
    }

    /**
     * Adds the '{@code isWatched}' attribute for the script-file component.
     * <p>
     *   If the {@code isWatched} argument is {@code null} the attribute will be removed.
     * </p>
     * @param isWatched the string value of the flag
     * @return this builder (for chaining)
     * @deprecated use {@link #setIsWatchedAttribute(String)}
     */
    @Deprecated
    default ScriptFileComponentBuilder addIsWatched(String isWatched) {
        return setIsWatchedAttribute(isWatched);
    }

    /**
     * Sets the '{@code charset}' attribute for the script-file component.
     * <p>
     *   If the {@code charset} argument is {@code null} the attribute will be removed.
     * </p>
     * @param charset the charset
     * @return this builder (for chaining)
     * @deprecated use {@link #setCharsetAttribute(String)}
     */
    @Deprecated
    default ScriptFileComponentBuilder addCharset(String charset) {
        return setCharsetAttribute(charset);
    }
}
