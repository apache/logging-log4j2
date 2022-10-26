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
package org.apache.logging.log4j.migration.jcl;

import org.apache.logging.log4j.migration.Log4j2Types;
import org.objectweb.asm.commons.Remapper;

public class JclRemapper extends Remapper {

    @Override
    public String map(String internalName) {
        switch (internalName) {
            case JclTypes.LOG:
                return Log4j2Types.LOGGER;
            case JclTypes.LOGFACTORY:
                // the exception LogFactory.getFactory() is dealt with in JclMethodRemapper
                return Log4j2Types.LOGGERCONTEXT;
        }
        return super.map(internalName);
    }

}
