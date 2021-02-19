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
package org.apache.logging.log4j.core.lookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests MainLookup.
 */
public class MainLookupTest {

    @Test
    public void testMainArgs(){
        MainMapLookup.setMainArguments("--file", "foo.txt", "--verbose", "-x", "bar");
        String str ="${key} ${main:-1} ${main:0} ${main:1} ${main:2} ${main:3} ${main:4} ${main:\\--file} ${main:foo.txt} ${main:\\--verbose} ${main:\\-x} ${main:bar} ${main:\\--quiet:-true}";
        Map<String, String> properties =  new HashMap<String, String>();
        properties.put("key", "value");
        properties.put("bar", "default_bar_value");
        Interpolator lookup = new Interpolator(properties);
        StrSubstitutor substitutor = new StrSubstitutor(lookup);
        String replacedValue = substitutor.replace(null, str);
        String[] values = replacedValue.split(" ");
        assertThat(values[0]).describedAs("Item 0 is incorrect ").isEqualTo("value");
        assertThat(values[1]).describedAs("Item 1 is incorrect ").isEqualTo("1");
        assertThat(values[2]).describedAs("Item 2 is incorrect").isEqualTo("--file");
        assertThat(values[3]).describedAs("Item 3 is incorrect").isEqualTo("foo.txt");
        assertThat(values[4]).describedAs("Item 4 is incorrect").isEqualTo("--verbose");
        assertThat(values[5]).describedAs("Item 5 is incorrect").isEqualTo("-x");
        assertThat(values[6]).describedAs("Iten 6 is incorrect").isEqualTo("bar");
        assertThat(values[7]).describedAs("Item 7 is incorrect").isEqualTo("foo.txt");
        assertThat(values[8]).describedAs("Item 8 is incorrect").isEqualTo("--verbose");
        assertThat(values[9]).describedAs("Item 9 is incorrect").isEqualTo("-x");
        assertThat(values[10]).describedAs("Item 10 is incorrect").isEqualTo("bar");
        assertThat(values[11]).describedAs("Item 11 is incorrect").isEqualTo("default_bar_value");
        assertThat(values[12]).describedAs("Item 12 is incorrect").isEqualTo("true");
    }
}
