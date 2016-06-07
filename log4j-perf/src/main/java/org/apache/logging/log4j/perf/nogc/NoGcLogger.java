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
package org.apache.logging.log4j.perf.nogc;

import org.apache.logging.log4j.core.Layout;

import java.nio.charset.StandardCharsets;

/**
 * Logger with unrolled varargs, cached Message and NoGcLayout.
 */
public class NoGcLogger extends AbstractLogger {
    private final NoGcMessage reusedMessage = new NoGcMessage();

    @Override
    protected Layout<?> createLayout() {
        return new NoGcLayout(StandardCharsets.UTF_8);
    }

    public void log(final String message, final Object p1, final Object p2, final Object p3, final Object p4) {
        reusedMessage.set(message, p1, p2, p3, p4);
        log(reusedMessage);
    }

}
