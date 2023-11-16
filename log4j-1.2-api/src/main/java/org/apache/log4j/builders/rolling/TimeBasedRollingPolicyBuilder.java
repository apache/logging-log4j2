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
package org.apache.log4j.builders.rolling;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;

import java.util.Properties;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.w3c.dom.Element;

@Plugin(name = "org.apache.log4j.rolling.TimeBasedRollingPolicy", category = CATEGORY)
public class TimeBasedRollingPolicyBuilder extends AbstractBuilder<TriggeringPolicy>
        implements TriggeringPolicyBuilder {

    public TimeBasedRollingPolicyBuilder(final String prefix, final Properties props) {
        super(prefix, props);
    }

    public TimeBasedRollingPolicyBuilder() {
        super();
    }

    @Override
    public TimeBasedTriggeringPolicy parse(final Element element, final XmlConfiguration configuration) {
        return createTriggeringPolicy();
    }

    @Override
    public TimeBasedTriggeringPolicy parse(final PropertiesConfiguration configuration) {
        return createTriggeringPolicy();
    }

    private TimeBasedTriggeringPolicy createTriggeringPolicy() {
        return TimeBasedTriggeringPolicy.newBuilder().build();
    }
}
