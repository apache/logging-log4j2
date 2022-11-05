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
import org.apache.logging.log4j.core.test.plugins.Log4jPlugins;
import org.apache.logging.log4j.plugins.model.PluginService;
module org.apache.logging.log4j.core.test {
    exports org.apache.logging.log4j.core.test;
    exports org.apache.logging.log4j.core.test.appender;
    exports org.apache.logging.log4j.core.test.appender.rolling.action;
    exports org.apache.logging.log4j.core.test.categories;
    exports org.apache.logging.log4j.core.test.hamcrest;
    exports org.apache.logging.log4j.core.test.junit;
    exports org.apache.logging.log4j.core.test.layout;
    exports org.apache.logging.log4j.core.test.net.mock;
    exports org.apache.logging.log4j.core.test.parser;
    exports org.apache.logging.log4j.core.test.util;

    opens org.apache.logging.log4j.core.test.junit to
      org.junit.platform.commons;

    requires java.naming;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.test;
    requires transitive org.apache.logging.log4j.plugins;
    requires org.apache.logging.log4j.plugins.test;
    requires transitive org.apache.logging.log4j.core;
    requires static org.hamcrest;
    requires static junit;
    requires static org.junit.jupiter.api;
    requires static org.junit.platform.commons;
    provides PluginService with Log4jPlugins;
}
