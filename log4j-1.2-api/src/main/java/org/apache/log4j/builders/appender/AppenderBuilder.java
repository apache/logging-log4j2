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
package org.apache.log4j.builders.appender;

import java.util.Properties;
import org.apache.log4j.Appender;
import org.apache.log4j.builders.Builder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.w3c.dom.Element;

/**
 * Define an Appender Builder.
 *
 * @param <T> The type to build.
 */
public interface AppenderBuilder<T extends Appender> extends Builder<T> {

    Appender parseAppender(Element element, XmlConfiguration configuration);

    Appender parseAppender(
            String name,
            String appenderPrefix,
            String layoutPrefix,
            String filterPrefix,
            Properties props,
            PropertiesConfiguration configuration);
}
