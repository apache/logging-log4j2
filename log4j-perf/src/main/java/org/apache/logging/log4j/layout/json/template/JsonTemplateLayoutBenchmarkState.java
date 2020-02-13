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
package org.apache.logging.log4j.layout.json.template;

import co.elastic.logging.log4j2.EcsLayout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.GelfLayout;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.jackson.json.layout.JsonLayout;
import org.apache.logging.log4j.layout.json.template.JsonTemplateLayout.EventTemplateAdditionalFields;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.List;

@State(Scope.Benchmark)
public class JsonTemplateLayoutBenchmarkState {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final int LOGSTASH_LAYOUT_MAX_BYTE_COUNT = 4096;

    private final ByteBufferDestination byteBufferDestination;

    private final JsonTemplateLayout jsonTemplateLayout4JsonLayout;

    private final JsonTemplateLayout jsonTemplateLayout4EcsLayout;

    private final JsonTemplateLayout jsonTemplateLayout4GelfLayout;

    private final JsonLayout defaultJsonLayout;

    private final JsonLayout customJsonLayout;

    private final EcsLayout ecsLayout;

    private final GelfLayout gelfLayout;

    private final List<LogEvent> fullLogEvents;

    private final List<LogEvent> liteLogEvents;

    public JsonTemplateLayoutBenchmarkState() {
        this.byteBufferDestination = new BlackHoleByteBufferDestination(1024 * 512);
        this.jsonTemplateLayout4JsonLayout = createJsonTemplateLayout4JsonLayout();
        this.jsonTemplateLayout4EcsLayout = createJsonTemplateLayout4EcsLayout();
        this.jsonTemplateLayout4GelfLayout = createJsonTemplateLayout4GelfLayout();
        this.defaultJsonLayout = createDefaultJsonLayout();
        this.customJsonLayout = createCustomJsonLayout();
        this.ecsLayout = createEcsLayout();
        this.gelfLayout = createGelfLayout();
        int logEventCount = 1_000;
        this.fullLogEvents = LogEventFixture.createFullLogEvents(logEventCount);
        this.liteLogEvents = LogEventFixture.createLiteLogEvents(logEventCount);
    }

    private static JsonTemplateLayout createJsonTemplateLayout4JsonLayout() {
        return JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplateUri("classpath:JsonLayout.json")
                .setMaxByteCount(LOGSTASH_LAYOUT_MAX_BYTE_COUNT)
                .build();
    }

    private static JsonTemplateLayout createJsonTemplateLayout4EcsLayout() {
        final EventTemplateAdditionalFields additionalFields = EventTemplateAdditionalFields
                .newBuilder()
                .setAdditionalFields(new KeyValuePair[]{
                        new KeyValuePair("service.name", "benchmark")
                })
                .build();
        return JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplateUri("classpath:EcsLayout.json")
                .setEventTemplateAdditionalFields(additionalFields)
                .setMaxByteCount(LOGSTASH_LAYOUT_MAX_BYTE_COUNT)
                .build();
    }

    private static JsonTemplateLayout createJsonTemplateLayout4GelfLayout() {
        return JsonTemplateLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplateUri("classpath:GelfLayout.json")
                .setMaxByteCount(LOGSTASH_LAYOUT_MAX_BYTE_COUNT)
                .setEventTemplateAdditionalFields(EventTemplateAdditionalFields
                        .newBuilder()
                        .setAdditionalFields(new KeyValuePair[]{
                                // Adding "host" as a constant rather than using
                                // the "hostName" property lookup at runtime, which
                                // is what GelfLayout does as well.
                                new KeyValuePair("host", NetUtils.getLocalHostname())
                        })
                        .build())
                .build();
    }

    private static JsonLayout createDefaultJsonLayout() {
        return JsonLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .build();
    }

    private static JsonLayout createCustomJsonLayout() {
        return JsonLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setAdditionalFields(new KeyValuePair[]{
                        new KeyValuePair("@version", "1")
                })
                .build();
    }

    private static EcsLayout createEcsLayout() {
        return EcsLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setServiceName("benchmark")
                .build();
    }

    private static GelfLayout createGelfLayout() {
        return GelfLayout
                .newBuilder()
                .setConfiguration(CONFIGURATION)
                .setCompressionType(GelfLayout.CompressionType.OFF)
                .build();
    }

    ByteBufferDestination getByteBufferDestination() {
        return byteBufferDestination;
    }

    JsonTemplateLayout getJsonTemplateLayout4JsonLayout() {
        return jsonTemplateLayout4JsonLayout;
    }

    JsonTemplateLayout getJsonTemplateLayout4EcsLayout() {
        return jsonTemplateLayout4EcsLayout;
    }

    JsonTemplateLayout getJsonTemplateLayout4GelfLayout() {
        return jsonTemplateLayout4GelfLayout;
    }

    JsonLayout getDefaultJsonLayout() {
        return defaultJsonLayout;
    }

    JsonLayout getCustomJsonLayout() {
        return customJsonLayout;
    }

    EcsLayout getEcsLayout() {
        return ecsLayout;
    }

    GelfLayout getGelfLayout() {
        return gelfLayout;
    }

    List<LogEvent> getFullLogEvents() {
        return fullLogEvents;
    }

    List<LogEvent> getLiteLogEvents() {
        return liteLogEvents;
    }

}
