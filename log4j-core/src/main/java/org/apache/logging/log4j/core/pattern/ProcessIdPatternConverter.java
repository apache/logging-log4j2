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
import org.apache.logging.log4j.util.ProcessIdUtil;

@Plugin(name = "ProcessIdPatternConverter", category = "Converter")
@ConverterKeys({"pid", "processId"})
public final class ProcessIdPatternConverter extends LogEventPatternConverter {
    private static final String DEFAULT_DEFAULT_VALUE = "???";
    private final String pid;

    /**
     * Private constructor.
     */
    private ProcessIdPatternConverter(final String... options) {
        super("Process ID", "pid");
        final String defaultValue = options.length > 0 ? options[0] : DEFAULT_DEFAULT_VALUE;
        final String discoveredPid = ProcessIdUtil.getProcessId();
        pid = discoveredPid.equals(ProcessIdUtil.DEFAULT_PROCESSID) ? defaultValue : discoveredPid;
    }

    /**
     * Returns the process ID.
     * @return the process ID
     */
    public String getProcessId() {
        return pid;
    }

    public static void main(final String[] args) {
        System.out.println(new ProcessIdPatternConverter().pid);
    }

    /**
     * Obtains an instance of ProcessIdPatternConverter.
     *
     * @param options options, currently ignored, may be null.
     * @return instance of ProcessIdPatternConverter.
     */
    public static ProcessIdPatternConverter newInstance(final String[] options) {
        return new ProcessIdPatternConverter(options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        toAppendTo.append(pid);
    }
}
