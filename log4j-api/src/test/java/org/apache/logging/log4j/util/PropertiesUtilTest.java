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

package org.apache.logging.log4j.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

public class PropertiesUtilTest {

    private final Properties properties = new Properties();

    @BeforeEach
    public void setUp() throws Exception {
        properties.load(ClassLoader.getSystemResourceAsStream("PropertiesUtilTest.properties"));
    }

    @Test
    public void testExtractSubset() {
        assertHasAllProperties(PropertiesUtil.extractSubset(properties, "a"));
        assertHasAllProperties(PropertiesUtil.extractSubset(properties, "b."));
        assertHasAllProperties(PropertiesUtil.extractSubset(properties, "c.1"));
        assertHasAllProperties(PropertiesUtil.extractSubset(properties, "dd"));
        assertThat(properties).hasSize(0);
    }

    @Test
    public void testPartitionOnCommonPrefix() {
        final Map<String, Properties> parts = PropertiesUtil.partitionOnCommonPrefixes(properties);
        assertThat(parts).hasSize(4);
        assertHasAllProperties(parts.get("a"));
        assertHasAllProperties(parts.get("b"));
        assertHasAllProperties(PropertiesUtil.partitionOnCommonPrefixes(parts.get("c")).get("1"));
        assertHasAllProperties(parts.get("dd"));
    }

    private static void assertHasAllProperties(final Properties properties) {
        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("1")).isEqualTo("1");
        assertThat(properties.getProperty("2")).isEqualTo("2");
        assertThat(properties.getProperty("3")).isEqualTo("3");
    }


    @Test
    public void testGetCharsetProperty() {
        final Properties p = new Properties();
        p.setProperty("e.1", StandardCharsets.US_ASCII.name());
        p.setProperty("e.2", "wrong-charset-name");
        final PropertiesUtil pu = new PropertiesUtil(p);

        assertThat(pu.getCharsetProperty("e.0")).isEqualTo(Charset.defaultCharset());
        assertThat(pu.getCharsetProperty("e.1")).isEqualTo(StandardCharsets.US_ASCII);
        assertThat(pu.getCharsetProperty("e.2")).isEqualTo(Charset.defaultCharset());
    }
    
    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
    public void testGetMappedProperty_sun_stdout_encoding() {
        final PropertiesUtil pu = new PropertiesUtil(System.getProperties());
        Charset expected = System.console() == null ? Charset.defaultCharset() : StandardCharsets.UTF_8;
        assertThat(pu.getCharsetProperty("sun.stdout.encoding")).isEqualTo(expected);
    }

    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
    public void testGetMappedProperty_sun_stderr_encoding() {
        final PropertiesUtil pu = new PropertiesUtil(System.getProperties());
        Charset expected = System.console() == null ? Charset.defaultCharset() : StandardCharsets.UTF_8;
        assertThat(pu.getCharsetProperty("sun.err.encoding")).isEqualTo(expected);
    }

    @Test
    @ResourceLock(Resources.SYSTEM_PROPERTIES)
    public void testNonStringSystemProperties() {
        Object key1 = "1";
        Object key2 = new Object();
        System.getProperties().put(key1, new Object());
        System.getProperties().put(key2, "value-2");
        try {
            final PropertiesUtil util = new PropertiesUtil(new Properties());
            assertThat(util.getStringProperty("1")).isNull();
        } finally {
            System.getProperties().remove(key1);
            System.getProperties().remove(key2);
        }
    }

    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
    public void testPublish() {
        final Properties props = new Properties();
        final PropertiesUtil util = new PropertiesUtil(props);
        String value = System.getProperty("Application");
        assertThat(value).describedAs("System property was not published").isNotNull();
        assertThat(value).isEqualTo("Log4j");
    }
}
