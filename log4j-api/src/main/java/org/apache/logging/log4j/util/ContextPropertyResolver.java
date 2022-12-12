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
package org.apache.logging.log4j.util;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class ContextPropertyResolver implements PropertyResolver {
    private final PropertyResolver delegate;
    private final String context;

    public ContextPropertyResolver(final PropertyResolver delegate, final String context) {
        this.delegate = delegate;
        this.context = context;
    }

    @Override
    public void addSource(final PropertySource source) {
        throw new UnsupportedOperationException("Cannot modify ContextPropertyResolver view");
    }

    @Override
    public boolean hasProperty(final String key) {
        return hasProperty(context, key);
    }

    @Override
    public boolean hasProperty(final String context, final String key) {
        return delegate.hasProperty(context, key);
    }

    @Override
    public Optional<String> getString(final String key) {
        return getString(context, key);
    }

    @Override
    public Optional<String> getString(final String context, final String key) {
        return delegate.getString(context, key);
    }

    @Override
    public List<String> getList(final String key) {
        return getList(context, key);
    }

    @Override
    public List<String> getList(final String context, final String key) {
        return delegate.getList(context, key);
    }

    @Override
    public boolean getBoolean(final String key) {
        return getBoolean(context, key);
    }

    @Override
    public boolean getBoolean(final String key, final boolean defaultValue) {
        return getBoolean(context, key, defaultValue);
    }

    @Override
    public boolean getBoolean(final String context, final String key, final boolean defaultValue) {
        return delegate.getBoolean(context, key, defaultValue);
    }

    @Override
    public boolean getBoolean(final String key, final boolean defaultValueIfAbsent, final boolean defaultValueIfPresent) {
        return getBoolean(context, key, defaultValueIfAbsent, defaultValueIfPresent);
    }

    @Override
    public boolean getBoolean(final String context, final String key, final boolean defaultValueIfAbsent, final boolean defaultValueIfPresent) {
        return delegate.getBoolean(context, key, defaultValueIfAbsent, defaultValueIfPresent);
    }

    @Override
    public OptionalInt getInt(final String key) {
        return getInt(context, key);
    }

    @Override
    public OptionalInt getInt(final String context, final String key) {
        return delegate.getInt(context, key);
    }

    @Override
    public OptionalLong getLong(final String key) {
        return getLong(context, key);
    }

    @Override
    public OptionalLong getLong(final String context, final String key) {
        return delegate.getLong(context, key);
    }
}
