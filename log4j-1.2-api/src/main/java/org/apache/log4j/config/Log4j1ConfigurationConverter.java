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
package org.apache.log4j.config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

/**
 * Tool for converting a Log4j 1.x properties configuration file to Log4j 2.x XML configuration file.
 */
public final class Log4j1ConfigurationConverter {

    public static void main(String[] args) throws IOException {
        try (InputStream input = args.length > 0 ? new FileInputStream(args[0]) : System.in;
             OutputStream output = args.length > 1 ? new FileOutputStream(args[1]) : System.out) {
            ConfigurationBuilder<BuiltConfiguration> builder = new Log4j1ConfigurationParser().buildConfigurationBuilder(input);
            builder.writeXmlConfiguration(output);
        }
    }

}
