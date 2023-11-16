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
import static org.apache.log4j.xml.XmlConfiguration.PARAM_TAG;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.w3c.dom.Element;

@Plugin(name = "org.apache.log4j.rolling.SizeBasedTriggeringPolicy", category = CATEGORY)
public class SizeBasedTriggeringPolicyBuilder extends AbstractBuilder<TriggeringPolicy>
        implements TriggeringPolicyBuilder {

    private static final String MAX_SIZE_PARAM = "MaxFileSize";
    private static final long DEFAULT_MAX_SIZE = 10 * 1024 * 1024;

    public SizeBasedTriggeringPolicyBuilder() {
        super();
    }

    public SizeBasedTriggeringPolicyBuilder(final String prefix, final Properties props) {
        super(prefix, props);
    }

    @Override
    public SizeBasedTriggeringPolicy parse(final Element element, final XmlConfiguration configuration) {
        final AtomicLong maxSize = new AtomicLong(DEFAULT_MAX_SIZE);
        forEachElement(element.getChildNodes(), currentElement -> {
            switch (currentElement.getTagName()) {
                case PARAM_TAG:
                    switch (getNameAttributeKey(currentElement)) {
                        case MAX_SIZE_PARAM:
                            set(MAX_SIZE_PARAM, currentElement, maxSize);
                            break;
                    }
                    break;
            }
        });
        return createTriggeringPolicy(maxSize.get());
    }

    @Override
    public SizeBasedTriggeringPolicy parse(final PropertiesConfiguration configuration) {
        final long maxSize = getLongProperty(MAX_SIZE_PARAM, DEFAULT_MAX_SIZE);
        return createTriggeringPolicy(maxSize);
    }

    private SizeBasedTriggeringPolicy createTriggeringPolicy(final long maxSize) {
        return SizeBasedTriggeringPolicy.createPolicy(Long.toString(maxSize));
    }
}
