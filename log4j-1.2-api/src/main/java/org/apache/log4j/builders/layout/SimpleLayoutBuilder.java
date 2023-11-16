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
package org.apache.log4j.builders.layout;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;

import org.apache.log4j.Layout;
import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.w3c.dom.Element;

/**
 * Build a Pattern Layout
 */
@Plugin(name = "org.apache.log4j.SimpleLayout", category = CATEGORY)
public class SimpleLayoutBuilder implements LayoutBuilder {

    @Override
    public Layout parse(final Element layoutElement, final XmlConfiguration config) {
        return new LayoutWrapper(PatternLayout.newBuilder()
                .withPattern("%v1Level - %m%n")
                .withConfiguration(config)
                .build());
    }

    @Override
    public Layout parse(final PropertiesConfiguration config) {
        return new LayoutWrapper(PatternLayout.newBuilder()
                .withPattern("%v1Level - %m%n")
                .withConfiguration(config)
                .build());
    }
}
