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
package org.apache.logging.log4j.tojul;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.logging.log4j.util.StackLocatorUtil;

/**
 * Extension of {@link java.util.logging.LogRecord} with lazy get source related methods based on Log4j's {@link StackLocatorUtil#calcLocation(String)}.
 */
final class LazyLog4jLogRecord extends LogRecord {

    private static final long serialVersionUID = 6798134264543826471L;

    // parent class LogRecord already has a needToInferCaller but it's private
    private transient boolean inferCaller = true;

    private final String fqcn;

    LazyLog4jLogRecord(final String fqcn, final Level level, final String msg) {
        super(level, msg);
        this.fqcn = fqcn;
    }

    @Override
    public String getSourceClassName() {
        if (inferCaller) {
            inferCaller();
        }
        return super.getSourceClassName();
    }

    @Override
    public String getSourceMethodName() {
        if (inferCaller) {
            inferCaller();
        }
        return super.getSourceMethodName();
    }

    private void inferCaller() {
        StackTraceElement location = null;
        if (fqcn != null) {
            location = StackLocatorUtil.calcLocation(fqcn);
        }
        if (location != null) {
            setSourceClassName(location.getClassName());
            setSourceMethodName(location.getMethodName());
        } else {
            setSourceClassName(null);
            setSourceMethodName(null);
        }
        inferCaller = false;
    }
}
