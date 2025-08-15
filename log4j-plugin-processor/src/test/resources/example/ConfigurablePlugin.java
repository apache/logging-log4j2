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
package example;

import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginElement;

@Configurable
@Plugin("configurable")
public class ConfigurablePlugin {
    private final ValidatingPlugin alpha;
    private final ValidatingPluginWithGenericBuilder beta;
    private final ValidatingPluginWithTypedBuilder gamma;
    private final PluginWithGenericSubclassFoo1Builder delta;

    @Inject
    public ConfigurablePlugin(
            @PluginElement final ValidatingPlugin alpha,
            @PluginElement final ValidatingPluginWithGenericBuilder beta,
            @PluginElement final ValidatingPluginWithTypedBuilder gamma,
            @PluginElement final PluginWithGenericSubclassFoo1Builder delta) {
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
        this.delta = delta;
    }

    public String getAlphaName() {
        return alpha.getName();
    }

    public String getBetaName() {
        return beta.getName();
    }

    public String getGammaName() {
        return gamma.getName();
    }

    public String getDeltaThing() {
        return delta.getThing();
    }

    public String getDeltaName() {
        return delta.getFoo1();
    }
}
