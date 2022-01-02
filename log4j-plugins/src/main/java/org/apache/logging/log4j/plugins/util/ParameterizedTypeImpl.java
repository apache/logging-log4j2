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

package org.apache.logging.log4j.plugins.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

class ParameterizedTypeImpl implements ParameterizedType {
    private final Type ownerType;
    private final Type rawType;
    private final Type[] actualTypeArguments;

    ParameterizedTypeImpl(final Type ownerType, final Type rawType, final Type... actualTypeArguments) {
        this.ownerType = ownerType;
        this.rawType = rawType;
        this.actualTypeArguments = actualTypeArguments;
    }

    @Override
    public Type getOwnerType() {
        return ownerType;
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return Arrays.copyOf(actualTypeArguments, actualTypeArguments.length);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ParameterizedType)) return false;
        final ParameterizedType that = (ParameterizedType) o;
        return Objects.equals(ownerType, that.getOwnerType()) &&
                Objects.equals(rawType, that.getRawType()) &&
                Arrays.equals(actualTypeArguments, that.getActualTypeArguments());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(ownerType, rawType);
        result = 31 * result + Arrays.hashCode(actualTypeArguments);
        return result;
    }
}
