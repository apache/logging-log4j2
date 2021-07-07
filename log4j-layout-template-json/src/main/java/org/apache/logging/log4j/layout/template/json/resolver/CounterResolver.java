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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * Resolves a number from an internal counter.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config   = [ start ] , [ overflow ]
 * start    = "start" -> number
 * overflow = "overflow" -> boolean
 * </pre>
 *
 * Unless provided, <tt>start</tt> and <tt>overflow</tt> are respectively set to
 * zero and <tt>true</tt> by default.
 * <p>
 * When <tt>overflow</tt> is enabled, the internal counter is created using a
 * <tt>long</tt>, which is subject to overflow while incrementing, though
 * garbage-free. Otherwise, a {@link BigInteger} is used, which does not
 * overflow, but incurs allocation costs.
 *
 * <h3>Examples</h3>
 *
 * Resolves a sequence of numbers starting from 0. Once {@link Long#MAX_VALUE}
 * is reached, counter overflows to {@link Long#MIN_VALUE}.
 *
 * <pre>
 * {
 *   "$resolver": "counter"
 * }
 * </pre>
 *
 * Resolves a sequence of numbers starting from 1000. Once {@link Long#MAX_VALUE}
 * is reached, counter overflows to {@link Long#MIN_VALUE}.
 *
 * <pre>
 * {
 *   "$resolver": "counter",
 *   "start": 1000
 * }
 * </pre>
 *
 * Resolves a sequence of numbers starting from 0 and keeps on doing as long as
 * JVM heap allows.
 *
 * <pre>
 * {
 *   "$resolver": "counter",
 *   "overflow": false
 * }
 * </pre>
 */
public class CounterResolver implements EventResolver {

    private final Consumer<JsonWriter> delegate;

    public CounterResolver(final TemplateResolverConfig config) {
        final BigInteger start = readStart(config);
        final boolean overflow = config.getBoolean("overflow", true);
        this.delegate = overflow
                ? createLongResolver(start)
                : createBigIntegerResolver(start);
    }

    private static BigInteger readStart(final TemplateResolverConfig config) {
        final Object start = config.getObject("start", Object.class);
        if (start == null) {
            return BigInteger.ZERO;
        } else if (start instanceof Short || start instanceof Integer || start instanceof Long) {
            return BigInteger.valueOf(((Number) start).longValue());
        } else if (start instanceof BigInteger) {
            return (BigInteger) start;
        } else {
            final Class<?> clazz = start.getClass();
            final String message = String.format(
                    "could not read start of type %s: %s", clazz, config);
            throw new IllegalArgumentException(message);
        }
    }

    private static Consumer<JsonWriter> createLongResolver(final BigInteger start) {
        final long effectiveStart = start.longValue();
        final AtomicLong counter = new AtomicLong(effectiveStart);
        return (jsonWriter) -> {
            final long number = counter.getAndIncrement();
            jsonWriter.writeNumber(number);
        };
    }

    private static Consumer<JsonWriter> createBigIntegerResolver(final BigInteger start) {
        final AtomicBigInteger counter = new AtomicBigInteger(start);
        return jsonWriter -> {
            final BigInteger number = counter.getAndIncrement();
            jsonWriter.writeNumber(number);
        };
    }

    private static final class AtomicBigInteger {

        private final AtomicReference<BigInteger> lastNumber;

        private AtomicBigInteger(final BigInteger start) {
            this.lastNumber = new AtomicReference<>(start);
        }

        private BigInteger getAndIncrement() {
            BigInteger prevNumber;
            BigInteger nextNumber;
            do {
                prevNumber = lastNumber.get();
                nextNumber = prevNumber.add(BigInteger.ONE);
            } while (!compareAndSetWithBackOff(prevNumber, nextNumber));
            return prevNumber;
        }

        /**
         * {@link AtomicReference#compareAndSet(Object, Object)} shortcut with a
         * constant back off. This technique was originally described in
         * <a href="https://arxiv.org/abs/1305.5800">Lightweight Contention
         * Management for Efficient Compare-and-Swap Operations</a> and showed
         * great results in benchmarks.
         */
        private boolean compareAndSetWithBackOff(
                final BigInteger prevNumber,
                final BigInteger nextNumber) {
            if (lastNumber.compareAndSet(prevNumber, nextNumber)) {
                return true;
            }
            LockSupport.parkNanos(1); // back-off
            return false;
        }

    }

    static String getName() {
        return "counter";
    }

    @Override
    public void resolve(final LogEvent ignored, final JsonWriter jsonWriter) {
        delegate.accept(jsonWriter);
    }

}
