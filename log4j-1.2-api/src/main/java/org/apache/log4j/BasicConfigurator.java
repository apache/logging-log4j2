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
package org.apache.log4j;

/**
 * Provided for compatibility with Log4j 1.x.
 */
public class BasicConfigurator {

    protected BasicConfigurator() {
    }

    public static void configure() {
        LogManager.reconfigure();
    }

    /**
     * No-op implementation.
     * @param appender The appender.
     */
    public static void configure(final Appender appender) {
        // no-op
    }

    /**
     * No-op implementation.
     */
    public static void resetConfiguration() {
        // no-op
    }
}
