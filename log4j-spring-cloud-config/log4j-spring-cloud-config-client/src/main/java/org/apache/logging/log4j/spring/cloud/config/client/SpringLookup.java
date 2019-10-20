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
package org.apache.logging.log4j.spring.cloud.config.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.springframework.core.env.Environment;

/**
 * Lookup for Spring properties.
 */
@Plugin(name = "spring", category = StrLookup.CATEGORY)
public class SpringLookup implements StrLookup {

    private final Environment environment;

    public SpringLookup() {
        Object obj = LogManager.getContext(false).getObject(Log4j2CloudConfigLoggingSystem.ENVIRONMENT_KEY);
        environment = obj instanceof Environment ? (Environment) obj : null;
    }

    @Override
    public String lookup(String key) {
        if (environment != null) {
            return environment.getProperty(key);
        }
        return null;
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return lookup((key));
    }
}
