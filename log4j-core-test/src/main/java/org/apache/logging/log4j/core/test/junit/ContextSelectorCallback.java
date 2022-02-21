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

package org.apache.logging.log4j.core.test.junit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ContextSelectorCallback implements BeforeAllCallback {
    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        final ContextSelectorType source = testClass.getAnnotation(ContextSelectorType.class);
        if (source != null) {
            final Injector injector = DI.createInjector();
            injector.bindFactory(ContextSelector.KEY, injector.getFactory(source.value()));
            injector.init();
            final Log4jContextFactory factory = injector.getInstance(Log4jContextFactory.class);
            LogManager.setFactory(factory);
        }
    }
}
