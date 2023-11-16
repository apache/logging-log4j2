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
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.w3c.dom.Element;

@Plugin(name = "org.apache.log4j.rolling.CompositeTriggeringPolicy", category = CATEGORY)
public class CompositeTriggeringPolicyBuilder extends AbstractBuilder<TriggeringPolicy>
        implements TriggeringPolicyBuilder {

    private static final TriggeringPolicy[] EMPTY_TRIGGERING_POLICIES = new TriggeringPolicy[0];
    private static final String POLICY_TAG = "triggeringPolicy";

    public CompositeTriggeringPolicyBuilder() {
        super();
    }

    public CompositeTriggeringPolicyBuilder(final String prefix, final Properties props) {
        super(prefix, props);
    }

    @Override
    public CompositeTriggeringPolicy parse(final Element element, final XmlConfiguration configuration) {
        final List<TriggeringPolicy> policies = new ArrayList<>();
        forEachElement(element.getChildNodes(), currentElement -> {
            switch (currentElement.getTagName()) {
                case POLICY_TAG:
                    final TriggeringPolicy policy = configuration.parseTriggeringPolicy(currentElement);
                    if (policy != null) {
                        policies.add(policy);
                    }
                    break;
            }
        });
        return createTriggeringPolicy(policies);
    }

    @Override
    public CompositeTriggeringPolicy parse(final PropertiesConfiguration configuration) {
        return createTriggeringPolicy(Collections.emptyList());
    }

    private CompositeTriggeringPolicy createTriggeringPolicy(final List<TriggeringPolicy> policies) {
        return CompositeTriggeringPolicy.createPolicy(policies.toArray(EMPTY_TRIGGERING_POLICIES));
    }
}
