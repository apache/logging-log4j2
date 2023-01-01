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

import java.util.Map;
import java.util.function.Supplier;

import org.apache.logging.log4j.plugins.ContextScoped;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Namespace;

@ContextScoped
public class InterpolatorFactory {
    private final Map<String, Supplier<StrLookup>> strLookupPlugins;

    @Inject
    public InterpolatorFactory(@Namespace(StrLookup.CATEGORY) final Map<String, Supplier<StrLookup>> strLookupPlugins) {
        this.strLookupPlugins = strLookupPlugins;
    }

    public Interpolator newInterpolator(final StrLookup defaultLookup) {
        return new Interpolator(defaultLookup, strLookupPlugins);
    }
}
