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


/**
 *
 * Encapsulates the data needed for padding strings.
 */
class PaddingSpec {

  /** The char to use for padding */
  private final char paddingchar;

  /** The length to which to pad the string */
  private final int length;


  /**
   * Creates a new PaddingSpec with the given values.
   *
   * @param paddingchar the char to use for padding
   * @param length the length to which to pad the string
   */
  public PaddingSpec(final char paddingchar, final int length) {
    this.paddingchar = paddingchar;
    this.length = length;
  }


  public char getPaddingchar() {
    return paddingchar;
  }


  public int getLength() {
    return length;
  }


  /**
   * Applies this padding to an integer value.
   *
   * @param i the integer value to pad
   * @return a padded string for the given integer value
   */
  public String apply(final int i) {
    //FIXME: This is currently very inefficient and should be reworked.
    return this.apply(String.valueOf(i));
  }


  /**
   * Applies this padding to a string value.
   *
   * @param s the string value to pad
   * @return a padded string for the given string value
   */
  public String apply(final String s) {
    //FIXME: This is currently very inefficient and should be reworked.
    if (s.length() >= length) {
        return s;
    }
    final StringBuilder sb = new StringBuilder();
    while (sb.length() < length - s.length()) {
        sb.append('0');
    }
    sb.append(s);

    return sb.toString();
  }

}
