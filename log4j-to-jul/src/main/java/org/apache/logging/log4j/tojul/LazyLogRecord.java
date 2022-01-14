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
package org.apache.logging.log4j.tojul;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.logging.log4j.util.StackLocatorUtil;

/**
 * Extension of {@link java.util.logging.LogRecord} with lazy get source related methods.
 *
 * @author <a href="http://www.vorburger.ch">Michael Vorburger.ch</a> for Google
 */
/* package-local */ final class LazyLogRecord extends LogRecord {

    private static final long serialVersionUID = 6798134264543826471L;

    // parent class LogRecord already has a needToInferCaller but it's private
    private transient boolean stillNeedToInferCaller = true;

    private final String fqcn;

    LazyLogRecord(String fqcn, Level level, String msg) {
        super(level, msg);
        this.fqcn = fqcn;
    }

    @Override
    public String getSourceClassName() {
        if (stillNeedToInferCaller) {
            inferCaller();
        }
        return super.getSourceClassName();
    }

    @Override
    public String getSourceMethodName() {
        if (stillNeedToInferCaller) {
            inferCaller();
        }
        return super.getSourceMethodName();
    }

    private void inferCaller() {
        StackTraceElement location = StackLocatorUtil.calcLocation(fqcn);
        setSourceClassName(location.getClassName());
        setSourceMethodName(location.getMethodName());
        stillNeedToInferCaller = false;
    }
}