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

package org.apache.logging.log4j.plugins.spi.impl;

import org.apache.logging.log4j.plugins.spi.Bean;
import org.apache.logging.log4j.plugins.spi.BeanManager;
import org.apache.logging.log4j.plugins.spi.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.Producer;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.lang.reflect.Method;
import java.util.Collection;

class MethodProducerFactory extends AbstractProducerFactory {
    private final BeanManager beanManager;
    private final Collection<InjectionPoint> producerInjectionPoints;

    MethodProducerFactory(final BeanManager beanManager, final Bean<?> declaringBean,
                          final Method producerMethod, final Collection<InjectionPoint> producerInjectionPoints,
                          final Method disposerMethod, final Collection<InjectionPoint> disposerInjectionPoints) {
        super(declaringBean, producerMethod, disposerMethod, disposerInjectionPoints);
        this.producerInjectionPoints = producerInjectionPoints;
        this.beanManager = beanManager;
    }

    @Override
    public <T> Producer<T> createProducer(final Bean<T> bean) {
        final Method producerMethod = TypeUtil.cast(producerMember);
        return new MethodProducer<>(beanManager, declaringBean, producerMethod, producerInjectionPoints,
                disposerMethod, disposerInjectionPoints);
    }
}
