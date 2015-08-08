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
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;

class LevelLoggerImpl implements LevelLogger {

	private final Level level;

	private final Logger logger;

	LevelLoggerImpl(final Level level, final Logger logger) {
		super();
		this.level = level;
		this.logger = logger;
	}

	@Override
	public void catching(final Throwable t) {
		logger.catching(level, t);
	}

	@Override
	public void entry() {
		logger.entry();
	}

	@Override
	public void entry(final Object... params) {
		logger.entry(params);
	}

	@Override
	public void exit() {
		logger.exit();
	}

	@Override
	public <R> R exit(final R result) {
		return logger.exit(result);
	}

	@Override
	public Level getLevel() {
		return level;
	}

	@Override
	public MessageFactory getMessageFactory() {
		return logger.getMessageFactory();
	}

	@Override
	public String getName() {
		return logger.getName();
	}

	@Override
	public boolean isEnabled() {
		return logger.isEnabled(level);
	}

	@Override
	public boolean isEnabled(final Level level) {
		return this.level.isLessSpecificThan(level);
	}

	@Override
	public boolean isEnabled(final Marker marker) {
		return logger.isEnabled(level, marker);
	}

	@Override
	public void log(final Marker marker, final Message msg) {
		logger.log(level, marker, msg);
	}

	@Override
	public void log(final Marker marker, final Message msg, final Throwable t) {
		logger.log(level, marker, msg, t);
	}

	@Override
	public void log(final Marker marker, final Object message) {
		logger.log(level, marker, message);
	}

	@Override
	public void log(final Marker marker, final Object message, final Throwable t) {
		logger.log(level, marker, message, t);
	}

	@Override
	public void log(final Marker marker, final String message) {
		logger.log(level, marker, message);
	}

	@Override
	public void log(final Marker marker, final String message, final Object... params) {
		logger.log(level, marker, message, params);
	}

	@Override
	public void log(final Marker marker, final String message, final Throwable t) {
		logger.log(level, marker, message, t);
	}

	@Override
	public void log(final Message msg) {
		logger.log(level, msg);
	}

	@Override
	public void log(final Message msg, final Throwable t) {
		logger.log(level, msg, t);
	}

	@Override
	public void log(final Object message) {
		logger.log(level, message);
	}

	@Override
	public void log(final Object message, final Throwable t) {
		logger.log(level, message, t);
	}

	@Override
	public void log(final String message) {
		logger.log(level, message);
	}

	@Override
	public void log(final String message, final Object... params) {
		logger.log(level, message, params);
	}

	@Override
	public void log(final String message, final Throwable t) {
		logger.log(level, message, t);
	}

	@Override
	public void printf(final Marker marker, final String format, final Object... params) {
		logger.printf(level, marker, format, params);
	}

	@Override
	public void printf(final String format, final Object... params) {
		logger.printf(level, format, params);
	}

	@Override
	public <T extends Throwable> T throwing(final T t) {
		return logger.throwing(t);
	}

}
