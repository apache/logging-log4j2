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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
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
import org.apache.logging.log4j.core.config.plugins.PluginElement;
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
            throw new IllegalArgumentException("The 'regex' attribute must not be null or empty.");
        }

        this.useRawMessage = Boolean.TRUE.equals(builder.useRawMsg);

        try {
            this.pattern = Pattern.compile(builder.regex);
        } catch (final Exception ex) {
            throw new IllegalArgumentException("Unable to compile regular expression: '" + builder.regex + "'.", ex);
        }
    }

    /**
     * Returns the compiled regular-expression pattern.
     * @return the pattern (will never be {@code null}
     */
    public Pattern getPattern() {
        return this.pattern;
    }

    /**
     * Returns the regular-expression.
     * @return the regular-expression (it may be an empty string but never {@code null})
     */
    public String getRegex() {
        return this.pattern.pattern();
    }

    /**
     * Returns whether the raw-message should be used.
     * @return {@code true} if the raw message should be used; otherwise, {@code false}
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
        Objects.requireNonNull(event, "The 'event' argument must not be null.");
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
    public Result filter(final @Nullable String msg) {
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
        return "useRawMessage=" + useRawMessage + ", pattern=" + pattern.toString();
    }

    /**
     * Creates a new builder instance.
     * @return the new builder instance
     */
    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A {@link RegexFilter} builder instance.
     */
    public static final class Builder extends AbstractFilterBuilder<RegexFilter.Builder>
            implements org.apache.logging.log4j.core.util.Builder<RegexFilter> {

        /* NOTE: LOG4J-3086 - No patternFlags in builder - this functionality has been deprecated/removed. */

        /**
         * The regular expression to match.
         */
        @PluginBuilderAttribute
        @Required(message = "No 'regex' provided for RegexFilter")
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
            this.regex = Assert.requireNonEmpty(regex, "The 'regex' attribute must not be null or empty.");
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

        /**
         * Builds and returns a {@link RegexFilter} instance configured by this builder.
         *
         * @return the created {@link RegexFilter} or {@code null} if the builder is misconfigured
         */
        @Override
        public @Nullable RegexFilter build() {

            // validate the "regex" attribute
            if (Strings.isEmpty(this.regex)) {
                LOGGER.error("Unable to create RegexFilter: The 'regex' attribute be set to a non-empty String.");
                return null;
            }

            // build with *safety* to not throw exceptions
            try {
                return new RegexFilter(this);
            } catch (final Exception ex) {
                LOGGER.error("Unable to create RegexFilter. {}", ex.getMessage(), ex);
                return null;
            }
        }
    }

    /*
     * DEPRECATIONS:
     * The constructor/fields/methods below have been deprecated.
     * - the 'create***' factory methods should no longer be used - use the builder instead
     * - pattern-flags should now be passed via the regular expression itself
     */

    /**
     * @deprecated pattern flags have been deprecated - they can just be included in the regex-expression.
     */
    @Deprecated
    private static final int DEFAULT_PATTERN_FLAGS = 0;

    /**
     * @deprecated - pattern flags no longer supported.
     */
    @Deprecated
    private String[] patternFlags = new String[0];

    /**
     * @deprecated use {@link RegexFilter.Builder} instead
     */
    @Deprecated
    @SuppressWarnings("MagicConstant")
    private RegexFilter(
            final String regex,
            final boolean useRawMessage,
            final @Nullable String @Nullable [] patternFlags,
            final @Nullable Result onMatch,
            final @Nullable Result onMismatch) {
        super(onMatch, onMismatch);
        Objects.requireNonNull(regex, "The 'regex' argument must be provided for RegexFilter");
        this.patternFlags = patternFlags == null ? new String[0] : patternFlags.clone();
        try {
            int flags = toPatternFlags(this.patternFlags);
            this.pattern = Pattern.compile(regex, flags);
        } catch (final Exception ex) {
            throw new IllegalArgumentException("Unable to compile regular expression: '" + regex + "'.", ex);
        }
        this.useRawMessage = useRawMessage;
    }

    /**
     * Returns the pattern-flags applied to the regular-expression when compiling the pattern.
     *
     * @return the pattern-flags (maybe empty but never {@code null}
     * @deprecated pattern-flags are no longer supported
     */
    @Deprecated
    public String[] getPatternFlags() {
        return this.patternFlags.clone();
    }

    /**
     * Creates a Filter that matches a regular expression.
     *
     * @param regex        The regular expression to match.
     * @param patternFlags An array of Strings where each String is a {@link Pattern#compile(String, int)} compilation flag.
     *                     (no longer used - pattern flags can be embedded in regex-expression.
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
            // @formatter:off
            @PluginAttribute("regex") final String regex,
            @PluginElement("PatternFlags") final String @Nullable [] patternFlags,
            @PluginAttribute("useRawMsg") final @Nullable Boolean useRawMsg,
            @PluginAttribute("onMatch") final @Nullable Result match,
            @PluginAttribute("onMismatch") final @Nullable Result mismatch)
            // @formatter:on
            throws IllegalArgumentException, IllegalAccessException {

        // LOG4J-3086 - pattern-flags can be embedded in RegEx expression
        Objects.requireNonNull(regex, "The 'regex' argument must not be null.");

        return new RegexFilter(regex, Boolean.TRUE.equals(useRawMsg), patternFlags, match, mismatch);
    }

    /** @deprecated pattern flags have been deprecated - they can just be included in the regex-expression. */
    @Deprecated
    private static int toPatternFlags(final String @Nullable [] patternFlags)
            throws IllegalArgumentException, IllegalAccessException {
        if (patternFlags == null || patternFlags.length == 0) {
            return DEFAULT_PATTERN_FLAGS;
        }
        final Field[] fields = Pattern.class.getDeclaredFields();
        final Comparator<Field> comparator = (f1, f2) -> f1.getName().compareTo(f2.getName());
        Arrays.sort(fields, comparator);
        final String[] fieldNames = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            fieldNames[i] = fields[i].getName();
        }
        int flags = DEFAULT_PATTERN_FLAGS;
        for (final String test : patternFlags) {
            final int index = Arrays.binarySearch(fieldNames, test);
            if (index >= 0) {
                final Field field = fields[index];
                flags |= field.getInt(Pattern.class);
            }
        }
        return flags;
    }
}
