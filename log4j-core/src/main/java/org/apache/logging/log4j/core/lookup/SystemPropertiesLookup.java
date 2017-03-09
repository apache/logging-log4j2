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
package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Looks up keys from system properties using an expression.
 * The expression can be either:
 * <ol>
 *   <li>A single property key for {@code System.getProperty(key)}</li>
 *   <li>
 *     A series of pipe-separated property keys followed by a default value:
 *
 *     <blockquote>
 *       <pre>key<sub>1</sub>|key<sub>2</sub>|...|key<sub>n</sub>|defaultValue</pre>
 *     </blockquote>
 *
 *     If <code>key<sub>1</sub></code> is not {@code null}, then it is used.
 *     Otherwise <code>key<sub>2</sub></code> is checked and so on until the {@code defaultValue} is reached.
 *     Example:
 *
 *     <blockquote>
 *       <pre>&lt;Logger name="com.myapp.dao" level="${sys:myapp.log.level|myapp.log.level.dao|warn}"/&gt;</pre>
 *     </blockquote>
 *
 *   </li>
 * </ol>
 */
@Plugin(name = "sys", category = StrLookup.CATEGORY)
public class SystemPropertiesLookup extends AbstractLookup {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final Marker LOOKUP = MarkerManager.getMarker("LOOKUP");

    /**
     * @param event The current LogEvent.
     * @param expression the expression to be looked up.
     * @return The value resolved by expression.
     */
    @Override
    public String lookup(final LogEvent event, final String expression) {
        try {
            if (expression.indexOf('|') < 0) {
                return System.getProperty(expression);
            }

            String[] expressionElements = expression.split("\\|");

            for (int i = 0; i < expressionElements.length - 1; i++) {
                String propertyValue = System.getProperty(expressionElements[i]);

                if (propertyValue != null) {
                    return propertyValue;
                }
            }

            return expressionElements[expressionElements.length - 1];
        } catch (final Exception ex) {
            LOGGER.warn(LOOKUP, "Error while resolving system property by expression [{}].", expression, ex);
            return null;
        }
    }
}