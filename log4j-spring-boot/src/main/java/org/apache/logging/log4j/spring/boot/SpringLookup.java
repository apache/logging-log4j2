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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.status.StatusLogger;
import org.springframework.core.env.Environment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lookup for Spring properties.
 */
@Plugin(name = "spring", category = StrLookup.CATEGORY)
public class SpringLookup extends SpringEnvironmentHolder implements StrLookup {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String ACTIVE = "profiles.active";
    private static final String DEFAULT = "profiles.default";
    private static final String PATTERN = "\\[(\\d+?)\\]";
    private static final Pattern ACTIVE_PATTERN = Pattern.compile(ACTIVE + PATTERN);
    private static final Pattern DEFAULT_PATTERN = Pattern.compile(DEFAULT + PATTERN);

    public SpringLookup() {
        getEnvironment();
    }

    @Override
    public String lookup(String key) {
        Environment env = getEnvironment();
        if (env != null) {
            String lowerKey = key.toLowerCase();
            if (lowerKey.startsWith(ACTIVE)) {
                switch (env.getActiveProfiles().length) {
                    case 0: {
                        return null;
                    }
                    case 1: {
                        return env.getActiveProfiles()[0];
                    }
                    default: {
                        Matcher matcher = ACTIVE_PATTERN.matcher(key);
                        if (matcher.matches()) {
                            try {
                                int index = Integer.parseInt(matcher.group(1));
                                if (index < env.getActiveProfiles().length) {
                                    return env.getActiveProfiles()[index];
                                }
                                LOGGER.warn("Index out of bounds for Spring active profiles: {}", index);
                                return null;
                            } catch (Exception ex) {
                                LOGGER.warn("Unable to parse {} as integer value", matcher.group(1));
                                return null;
                            }

                        }
                        return String.join(",", env.getActiveProfiles());
                    }
                }
            } else if (lowerKey.startsWith(DEFAULT)) {
                switch (env.getDefaultProfiles().length) {
                    case 0: {
                        return null;
                    }
                    case 1: {
                        return env.getDefaultProfiles()[0];
                    }
                    default: {
                        Matcher matcher = DEFAULT_PATTERN.matcher(key);
                        if (matcher.matches()) {
                            try {
                                int index = Integer.parseInt(matcher.group(1));
                                if (index < env.getDefaultProfiles().length) {
                                    return env.getDefaultProfiles()[index];
                                }
                                LOGGER.warn("Index out of bounds for Spring default profiles: {}", index);
                                return null;
                            } catch (Exception ex) {
                                LOGGER.warn("Unable to parse {} as integer value", matcher.group(1));
                                return null;
                            }

                        }
                        return String.join(",", env.getDefaultProfiles());
                    }
                }
            }

            return env.getProperty(key);

        }
        return null;
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return lookup((key));
    }
}
