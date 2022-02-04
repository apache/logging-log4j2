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

import java.util.Map;
import org.apache.logging.log4j.plugins.Named;

public class ImplicitMethodBean {
    private String alpha;
    private int beta;
    private Map<String, String> gamma;

    public String getAlpha() {
        return alpha;
    }

    public ImplicitMethodBean setAlpha(@Named final String alpha) {
        this.alpha = alpha;
        return this;
    }

    public int getBeta() {
        return beta;
    }

    public ImplicitMethodBean setBeta(@Named final int beta) {
        this.beta = beta;
        return this;
    }

    public Map<String, String> getGamma() {
        return gamma;
    }

    public ImplicitMethodBean setGamma(@Named final Map<String, String> gamma) {
        this.gamma = gamma;
        return this;
    }
}
