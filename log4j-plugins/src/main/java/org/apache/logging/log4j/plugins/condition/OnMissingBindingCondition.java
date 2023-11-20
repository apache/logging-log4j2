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
package org.apache.logging.log4j.plugins.condition;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import org.apache.logging.log4j.plugins.di.InstanceFactory;
import org.apache.logging.log4j.plugins.di.Key;

public class OnMissingBindingCondition implements Condition {
    @Override
    public boolean matches(final ConditionContext context, final AnnotatedElement element) {
        final Key<?> key;
        if (element instanceof Method) {
            key = Key.forMethod((Method) element);
        } else if (element instanceof Class<?>) {
            key = Key.forClass((Class<?>) element);
        } else {
            return false;
        }
        final InstanceFactory instanceFactory = context.getInstanceFactory();
        return !instanceFactory.hasBinding(key);
    }
}
