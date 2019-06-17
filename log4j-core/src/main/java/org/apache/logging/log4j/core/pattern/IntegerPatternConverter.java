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
package org.apache.logging.log4j.core.pattern;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Formats an integer.
 */
@Plugin(name = "IntegerPatternConverter", category = "FileConverter")
@ConverterKeys({ "i", "index" })
@PerformanceSensitive("allocation")
public final class IntegerPatternConverter extends AbstractPatternConverter implements ArrayPatternConverter {

    /** A pattern specifying the valid options of an IntegerPattern. */
    protected static final Pattern PATTERN_OPTIONS= Pattern.compile(""
        + "^"
        + "(?<PADDING>[^1-9])?"       // optional padding character
        + "(?<LENGTH>[1-9][0-9]*)"    // length to pad to
        + "$");


    /**
     * Singleton.
     */
    private static final IntegerPatternConverter DEFAULT_INSTANCE = new IntegerPatternConverter();

    /** An (optional) padding. */
    private PaddingSpec padding;

    /**
     * Private constructor.
     */
    private IntegerPatternConverter() {
        super("Integer", "integer");
    }

    /**
     * Private constructor.
     */
    private IntegerPatternConverter(final PaddingSpec padding) {
        super("Integer", "integer");
        this.padding= padding;
    }

    /**
     * Obtains an instance of pattern converter.
     *
     * @param options options, may be null.
     * @return instance of pattern converter.
     */
    public static IntegerPatternConverter newInstance(final String[] options) {
        if (options == null || options.length == 0) {
            return DEFAULT_INSTANCE;
        } else if (options.length > 1){
            LOGGER.error("At max 1 option is allowed {}");
            return DEFAULT_INSTANCE;
        } else {
            final String option= options[0];
            final Matcher matcher = PATTERN_OPTIONS.matcher(option);

            if (matcher.matches()) {
                final int length= Integer.parseInt(matcher.group("LENGTH"));
                if (matcher.group("PADDING") != null) {
                    return new IntegerPatternConverter(new PaddingSpec(matcher.group("PADDING").charAt(0), length));
                } else {
                    return new IntegerPatternConverter(new PaddingSpec('0', length));
                }
            } else {
                LOGGER.error("Invalid option {} will be ignored", option);
                return DEFAULT_INSTANCE;
            }
        }
    }

    @Override
    public void format(final StringBuilder toAppendTo, final Object... objects) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] instanceof Integer) {
                format(objects[i], toAppendTo);
                break;
            } else if (objects[i] instanceof NotANumber) {
                toAppendTo.append(NotANumber.VALUE);
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final Object obj, final StringBuilder toAppendTo) {
        if (obj instanceof Integer) {
            if (this.padding != null) {
                toAppendTo.append(this.padding.apply((Integer) obj));
            } else {
                toAppendTo.append(((Integer) obj).intValue());
            }
        } else if (obj instanceof Date) {
            toAppendTo.append(((Date) obj).getTime());
        }
    }
}
