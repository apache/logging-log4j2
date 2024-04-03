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
package org.apache.logging.log4j.core.util;

import java.util.Map;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.util.StringMap;

/**
 * Source of context data to be added to each log event.
 * @deprecated Use ContextDataProvider from Log4j API from 2.24.0.
 */
@Deprecated
public interface ContextDataProvider extends org.apache.logging.log4j.spi.ContextDataProvider {

    Map<String, String> supplyContextData();

    /**
     * Returns the context data as a StringMap.
     * <p>
     *     Thread-safety note: The returned object can safely be passed off to another thread: future changes in the
     *     underlying context data will not be reflected in the returned object.
     * </p>
     * @return the context data in a StringMap.
     * @deprecated No longer used since 2.24.0. Will be removed in 3.0.0.
     */
    @Deprecated
    default StringMap supplyStringMap() {
        return new JdkMapAdapterStringMap(supplyContextData(), true);
    }
}
