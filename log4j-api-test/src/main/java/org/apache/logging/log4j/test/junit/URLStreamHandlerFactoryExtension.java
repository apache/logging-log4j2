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

package org.apache.logging.log4j.test.junit;

import org.apache.logging.log4j.util.ReflectionUtil;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.net.URLStreamHandlerFactory;

public class URLStreamHandlerFactoryExtension implements BeforeAllCallback {
    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        final URLStreamHandlerFactory factory = AnnotationSupport.findAnnotation(testClass, UsingURLStreamHandlerFactory.class)
                .map(UsingURLStreamHandlerFactory::value)
                .map(ReflectionUtil::instantiate)
                .orElseThrow();
        final URLStreamHandlerFactory oldFactory = URLStreamHandlerFactories.getURLStreamHandlerFactory();
        URLStreamHandlerFactories.setURLStreamHandlerFactory(factory);
        context.getStore(ExtensionContext.Namespace.create(getClass(), testClass))
                .put(URLStreamHandlerFactory.class, (ExtensionContext.Store.CloseableResource) () ->
                        URLStreamHandlerFactories.setURLStreamHandlerFactory(oldFactory));
    }
}
