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
package org.apache.logging.log4j.perf.util;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.pmw.tinylog.Configuration;
import org.pmw.tinylog.LogEntry;
import org.pmw.tinylog.writers.LogEntryValue;
import org.pmw.tinylog.writers.PropertiesSupport;
import org.pmw.tinylog.writers.Property;
import org.pmw.tinylog.writers.Writer;

/**
 * tinylog no-op writer.
 */
@PropertiesSupport(name = "no-op", properties = {
		@Property(name = "location", type = boolean.class, optional = true)
	})
public class NoOpTinylogWriter implements Writer {
    public AtomicLong count = new AtomicLong();
    
    boolean location;
    
    public NoOpTinylogWriter() {
    	this.location = false;
	}
	
	public NoOpTinylogWriter(boolean location) {
		this.location = location;
	}

	@Override
	public Set<LogEntryValue> getRequiredLogEntryValues() {
		if (location) {
			return EnumSet.of(LogEntryValue.CLASS, LogEntryValue.METHOD, LogEntryValue.LINE);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public void init(Configuration configuration) {
		
	}

	@Override
	public void write(LogEntry logEntry) {
		count.incrementAndGet();
	}

	@Override
	public void flush() {
		
	}

	@Override
	public void close() {
		
	}
}
