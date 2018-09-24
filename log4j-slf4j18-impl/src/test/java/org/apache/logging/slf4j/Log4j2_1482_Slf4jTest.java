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

package org.apache.logging.slf4j;

import org.apache.logging.log4j.core.layout.Log4j2_1482_Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests https://issues.apache.org/jira/browse/LOG4J2-1482
 */
public class Log4j2_1482_Slf4jTest extends Log4j2_1482_Test {

	@Override
	protected void log(final int runNumber) {
		if (runNumber == 2) {
			// System.out.println("Set a breakpoint here.");
		}
		final Logger logger = LoggerFactory.getLogger("auditcsvfile");
		final int val1 = 9, val2 = 11, val3 = 12;
		logger.info("Info Message!", val1, val2, val3);
		logger.info("Info Message!", val1, val2, val3);
		logger.info("Info Message!", val1, val2, val3);
	}

}