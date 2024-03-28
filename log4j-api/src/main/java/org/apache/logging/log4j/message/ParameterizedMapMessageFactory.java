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
package org.apache.logging.log4j.message;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Extends a StringMapMessage to appender a "normal" Parameterized message to the Map data.
 */
public class ParameterizedMapMessageFactory extends AbstractMessageFactory {

    private final Supplier<Map<String, String>> mapSupplier;

    public ParameterizedMapMessageFactory(Supplier<Map<String, String>> mapSupplier) {
        this.mapSupplier = mapSupplier;
    }

    @Override
    public Message newMessage(final CharSequence message) {
        Map<String, String> map = mapSupplier.get();
        Message msg = new SimpleMessage(message);
        return map.isEmpty() ? msg : new ParameterizedMapMessage(msg, map);
    }

    @Override
    public Message newMessage(final Object message) {
        Map<String, String> map = mapSupplier.get();
        Message msg = new ObjectMessage(message);
        return map.isEmpty() ? msg : new ParameterizedMapMessage(msg, map);
    }

    @Override
    public Message newMessage(final String message) {
        Map<String, String> map = mapSupplier.get();
        Message msg = new SimpleMessage(message);
        return map.isEmpty() ? msg : new ParameterizedMapMessage(msg, map);
    }

    @Override
    public Message newMessage(final String message, final Object... params) {
        Map<String, String> map = mapSupplier.get();
        Message msg = new ParameterizedMessage(message, params);
        return map.isEmpty() ? msg : new ParameterizedMapMessage(msg, map);
    }

    @Override
    public Message newMessage(final String message, final Object p0) {
        Map<String, String> map = mapSupplier.get();
        Message msg = new ParameterizedMessage(message, p0);
        return map.isEmpty() ? msg : new ParameterizedMapMessage(msg, map);
    }

    @Override
    public Message newMessage(final String message, final Object p0, final Object p1) {
        Map<String, String> map = mapSupplier.get();
        Message msg = new ParameterizedMessage(message, p0, p1);
        return map.isEmpty() ? msg : new ParameterizedMapMessage(msg, map);
    }

    @Override
    public Message newMessage(final String message, final Object p0, final Object p1, final Object p2) {
        Map<String, String> map = mapSupplier.get();
        Message msg = new ParameterizedMessage(message, p0, p1, p2);
        return map.isEmpty() ? msg : new ParameterizedMapMessage(msg, map);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
        Map<String, String> map = mapSupplier.get();
        Message msg = new ParameterizedMessage(message, p0, p1, p2, p3);
        return map.isEmpty() ? msg : new ParameterizedMapMessage(msg, map);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        Map<String, String> map = mapSupplier.get();
        Message msg = new ParameterizedMessage(message, p0, p1, p2, p3, p4);
        return map.isEmpty() ? msg : new ParameterizedMapMessage(msg, map);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        Map<String, String> map = mapSupplier.get();
        Message msg = new ParameterizedMessage(message, p0, p1, p2, p3, p4, p5);
        return map.isEmpty() ? msg : new ParameterizedMapMessage(msg, map);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        Map<String, String> map = mapSupplier.get();
        Message msg = new ParameterizedMessage(message, p0, p1, p2, p3, p4, p5, p6);
        return map.isEmpty() ? msg : new ParameterizedMapMessage(msg, map);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        Map<String, String> map = mapSupplier.get();
        Message msg = new ParameterizedMessage(message, p0, p1, p2, p3, p4, p5, p6, p7);
        return map.isEmpty() ? msg : new ParameterizedMapMessage(msg, map);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        Map<String, String> map = mapSupplier.get();
        Message msg = new ParameterizedMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
        return map.isEmpty() ? msg : new ParameterizedMapMessage(msg, map);
    }

    /**
     * @since 2.6.1
     */
    @Override
    public Message newMessage(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        Map<String, String> map = mapSupplier.get();
        Message msg = new ParameterizedMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
        return map.isEmpty() ? msg : new ParameterizedMapMessage(msg, map);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ParameterizedMapMessageFactory)) {
            return false;
        }
        ParameterizedMapMessageFactory that = (ParameterizedMapMessageFactory) o;
        return Objects.equals(mapSupplier, that.mapSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapSupplier);
    }
}
