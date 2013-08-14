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
package org.apache.logging.log4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Test;


public class Log4jInitPerformance {


    @Test
    public void testInitialize() throws Exception {
        final String log4jConfigString =
            "<Configuration name=\"ConfigTest\" status=\"debug\" >" +
                "<Appenders>" +
                " <Console name=\"STDOUT\">" +
                "    <PatternLayout pattern=\"%m%n\"/>" +
                " </Console>" +
                "</Appenders>" +
                "<Loggers>" +
                "  <Root level=\"error\">" +
                "    <AppenderRef ref=\"STDOUT\"/>" +
                "  </Root>" +
                "</Loggers>" +
                "</Configuration>";
        final InputStream is = new ByteArrayInputStream(log4jConfigString.getBytes());
        final ConfigurationFactory.ConfigurationSource source =
            new ConfigurationFactory.ConfigurationSource(is);
        final long begin = System.currentTimeMillis();
        Configurator.initialize(null, source);
        final long tookForInit = System.currentTimeMillis() - begin;
        System.out.println("log4j 2.0 initialization took " + tookForInit + "ms");
    }

}