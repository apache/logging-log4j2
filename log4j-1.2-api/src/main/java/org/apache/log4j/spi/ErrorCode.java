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

package org.apache.log4j.spi;


/**
   This interface defines commonly encoutered error codes.
 */
public interface ErrorCode {

  public final int GENERIC_FAILURE = 0;
  public final int WRITE_FAILURE = 1;
  public final int FLUSH_FAILURE = 2;
  public final int CLOSE_FAILURE = 3;
  public final int FILE_OPEN_FAILURE = 4;
  public final int MISSING_LAYOUT = 5;
  public final int ADDRESS_PARSE_FAILURE = 6;
}
