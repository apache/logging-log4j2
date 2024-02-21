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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.kit.env.support.BasicPropertyEnvironment;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * An adapter of the {@link PropertiesUtil} from Log4j API 2.x.
 *
 * @implNote Since {@link PropertiesUtil} requires all properties to start with {@code log4j2.}, we must add the prefix
 * before querying for the property.
 */
public class PropertiesUtilPropertyEnvironment extends BasicPropertyEnvironment {

    private static final String PREFIX = "log4j2.";
    public static final PropertyEnvironment INSTANCE =
            new PropertiesUtilPropertyEnvironment(PropertiesUtil.getProperties(), StatusLogger.getLogger());

    private final PropertiesUtil propsUtil;

    public PropertiesUtilPropertyEnvironment(final PropertiesUtil propsUtil, final Logger statusLogger) {
        super(statusLogger);
        this.propsUtil = propsUtil;
    }

    @Override
    public String getStringProperty(final String name) {
        return propsUtil.getStringProperty(PREFIX + name);
    }
}
