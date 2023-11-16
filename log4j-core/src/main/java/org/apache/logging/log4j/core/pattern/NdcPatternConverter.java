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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Returns the event's NDC in a StringBuilder.
 */
@Plugin(name = "NdcPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"x", "NDC"})
public final class NdcPatternConverter extends LogEventPatternConverter {
    /**
     * Singleton.
     */
    private static final NdcPatternConverter INSTANCE = new NdcPatternConverter();

    /**
     * Private constructor.
     */
    private NdcPatternConverter() {
        super("NDC", "ndc");
    }

    /**
     * Obtains an instance of NdcPatternConverter.
     *
     * @param options options, may be null.
     * @return instance of NdcPatternConverter.
     */
    public static NdcPatternConverter newInstance(final String[] options) {
        return INSTANCE;
    }

    @Override
    @PerformanceSensitive("allocation")
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        toAppendTo.append(event.getContextStack());
    }

    @Override
    public String emptyVariableOutput() {
        return "[]";
    }
}
