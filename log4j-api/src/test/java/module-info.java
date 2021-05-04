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
open module org.apache.logging.log4j {
    exports org.apache.logging.log4j.test;
    exports org.apache.logging.log4j.test.junit;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.assertj.core;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.engine;
    requires org.junit.jupiter.params;
    requires org.junit.platform.commons;
    requires org.junit.platform.engine;
    requires junit;

    uses org.apache.logging.log4j.spi.Provider;
    provides org.apache.logging.log4j.spi.Provider with org.apache.logging.log4j.TestProvider;
    uses org.apache.logging.log4j.util.PropertySource;
    uses org.apache.logging.log4j.message.ThreadDumpMessage.ThreadInfoFactory;
}