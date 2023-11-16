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
package org.apache.logging.log4j.core.pattern;

import java.util.Date;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Formats an integer.
 */
@Plugin(name = "IntegerPatternConverter", category = "FileConverter")
@ConverterKeys({"i", "index"})
@PerformanceSensitive("allocation")
public final class IntegerPatternConverter extends AbstractPatternConverter implements ArrayPatternConverter {

    /**
     * Singleton.
     */
    private static final IntegerPatternConverter INSTANCE = new IntegerPatternConverter();

    /**
     * Private constructor.
     */
    private IntegerPatternConverter() {
        super("Integer", "integer");
    }

    /**
     * Obtains an instance of pattern converter.
     *
     * @param options options, may be null.
     * @return instance of pattern converter.
     */
    public static IntegerPatternConverter newInstance(final String[] options) {
        return INSTANCE;
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
            toAppendTo.append(((Integer) obj).intValue());
        } else if (obj instanceof Date) {
            toAppendTo.append(((Date) obj).getTime());
        }
    }
}
