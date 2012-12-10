/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

package org.apache.logging.log4j.core.util;

/**
 * This class switches MDC values into the order
 * (unreasonably) expected by the witness files.
 */
public class MDCOrderFilter implements Filter {

    /**
     * Unexpected orders of keys.
     * Note expected values are "va-one-one" and "va-one-two".
     */
  private static final String[] patterns =
          new String[] {
                  "{key2,va12}{key1,va11}",
                  "{key2,value2}{key1,value1}"
          };

    /**
     * Replacement values.
     */
  private static final String[] replacements =
            new String[] {
                    "{key1,va11}{key2,va12}",
                    "{key1,value1}{key2,value2}"
            };

  /**
   *  Switch order of MDC keys when not in expected order.
   */
  public String filter(final String in) {
    if (in == null) {
      return null;
    }

    for(int i = 0; i < patterns.length; i++) {
        final int ipos = in.indexOf(patterns[i]);
        if (ipos >= 1) {
            return in.substring(0, ipos)
                    + replacements[i]
                    + in.substring(ipos + patterns[i].length());
        }
    }
    return in;
  }
}
