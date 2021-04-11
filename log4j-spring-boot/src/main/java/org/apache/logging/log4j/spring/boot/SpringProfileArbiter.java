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
package org.apache.logging.log4j.spring.boot;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.arbiters.Arbiter;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

/**
 * An Aribter that uses the active Spring profile to determine if configuration should be included.
 */
@Plugin(name = "SpringProfile", category = Node.CATEGORY, elementType = Arbiter.ELEMENT_TYPE,
        deferChildren = true, printObject = true)
public class SpringProfileArbiter extends SpringEnvironmentHolder implements Arbiter {

    private final String[] profileNames;

    private SpringProfileArbiter(final String[] profiles) {
        this.profileNames = profiles;

    }

    @Override
    public boolean isCondition() {
        Environment environment = getEnvironment();
        if (environment == null) {
            return false;
        }

        if (profileNames.length == 0) {
            return false;
        }
        return environment.acceptsProfiles(Profiles.of(profileNames));
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<SpringProfileArbiter> {

        public static final String ATTR_NAME = "name";

        @PluginBuilderAttribute(ATTR_NAME)
        private String name;

        @PluginConfiguration
        private Configuration configuration;;

        /**
         * Sets the Profile Name or Names.
         * @param name the profile name(s).
         * @return this
         */
        public Builder setName(final String name) {
            this.name = name;
            return asBuilder();
        }

        public Builder setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return asBuilder();
        }

        private SpringProfileArbiter.Builder asBuilder() {
            return this;
        }

        public SpringProfileArbiter build() {
            String[] profileNames = StringUtils.trimArrayElements(
                    StringUtils.commaDelimitedListToStringArray(configuration.getStrSubstitutor().replace(name)));
            return new SpringProfileArbiter(profileNames);
        }
    }
}
