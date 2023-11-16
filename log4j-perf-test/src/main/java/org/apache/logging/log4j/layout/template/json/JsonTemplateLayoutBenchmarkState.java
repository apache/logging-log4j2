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
package org.apache.logging.log4j.layout.template.json;

import co.elastic.logging.log4j2.EcsLayout;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.GelfLayout;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout.EventTemplateAdditionalField;
import org.apache.logging.log4j.layout.template.json.util.ThreadLocalRecyclerFactory;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class JsonTemplateLayoutBenchmarkState {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final int LOG_EVENT_COUNT = 1_000;

    private final ByteBufferDestination byteBufferDestination;

    private final Layout<?> jtl4JsonLayout;

    private final Layout<?> jtl4EcsLayout;

    private final Layout<?> jtl4GelfLayout;

    private final Layout<?> defaultJsonLayout;

    private final Layout<?> customJsonLayout;

    private final Layout<?> ecsLayout;

    private final Layout<?> gelfLayout;

    private final List<LogEvent> fullLogEvents;

    private final List<LogEvent> liteLogEvents;

    private int logEventIndex = 0;

    public JsonTemplateLayoutBenchmarkState() {
        this.byteBufferDestination = new BlackHoleByteBufferDestination(1024 * 512);
        this.jtl4JsonLayout = createJtl4JsonLayout();
        this.jtl4EcsLayout = createJtl4EcsLayout();
        this.jtl4GelfLayout = createJtl4GelfLayout();
        this.defaultJsonLayout = createDefaultJsonLayout();
        this.customJsonLayout = createCustomJsonLayout();
        this.ecsLayout = createEcsLayout();
        this.gelfLayout = createGelfLayout();
        this.fullLogEvents = LogEventFixture.createFullLogEvents(LOG_EVENT_COUNT);
        this.liteLogEvents = LogEventFixture.createLiteLogEvents(LOG_EVENT_COUNT);
    }

    private static JsonTemplateLayout createJtl4JsonLayout() {
        return JsonTemplateLayout.newBuilder()
                .setConfiguration(CONFIGURATION)
                .setCharset(CHARSET)
                .setEventTemplateUri("classpath:JsonLayout.json")
                .setRecyclerFactory(ThreadLocalRecyclerFactory.getInstance())
                .build();
    }

    private static JsonTemplateLayout createJtl4EcsLayout() {
        final EventTemplateAdditionalField[] additionalFields = new EventTemplateAdditionalField[] {
            EventTemplateAdditionalField.newBuilder()
                    .setKey("service.name")
                    .setValue("benchmark")
                    .build()
        };
        return JsonTemplateLayout.newBuilder()
                .setConfiguration(CONFIGURATION)
                .setCharset(CHARSET)
                .setEventTemplateUri("classpath:EcsLayout.json")
                .setRecyclerFactory(ThreadLocalRecyclerFactory.getInstance())
                .setEventTemplateAdditionalFields(additionalFields)
                .build();
    }

    private static JsonTemplateLayout createJtl4GelfLayout() {
        return JsonTemplateLayout.newBuilder()
                .setConfiguration(CONFIGURATION)
                .setCharset(CHARSET)
                .setEventTemplateUri("classpath:GelfLayout.json")
                .setRecyclerFactory(ThreadLocalRecyclerFactory.getInstance())
                .setEventTemplateAdditionalFields(new EventTemplateAdditionalField[] {
                    // Adding "host" as a constant rather than using
                    // the "hostName" property lookup at runtime, which
                    // is what GelfLayout does as well.
                    EventTemplateAdditionalField.newBuilder()
                            .setKey("host")
                            .setValue(NetUtils.getLocalHostname())
                            .build()
                })
                .build();
    }

    private static JsonLayout createDefaultJsonLayout() {
        return JsonLayout.newBuilder()
                .setConfiguration(CONFIGURATION)
                .setCharset(CHARSET)
                .build();
    }

    private static JsonLayout createCustomJsonLayout() {
        return JsonLayout.newBuilder()
                .setConfiguration(CONFIGURATION)
                .setCharset(CHARSET)
                .setAdditionalFields(new KeyValuePair[] {new KeyValuePair("@version", "\"1\"")})
                .build();
    }

    private static EcsLayout createEcsLayout() {
        final EcsLayout layout = EcsLayout.newBuilder()
                .setConfiguration(CONFIGURATION)
                .setServiceName("benchmark")
                .build();
        final Charset layoutCharset = layout.getCharset();
        // Note that EcsLayout doesn't support charset configuration, though it
        // uses UTF-8 internally.
        if (!CHARSET.equals(layoutCharset)) {
            throw new IllegalArgumentException(
                    "was expecting EcsLayout charset to be: " + CHARSET + ", found: " + layoutCharset);
        }
        return layout;
    }

    private static GelfLayout createGelfLayout() {
        return GelfLayout.newBuilder()
                .setConfiguration(CONFIGURATION)
                .setCharset(CHARSET)
                .setCompressionType(GelfLayout.CompressionType.OFF)
                .build();
    }

    ByteBufferDestination getByteBufferDestination() {
        return byteBufferDestination;
    }

    Layout<?> getJtl4JsonLayout() {
        return jtl4JsonLayout;
    }

    Layout<?> getJtl4EcsLayout() {
        return jtl4EcsLayout;
    }

    Layout<?> getJtl4GelfLayout() {
        return jtl4GelfLayout;
    }

    Layout<?> getDefaultJsonLayout() {
        return defaultJsonLayout;
    }

    Layout<?> getCustomJsonLayout() {
        return customJsonLayout;
    }

    Layout<?> getEcsLayout() {
        return ecsLayout;
    }

    Layout<?> getGelfLayout() {
        return gelfLayout;
    }

    List<LogEvent> getFullLogEvents() {
        return fullLogEvents;
    }

    List<LogEvent> getLiteLogEvents() {
        return liteLogEvents;
    }

    int nextLogEventIndex() {
        final int currentLogEventIndex = logEventIndex;
        logEventIndex = (logEventIndex + 1) % LOG_EVENT_COUNT;
        return currentLogEventIndex;
    }
}
