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
package org.apache.logging.log4j.kit.env.internal;

import java.io.IOException;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.kit.env.support.BasicPropertyEnvironment;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.jspecify.annotations.Nullable;

/**
 * An adapter of the {@link PropertiesUtil} from Log4j API 2.x.
 * <p>
 *     Since {@link PropertiesUtil} requires all properties to start with {@code log4j2.}, we must add the prefix
 *     before querying for the property.
 * </p>
 */
public class PropertiesUtilPropertyEnvironment extends BasicPropertyEnvironment {

    private static final String PREFIX = "log4j.";
    private static final String PROPERTY_MAPPING_RESOURCE = "META-INF/log4j/propertyMapping.json";
    public static final PropertyEnvironment INSTANCE =
            new PropertiesUtilPropertyEnvironment(PropertiesUtil.getProperties(), StatusLogger.getLogger());

    private final PropertiesUtil propsUtil;
    private final PropertyMapping propertyMapping;

    public PropertiesUtilPropertyEnvironment(final PropertiesUtil propsUtil, final Logger statusLogger) {
        this(propsUtil, statusLogger, getPropertyMapping(statusLogger));
    }

    PropertiesUtilPropertyEnvironment(
            final PropertiesUtil propsUtil, final Logger statusLogger, final PropertyMapping propertyMapping) {
        super(statusLogger);
        this.propsUtil = propsUtil;
        this.propertyMapping = propertyMapping;
    }

    private static PropertyMapping getPropertyMapping(final Logger statusLogger) {
        try {
            return DefaultPropertyMappingParser.parse(PROPERTY_MAPPING_RESOURCE);
        } catch (final IllegalArgumentException | IOException e) {
            statusLogger.error("Unable to load legacy property mappings.", e);
            return PropertyMapping.EMPTY;
        }
    }

    @Override
    public @Nullable String getStringProperty(final String name) {
        String value = propsUtil.getStringProperty(PREFIX + name);
        if (value == null) {
            final List<? extends String> legacyKeys = propertyMapping.getLegacyKeys(name);
            for (int i = 0; value == null && i < legacyKeys.size(); i++) {
                value = propsUtil.getStringProperty(legacyKeys.get(i));
            }
        }
        return value;
    }
}
