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

package org.apache.logging.log4j.core.appender.rolling.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class IfFileNameTest {

    @Test
    public void testCreateNameConditionFailsIfBothRegexAndPathAreNull() {
        assertThatThrownBy(() -> IfFileName.createNameCondition(null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testCreateNameConditionAcceptsIfEitherRegexOrPathOrBothAreNonNull() {
        IfFileName.createNameCondition("bar", null);
        IfFileName.createNameCondition(null, "foo");
        IfFileName.createNameCondition("bar", "foo");
    }

    @Test
    public void testGetSyntaxAndPattern() {
        assertThat(IfFileName.createNameCondition("path", null).getSyntaxAndPattern()).isEqualTo("glob:path");
        assertThat(IfFileName.createNameCondition("glob:path", null).getSyntaxAndPattern()).isEqualTo("glob:path");
        assertThat(IfFileName.createNameCondition(null, "bar").getSyntaxAndPattern()).isEqualTo("regex:bar");
        assertThat(IfFileName.createNameCondition(null, "regex:bar").getSyntaxAndPattern()).isEqualTo("regex:bar");
    }

    @Test
    public void testAcceptUsesPathPatternIfExists() {
        final IfFileName filter = IfFileName.createNameCondition("path", "regex");
        final Path relativePath = Paths.get("path");
        assertThat(filter.accept(null, relativePath, null)).isTrue();
        
        final Path pathMatchingRegex = Paths.get("regex");
        assertThat(filter.accept(null, pathMatchingRegex, null)).isFalse();
    }

    @Test
    public void testAcceptUsesRegexIfNoPathPatternExists() {
        final IfFileName regexFilter = IfFileName.createNameCondition(null, "regex");
        final Path pathMatchingRegex = Paths.get("regex");
        assertThat(regexFilter.accept(null, pathMatchingRegex, null)).isTrue();
        
        final Path noMatch = Paths.get("nomatch");
        assertThat(regexFilter.accept(null, noMatch, null)).isFalse();
    }

    @Test
    public void testAcceptIgnoresBasePathAndAttributes() {
        final IfFileName pathFilter = IfFileName.createNameCondition("path", null);
        final Path relativePath = Paths.get("path");
        assertThat(pathFilter.accept(null, relativePath, null)).isTrue();
        
        final IfFileName regexFilter = IfFileName.createNameCondition(null, "regex");
        final Path pathMatchingRegex = Paths.get("regex");
        assertThat(regexFilter.accept(null, pathMatchingRegex, null)).isTrue();
    }

    @Test
    public void testAcceptCallsNestedConditionsOnlyIfPathAccepted1() {
        final CountingCondition counter = new CountingCondition(true);
        final IfFileName regexFilter = IfFileName.createNameCondition(null, "regex", counter);
        final Path pathMatchingRegex = Paths.get("regex");
        
        assertThat(regexFilter.accept(null, pathMatchingRegex, null)).isTrue();
        assertThat(counter.getAcceptCount()).isEqualTo(1);
        assertThat(regexFilter.accept(null, pathMatchingRegex, null)).isTrue();
        assertThat(counter.getAcceptCount()).isEqualTo(2);
        assertThat(regexFilter.accept(null, pathMatchingRegex, null)).isTrue();
        assertThat(counter.getAcceptCount()).isEqualTo(3);
        
        final Path noMatch = Paths.get("nomatch");
        assertThat(regexFilter.accept(null, noMatch, null)).isFalse();
        assertThat(counter.getAcceptCount()).isEqualTo(3); // no increase
        assertThat(regexFilter.accept(null, noMatch, null)).isFalse();
        assertThat(counter.getAcceptCount()).isEqualTo(3);
        assertThat(regexFilter.accept(null, noMatch, null)).isFalse();
        assertThat(counter.getAcceptCount()).isEqualTo(3);
    }

    @Test
    public void testAcceptCallsNestedConditionsOnlyIfPathAccepted2() {
        final CountingCondition counter = new CountingCondition(true);
        final IfFileName globFilter = IfFileName.createNameCondition("glob", null, counter);
        final Path pathMatchingGlob = Paths.get("glob");
        
        assertThat(globFilter.accept(null, pathMatchingGlob, null)).isTrue();
        assertThat(counter.getAcceptCount()).isEqualTo(1);
        assertThat(globFilter.accept(null, pathMatchingGlob, null)).isTrue();
        assertThat(counter.getAcceptCount()).isEqualTo(2);
        assertThat(globFilter.accept(null, pathMatchingGlob, null)).isTrue();
        assertThat(counter.getAcceptCount()).isEqualTo(3);

        final Path noMatch = Paths.get("nomatch");
        assertThat(globFilter.accept(null, noMatch, null)).isFalse();
        assertThat(counter.getAcceptCount()).isEqualTo(3); // no increase
        assertThat(globFilter.accept(null, noMatch, null)).isFalse();
        assertThat(counter.getAcceptCount()).isEqualTo(3);
        assertThat(globFilter.accept(null, noMatch, null)).isFalse();
        assertThat(counter.getAcceptCount()).isEqualTo(3);
    }

    @Test
    public void testBeforeTreeWalk() {
        final CountingCondition counter = new CountingCondition(true);
        final IfFileName pathFilter = IfFileName.createNameCondition("path", null, counter, counter, counter);
        pathFilter.beforeFileTreeWalk();
        assertThat(counter.getBeforeFileTreeWalkCount()).isEqualTo(3);
    }
}
