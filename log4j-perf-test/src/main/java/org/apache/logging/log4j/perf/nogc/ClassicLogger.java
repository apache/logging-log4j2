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
package org.apache.logging.log4j.perf.nogc;

import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.ParameterizedMessage;

/**
 * Logger with varargs methods, ParameterizedMessage and PatternLayout("%m").
 */
public class ClassicLogger extends AbstractLogger {

    @Override
    protected StringLayout createLayout() {
        return PatternLayout.newBuilder()
                .withCharset(StandardCharsets.UTF_8)
                .withPattern("%m")
                .build();
    }

    public void log(final String message, final Object... params) {
        log(new ParameterizedMessage(message, params));
        // log(new NoGcMessage(16).set(message, params[0], params[1], params[2], params[3]));
    }
}
