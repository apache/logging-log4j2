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

package org.apache.logging.log4j.levellogger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

public class LevelLoggers {

	public LevelLogger all;
	public LevelLogger debug;
	public LevelLogger error;
	public LevelLogger fatal;
	public LevelLogger info;
	public LevelLogger off;
	public LevelLogger trace;
	public LevelLogger warn;

	public LevelLoggers(final Logger logger) {
		super();
		all = new LevelLoggerImpl(Level.ALL, logger);
		trace = new LevelLoggerImpl(Level.TRACE, logger);
		debug = new LevelLoggerImpl(Level.DEBUG, logger);
		info = new LevelLoggerImpl(Level.INFO, logger);
		warn = new LevelLoggerImpl(Level.WARN, logger);
		error = new LevelLoggerImpl(Level.ERROR, logger);
		fatal = new LevelLoggerImpl(Level.FATAL, logger);
		off = new LevelLoggerImpl(Level.OFF, logger);
	}

	public LevelLogger all() {
		return all;
	}

	public LevelLogger debug() {
		return debug;
	}

	public LevelLogger error() {
		return error;
	}

	public LevelLogger fatal() {
		return fatal;
	}

	public LevelLogger info() {
		return info;
	}

	public LevelLogger off() {
		return off;
	}

	public LevelLogger trace() {
		return trace;
	}

	public LevelLogger warn() {
		return warn;
	}

}
