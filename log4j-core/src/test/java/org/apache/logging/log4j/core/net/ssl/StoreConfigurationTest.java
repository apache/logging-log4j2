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
package org.apache.logging.log4j.core.net.ssl;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class StoreConfigurationTest<T extends StoreConfiguration<?>> {

    @Test
    public void equalsWithNotNullValues() {
        final String location = "/to/the/file.jks";
        final String password = "changeit";
        final StoreConfiguration<Object> a = new StoreConfiguration<>(location, password);
        final StoreConfiguration<Object> b = new StoreConfiguration<>(location, password);

        Assert.assertTrue(a.equals(b));
        Assert.assertTrue(b.equals(a));
    }

    @Test
    public void equalsWithNullAndNotNullValues() {
        final String location = "/to/the/file.jks";
        final String password = "changeit";
        final StoreConfiguration<Object> a = new StoreConfiguration<>(location, password);
        final StoreConfiguration<Object> b = new StoreConfiguration<>(null, null);

        Assert.assertTrue(a.equals(b));
        Assert.assertTrue(b.equals(a));
    }

    @Test
    public void equalsWithNullValues() {
        final StoreConfiguration<Object> a = new StoreConfiguration<>(null, null);
        final StoreConfiguration<Object> b = new StoreConfiguration<>(null, null);

        Assert.assertTrue(a.equals(b));
        Assert.assertTrue(b.equals(a));
    }
}
