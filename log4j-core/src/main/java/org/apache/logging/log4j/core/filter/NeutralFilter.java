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

package org.apache.logging.log4j.core.filter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

/**
 * A neutral filter where all filter methods return {@link org.apache.logging.log4j.core.Filter.Result#NEUTRAL}.
 */
public class NeutralFilter extends AbstractFilter {

    public static final NeutralFilter INSTANCE = new NeutralFilter();

    @Override
    public Result filter(LogEvent event) {
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0) {
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1) {
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2) {
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2,
            Object p3) {
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2,
            Object p3, Object p4) {
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2,
            Object p3, Object p4, Object p5) {
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2,
            Object p3, Object p4, Object p5, Object p6) {
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2,
            Object p3, Object p4, Object p5, Object p6, Object p7) {
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2,
            Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2,
            Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        return Result.NEUTRAL;
    }
    
}
