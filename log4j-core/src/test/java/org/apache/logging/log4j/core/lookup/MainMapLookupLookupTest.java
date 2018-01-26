/*
 * Copyright 2015 Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MainMapLookupLookupTest {

    
    @Test
    public void testBugLOG4J2_2211(){
    	MainMapLookup.setMainArguments(new String[] {"--file", "foo.txt", "--verbose", "-x", "bar"});
    	String str ="${key} ${main:-1} ${main:0} ${main:1} ${main:2} ${main:3} ${main:4} ${main:--file} ${main:foo.txt} ${main:--verbose} ${main:-x} ${main:bar}";
    	Map<String, String> properties =  new HashMap<String, String>();
    	properties.put("key", "value");//<property name="filed">value</property>
    	properties.put("bar", "default_bar_value");//<property name="bar">default_bar_value</property>
    	Interpolator lookup = new Interpolator(properties);
    	StrSubstitutor substitutor = new StrSubstitutor(lookup);
    	String replacedValue = substitutor.replace(null, str);
    	String[] values = replacedValue.split(" ");
    	assertEquals(values[0], "value");
    	assertEquals(values[1], "1");
    	assertEquals(values[2], "--file");
    	assertEquals(values[3], "foo.txt");
    	assertEquals(values[4], "--verbose");
    	assertEquals(values[5], "-x");
    	assertEquals(values[6], "bar");
    	assertEquals(values[7], "foo.txt");
    	assertEquals(values[8], "--verbose");
    	assertEquals(values[9], "-x");
    	assertEquals(values[10], "bar");
    	assertEquals(values[11], "default_bar_value");
    }
    
}
