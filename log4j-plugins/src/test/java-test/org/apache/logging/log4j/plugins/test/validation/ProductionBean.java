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
package org.apache.logging.log4j.plugins.test.validation;

import org.apache.logging.log4j.plugins.di.Disposes;
import org.apache.logging.log4j.plugins.di.Inject;
import org.apache.logging.log4j.plugins.di.Produces;
import org.apache.logging.log4j.plugins.di.Provider;

public class ProductionBean {
    private final String alpha;

    private ProductionBean(final String alpha) {
        this.alpha = alpha;
    }

    public String getAlpha() {
        return alpha;
    }

    @Produces
    public static final String ALPHA = "hello world";

    public static void destroyBean(@Disposes ProductionBean bean) {
        System.out.println(bean.getAlpha());
    }

    public static class Builder implements Provider<ProductionBean> {
        private String alpha;

        @Inject
        public Builder setAlpha(String alpha) {
            this.alpha = alpha;
            return this;
        }

        @Override
        public ProductionBean get() {
            return new ProductionBean(alpha);
        }
    }
}
