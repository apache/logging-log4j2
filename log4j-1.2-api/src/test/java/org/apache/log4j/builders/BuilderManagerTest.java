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
package org.apache.log4j.builders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.varia.StringMatchFilter;
import org.junit.jupiter.api.Test;

public class BuilderManagerTest {

    /**
     * This test ensures that instantiation failures due to missing parameters
     * always return an empty wrapper instead of null, hence disabling the
     * <i>"instantiate by classname"</i> fallback mechanism for supported components.
     */
    @Test
    public void testReturnInvalidValueOnError() {
        final PropertiesConfiguration config = new PropertiesConfiguration(null, null);
        final BuilderManager manager = new BuilderManager();
        final Properties props = new Properties();
        props.setProperty("FILE", FileAppender.class.getName());
        props.setProperty("FILE.filter.1", StringMatchFilter.class.getName());
        // Parse an invalid StringMatchFilter
        final Filter filter = manager.parse(
                StringMatchFilter.class.getName(), "FILE.filter", props, config, BuilderManager.INVALID_FILTER);
        assertEquals(BuilderManager.INVALID_FILTER, filter);
        // Parse an invalid FileAppender
        final Appender appender = manager.parseAppender(
                "FILE", FileAppender.class.getName(), "FILE", "FILE.layout", "FILE.filter.", props, config);
        assertEquals(BuilderManager.INVALID_APPENDER, appender);
    }
}
