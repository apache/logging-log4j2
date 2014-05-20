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
package org.apache.logging.log4j.samples.app;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.samples.dto.AuditEvent;
import org.apache.logging.log4j.samples.dto.Constraint;
import org.apache.logging.log4j.samples.util.NamingUtils;

/**
 *
 */
public class LogEventFactory {

    @SuppressWarnings("unchecked")
    public static <T extends AuditEvent> T getEvent(final Class<T> intrface) {

        final String eventId = NamingUtils.lowerFirst(intrface.getSimpleName());
        final StructuredDataMessage msg = new StructuredDataMessage(eventId, null, "Audit");
        return (T)Proxy.newProxyInstance(intrface
            .getClassLoader(), new Class<?>[]{intrface}, new AuditProxy(msg, intrface));
    }

    private static class AuditProxy implements InvocationHandler {

        private final StructuredDataMessage msg;
        private final Class<?> intrface;

        public AuditProxy(final StructuredDataMessage msg, final Class<?> intrface) {
            this.msg = msg;
            this.intrface = intrface;
        }

        @Override
        public Object invoke(final Object o, final Method method, final Object[] objects)
            throws Throwable {
            if (method.getName().equals("logEvent")) {

                final StringBuilder missing = new StringBuilder();

                final Method[] methods = intrface.getMethods();

                for (final Method _method : methods) {
                    final String name = NamingUtils.lowerFirst(NamingUtils
                        .getMethodShortName(_method.getName()));

                    final Annotation[] annotations = _method.getDeclaredAnnotations();
                    for (final Annotation annotation : annotations) {
                        final Constraint constraint = (Constraint) annotation;

                        if (constraint.required() && msg.get(name) == null) {
                            if (missing.length() > 0) {
                                missing.append(", ");
                            }
                            missing.append(name);
                        }
                    }
                }

                if (missing.length() > 0) {
                    throw new IllegalStateException("Event " + msg.getId().getName() +
                        " is missing required attributes " + missing);
                }
                EventLogger.logEvent(msg);
            }
            if (method.getName().equals("setCompletionStatus")) {
                final String name = NamingUtils.lowerFirst(NamingUtils.getMethodShortName(method.getName()));
                msg.put(name, objects[0].toString());
            }
            if (method.getName().startsWith("set")) {
                final String name = NamingUtils.lowerFirst(NamingUtils.getMethodShortName(method.getName()));

                /*
                 * Perform any validation here. Currently the catalog doesn't
                 * contain any information on validation rules.
                 */
                msg.put(name, objects[0].toString());
            }

            return null;
        }

    }
}
