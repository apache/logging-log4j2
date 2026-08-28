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
package org.apache.logging.log4j.core.filter;

import java.util.Objects;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.util.Assert;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFormatMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.util.Strings;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This filter returns the {@code onMatch} result if the message exactly matches the configured
 * "{@code regex}" regular-expression pattern; otherwise, it returns the {@code onMismatch} result.
 */
@Plugin(name = "RegexFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
@NullMarked
public final class RegexFilter extends AbstractFilter {

    /** The pattern compiled from the regular-expression. */
    private final Pattern pattern;

    /** Flag: if {@code true} use message format-pattern / field for the match target. */
    private final boolean useRawMessage;

    /**
     * Constructs a new {@code RegexFilter} configured by the given builder.
     * @param builder the builder
     * @throws IllegalArgumentException if the regular expression is not configured or cannot be compiled to a pattern
     */
    private RegexFilter(final Builder builder) {

        super(builder);

        if (Strings.isBlank(builder.regex)) {
            throw new IllegalArgumentException("The `regex` attribute must not be null or empty.");
        }

        this.useRawMessage = Boolean.TRUE.equals(builder.useRawMsg);

        try {
            this.pattern = Pattern.compile(builder.regex);
        } catch (final Exception ex) {
            throw new IllegalArgumentException("Unable to compile regular expression: `" + builder.regex + "`.", ex);
        }
    }

    /**
     * Returns the compiled regular-expression pattern.
     * @return the pattern (will never be {@code null}
     * @since 2.27.0
     */
    public Pattern getPattern() {
        return this.pattern;
    }

    /**
     * Returns the regular-expression.
     * @return the regular-expression (it may be an empty string but never {@code null})
     * @since 2.27.0
     */
    public String getRegex() {
        return this.pattern.pattern();
    }

    /**
     * Returns whether the raw-message should be used.
     * @return {@code true} if the raw message should be used; otherwise, {@code false}
     * @since 2.27.0
     */
    public boolean isUseRawMessage() {
        return this.useRawMessage;
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against the given message formatted with
     *   the given parameters.
     * </p>
     * <p>
     *   The following method arguments are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code logger}</li>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *   </ul>
     * </p>
     */
    @Override
    public Result filter(
            final @Nullable Logger logger,
            final @Nullable Level level,
            final @Nullable Marker marker,
            final @Nullable String msg,
            final @Nullable Object @Nullable ... params) {

        return (useRawMessage || params == null || params.length == 0)
                ? filter(msg)
                : filter(ParameterizedMessage.format(msg, params));
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against the given message.
     * </p>
     * <p>
     *   The following method arguments are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code logger}</li>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *     <li>{@code throwable}</li>
     *   </ul>
     * </p>
     */
    @Override
    public Result filter(
            final @Nullable Logger logger,
            final @Nullable Level level,
            final @Nullable Marker marker,
            final @Nullable Object message,
            final @Nullable Throwable throwable) {

        return (message == null) ? this.onMismatch : filter(message.toString());
    }

    /**
     * {@inheritDoc}
     * <p>
     *   This implementation performs the filter evaluation against the given message.
     * </p>
     * <p>
     *   The following method arguments are ignored by this filter method implementation:
     *   <ul>
     *     <li>{@code logger}</li>
     *     <li>{@code level}</li>
     *     <li>{@code marker}</li>
     *     <li>{@code throwable}</li>
     *   </ul>
     * </p>
     */
    @Override
    public Result filter(
            final @Nullable Logger logger,
            final @Nullable Level level,
            final @Nullable Marker marker,
            final @Nullable Message message,
            final @Nullable Throwable throwable) {
        return (message == null) ? this.onMismatch : filter(getMessageTextByType(message));
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code event} argument is {@code null}
     */
    @Override
    public Result filter(final LogEvent event) {
        Objects.requireNonNull(event, "event");
        return filter(getMessageTextByType(event.getMessage()));
    }

    /**
     * Apply the filter to the given message and return the {@code onMatch} result if the <i>entire</i>
     * message matches the configured regex pattern; otherwise, {@code onMismatch}.
     * <p>
     *   If the given '{@code msg}' is {@code null} the configured {@code onMismatch} result will be returned.
     * </p>
     * @param msg the message
     * @return the {@code onMatch} result if the pattern matches; otherwise, the {@code onMismatch} result
     */
    Result filter(final @Nullable String msg) {
        return (msg != null && pattern.matcher(msg).matches()) ? onMatch : onMismatch;
    }

    /**
     * Tests the filter pattern against the given Log4j {@code Message}.
     * <p>
     *   If the raw-message flag is enabled and message is an instance of the following, the raw message format
     *   will be returned.
     * </p>
     * <ul>
     *   <li>{@link MessageFormatMessage}</li>
     *   <li>{@link ParameterizedMessage}</li>
     *   <li>{@link StringFormattedMessage}</li>
     *   <li>{@link StructuredDataMessage}</li>
     * </ul>
     * <p>
     *   If the '{@code useRawMessage}' flag is disabled <i>OR</i> the message is not one of the above
     *   implementations, the message's formatted message will be returned.
     * </p>
     * <h3>Developer Note</h3>
     * <p>
     * While `Message#getFormat()` is broken in general, it still makes sense for certain types.
     * Hence, suppress the deprecation warning.
     * </p>
     *
     * @param message the message
     * @return the target message based on configuration and message-type
     */
    @SuppressWarnings("deprecation")
    private String getMessageTextByType(final Message message) {
        return useRawMessage
                        && (message instanceof ParameterizedMessage
                                || message instanceof StringFormattedMessage
                                || message instanceof MessageFormatMessage
                                || message instanceof StructuredDataMessage)
                ? message.getFormat()
                : message.getFormattedMessage();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "useRawMessage=" + useRawMessage + ", pattern=" + pattern;
    }

    /**
     * Creates a new builder instance.
     * @return the new builder instance
     * @since 2.27.0
     */
    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A {@link RegexFilter} builder instance.
     * @since 2.27.0
     */
    public static final class Builder extends AbstractFilterBuilder<RegexFilter.Builder>
            implements org.apache.logging.log4j.core.util.Builder<RegexFilter> {

        /**
         * The regular expression to match.
         */
        @PluginBuilderAttribute
        @Required(message = "No `regex` provided for `RegexFilter`")
        private @Nullable String regex;

        /**
         * If {@code true}, for {@link ParameterizedMessage}, {@link StringFormattedMessage},
         * and {@link MessageFormatMessage}, the message format pattern; for {@link StructuredDataMessage},
         * the message field will be used as the match target.
         */
        @PluginBuilderAttribute
        private @Nullable Boolean useRawMsg;

        /** Private constructor. */
        private Builder() {
            super();
        }

        /**
         * Sets the regular-expression.
         *
         * @param regex the regular-expression
         * @return this builder
         */
        public Builder setRegex(final String regex) {
            this.regex = Assert.requireNonEmpty(regex, "The `regex` attribute must not be null or empty.");
            return this;
        }

        /**
         * Sets the use raw msg flag.
         *
         * @param useRawMsg {@code true} if the message format-patter/field will be used as match target;
         *                  otherwise, {@code false}
         * @return this builder
         */
        public Builder setUseRawMsg(final boolean useRawMsg) {
            this.useRawMsg = useRawMsg;
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isValid() {
            return Strings.isNotEmpty(this.regex);
        }

        /**
         * Builds and returns a {@link RegexFilter} instance configured by this builder.
         *
         * @return the created {@link RegexFilter} or {@code null} if the builder is misconfigured
         */
        @Override
        public RegexFilter build() {

            // validate the "regex" attribute
            if (Strings.isEmpty(this.regex)) {
                throw new IllegalArgumentException(
                        "Unable to create `RegexFilter`: The `regex` attribute be set to a non-empty string.");
            }

            // build with *safety* to not throw exceptions
            try {
                return new RegexFilter(this);
            } catch (final Exception ex) {
                throw new IllegalArgumentException("Unable to create `RegexFilter`.", ex);
            }
        }
    }

    /**
     * @deprecated use {@link RegexFilter.Builder} instead
     */
    @SuppressWarnings("unused")
    @Deprecated
    private RegexFilter(
            final String regex,
            final boolean useRawMessage,
                    // `patternFlags` has never worked, and removed in `2.27.0`.
                    // We're keeping this field for binary backward compatibility.
                    final @Nullable String @Nullable [] patternFlags,
            final @Nullable Result onMatch,
            final @Nullable Result onMismatch) {
        super(onMatch, onMismatch);
        Objects.requireNonNull(regex, "regex");
        this.pattern = Pattern.compile(regex);
        this.useRawMessage = useRawMessage;
    }

    /**
     * Creates a Filter that matches a regular expression.
     *
     * @param regex        The regular expression to match.
     * @param patternFlags Ignored, kept for backward compatibility.
     * @param useRawMsg    If {@code true}, for {@link ParameterizedMessage}, {@link StringFormattedMessage},
     *                     and {@link MessageFormatMessage}, the message format pattern; for {@link StructuredDataMessage},
     *                     the message field will be used as the match target.
     * @param match        The action to perform when a match occurs.
     * @param mismatch     The action to perform when a mismatch occurs.
     * @return The RegexFilter.
     * @throws IllegalAccessException   When there is no access to the definition of the specified member.
     * @throws IllegalArgumentException When passed an illegal or inappropriate argument.
     * @deprecated use {@link #newBuilder} to instantiate builder
     */
    @Deprecated
    public static RegexFilter createFilter(
            @PluginAttribute("regex") final String regex,
            // `patternFlags` has never worked, and removed in `2.27.0`.
            // We're keeping this field for binary backward compatibility.
            final String @Nullable [] patternFlags,
            @PluginAttribute("useRawMsg") final @Nullable Boolean useRawMsg,
            @PluginAttribute("onMatch") final @Nullable Result match,
            @PluginAttribute("onMismatch") final @Nullable Result mismatch)
            throws IllegalArgumentException, IllegalAccessException {
        Objects.requireNonNull(regex, "regex");
        return new RegexFilter(regex, Boolean.TRUE.equals(useRawMsg), patternFlags, match, mismatch);
    }
}
