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
package org.apache.logging.slf4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.internal.DefaultLogBuilder;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.ExtendedLogger;

public class SLF4JLogBuilder extends DefaultLogBuilder {

    public SLF4JLogBuilder(ExtendedLogger logger, Level level) {
        super(logger, level);
    }

    public SLF4JLogBuilder() {
        super();
    }

    @Override
    protected boolean isEnabled(Message message) {
        // SLF4J will check again later
        return true;
    }

    @Override
    protected boolean isEnabled(CharSequence message) {
        // SLF4J will check again later
        return true;
    }

    @Override
    protected boolean isEnabled(String message) {
        // SLF4J will check again later
        return true;
    }

    @Override
    protected boolean isEnabled(String message, Object... params) {
        // SLF4J will check again later
        return true;
    }

    @Override
    protected boolean isEnabled(Object message) {
        // SLF4J will check again later
        return true;
    }

    @Override
    protected boolean isEnabled(String message, Object p0) {
        // SLF4J will check again later
        return true;
    }

    @Override
    protected boolean isEnabled(String message, Object p0, Object p1) {
        // SLF4J will check again later
        return true;
    }

    @Override
    protected boolean isEnabled(String message, Object p0, Object p1, Object p2) {
        // SLF4J will check again later
        return true;
    }

    @Override
    protected boolean isEnabled(String message, Object p0, Object p1, Object p2, Object p3) {
        // SLF4J will check again later
        return true;
    }

    @Override
    protected boolean isEnabled(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
        // SLF4J will check again later
        return true;
    }

    @Override
    protected boolean isEnabled(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        // SLF4J will check again later
        return true;
    }

    @Override
    protected boolean isEnabled(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6) {
        // SLF4J will check again later
        return true;
    }

    @Override
    protected boolean isEnabled(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6, Object p7) {
        // SLF4J will check again later
        return true;
    }

    @Override
    protected boolean isEnabled(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6, Object p7, Object p8) {
        // SLF4J will check again later
        return true;
    }

    @Override
    protected boolean isEnabled(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
            Object p6, Object p7, Object p8, Object p9) {
        // SLF4J will check again later
        return true;
    }

}
