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
package org.apache.logging.log4j.test.layout;

import java.nio.charset.Charset;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.util.Charsets;
import org.apache.logging.log4j.core.util.Constants;

/**
 *
 */
@Plugin(name = "BasicLayout", category = "Core", elementType = "layout", printObject = true)
public class BasicLayout extends AbstractStringLayout {

    private static final String HEADER = "Header" + Constants.LINE_SEPARATOR;

    @Override
    public byte[] getHeader() {
        return getBytes(HEADER);
    }

    public BasicLayout(final Charset charset) {
        super(charset);
    }

    @Override
    public String toSerializable(final LogEvent event) {
        return event.getMessage().getFormattedMessage() + Constants.LINE_SEPARATOR;
    }

    /**
     */
    @PluginFactory
    public static BasicLayout createLayout() {
        return new BasicLayout(Charsets.UTF_8);
    }
}
