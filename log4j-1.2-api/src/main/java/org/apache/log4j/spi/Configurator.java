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
package org.apache.log4j.spi;

import java.io.InputStream;
import java.net.URL;

import org.apache.logging.log4j.core.LoggerContext;

/**
 * Log4j 1.x Configurator interface.
 */
public interface Configurator {

    public static final String INHERITED = "inherited";

    public static final String NULL = "null";


    /**
     Interpret a resource pointed by a InputStream and set up log4j accordingly.

     The configuration is done relative to the <code>hierarchy</code>
     parameter.

     @param inputStream The InputStream to parse

     @since 1.2.17
     */
    void doConfigure(InputStream inputStream, final LoggerContext loggerContext);

    /**
     Interpret a resource pointed by a URL and set up log4j accordingly.

     The configuration is done relative to the <code>hierarchy</code>
     parameter.

     @param url The URL to parse
     */
    void doConfigure(URL url, final LoggerContext loggerContext);
}
