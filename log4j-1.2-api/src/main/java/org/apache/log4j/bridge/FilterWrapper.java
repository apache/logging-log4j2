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
package org.apache.log4j.bridge;

import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This acts as a container for Log4j 2 Filters to be attached to Log4j 1 components. However, the Log4j 2
 * Filters will always be called directly so this class just acts as a container.
 */
public class FilterWrapper extends Filter {

    private final org.apache.logging.log4j.core.Filter filter;

    public FilterWrapper(org.apache.logging.log4j.core.Filter filter) {
        this.filter = filter;
    }

    public org.apache.logging.log4j.core.Filter getFilter() {
        return filter;
    }

    /**
     * This method is never called.
     * @param event The LoggingEvent to decide upon.
     * @return 0
     */
    @Override
    public int decide(LoggingEvent event) {
        return 0;
    }
}
