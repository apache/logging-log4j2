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

import static org.apache.log4j.builders.BuilderManager.CATEGORY;

import java.util.Properties;
import org.apache.log4j.Appender;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.core.appender.NullAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.w3c.dom.Element;

/**
 * Build a Null Appender
 */
@Plugin(name = "org.apache.log4j.varia.NullAppender", category = CATEGORY)
public class NullAppenderBuilder implements AppenderBuilder {

    @Override
    public Appender parseAppender(final Element appenderElement, final XmlConfiguration config) {
        final String name = appenderElement.getAttribute("name");
        return AppenderWrapper.adapt(NullAppender.createAppender(name));
    }

    @Override
    public Appender parseAppender(
            final String name,
            final String appenderPrefix,
            final String layoutPrefix,
            final String filterPrefix,
            final Properties props,
            final PropertiesConfiguration configuration) {
        return AppenderWrapper.adapt(NullAppender.createAppender(name));
    }
}
