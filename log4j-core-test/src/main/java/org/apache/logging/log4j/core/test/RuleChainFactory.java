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
package org.apache.logging.log4j.core.test;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

/**
 * Builds JUnit {@link RuleChain}s.
 */
public class RuleChainFactory {

    /**
     * Creates a {@link RuleChain} where the rules are evaluated in the order you pass in.
     *
     * @param testRules
     *            test rules to evaluate
     * @return a new rule chain.
     */
    public static RuleChain create(final TestRule... testRules) {
        if (testRules == null || testRules.length == 0) {
            return RuleChain.emptyRuleChain();
        }
        RuleChain ruleChain = RuleChain.outerRule(testRules[0]);
        for (int i = 1; i < testRules.length; i++) {
            ruleChain = ruleChain.around(testRules[i]);
        }
        return ruleChain;
    }
}
