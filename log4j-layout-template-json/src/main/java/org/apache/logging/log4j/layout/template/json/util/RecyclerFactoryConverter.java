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
package org.apache.logging.log4j.layout.template.json.util;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverter;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverters;

/**
 * The default string (i.e., recycler factory spec) to {@link RecyclerFactory} type converter.
 */
@Plugin(name = "RecyclerFactoryConverter", category = TypeConverters.CATEGORY)
public final class RecyclerFactoryConverter implements TypeConverter<RecyclerFactory> {

    @Override
    public RecyclerFactory convert(final String recyclerFactorySpec) {
        return RecyclerFactories.ofSpec(recyclerFactorySpec);
    }
}
