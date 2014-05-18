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
package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.Constants;

/**
 * Formats a line separator.
 */
@Plugin(name = "LineSeparatorPatternConverter", category = "Converter")
@ConverterKeys({"n" })
public final class LineSeparatorPatternConverter extends LogEventPatternConverter {

  /**
   * Singleton.
   */
  private static final LineSeparatorPatternConverter INSTANCE =
    new LineSeparatorPatternConverter();

  /**
   * Line separator.
   */
  private final String lineSep;

  /**
   * Private constructor.
   */
  private LineSeparatorPatternConverter() {
    super("Line Sep", "lineSep");
    lineSep = Constants.LINE_SEPARATOR;
  }

  /**
   * Obtains an instance of pattern converter.
   * @param options options, may be null.
   * @return instance of pattern converter.
   */
  public static LineSeparatorPatternConverter newInstance(final String[] options) {
    return INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
public void format(final LogEvent event, final StringBuilder toAppendTo) {
    toAppendTo.append(lineSep);
  }
}
