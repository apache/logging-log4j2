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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutDefaults;
import org.apache.logging.log4j.layout.template.json.util.MapAccessor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * Accessor to the resolver configuration JSON object read from the template.
 * {@link TemplateResolver Template resolvers} can use this class to
 * read the configuration associated with them.
 * <p>
 * For instance, given the following template:
 * <pre>
 * {
 *   "@version": 1,
 *   "message": {
 *     "$resolver": "message",
 *     "stringified": true
 *   },
 *   "level": {
 *     "$resolver": "level",
 *     "field": "severity",
 *     "severity": {
 *       "field": "code"
 *     }
 *   }
 * }
 * </pre>
 * {@link LevelResolverFactory#create(EventResolverContext, TemplateResolverConfig)}
 * will be called with a {@link TemplateResolverConfig} accessor to the
 * following configuration JSON object block:
 * <pre>
 * {
 *   "$resolver": "level",
 *   "field": "severity",
 *   "severity": {
 *     "field": "code"
 *   }
 * }
 * </pre>
 */
public class TemplateResolverConfig extends MapAccessor {

    TemplateResolverConfig(final Map<String, Object> map) {
        super(map);
    }

    public Locale getLocale(final String key) {
        final String[] path = {key};
        return getLocale(path);
    }

    public Locale getLocale(final String[] path) {
        final String spec = getString(path);
        if (spec == null) {
            return JsonTemplateLayoutDefaults.getLocale();
        }
        final String[] specFields = spec.split("_", 3);
        switch (specFields.length) {
            case 1: return new Locale(specFields[0]);
            case 2: return new Locale(specFields[0], specFields[1]);
            case 3: return new Locale(specFields[0], specFields[1], specFields[2]);
        }
        final String message = String.format(
                "was expecting a locale at path %s: %s",
                Arrays.asList(path), this);
        throw new IllegalArgumentException(message);
    }

}
