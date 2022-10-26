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
package org.apache.logging.log4j.migration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Converter {

	/**
	 * @param filename file to convert
	 * @return {@code true} if this converter can convert the file
	 */
	boolean accepts(String filename);

	/**
	 * Copies the source to the destination, converting it if necessary, according
	 * to the requirements of the given profile.
	 *
	 * @param path The path to the data being converted
	 * @param src  The source data to convert
	 * @param dest The destination to write the converted data
	 *
	 * @throws IOException If the conversion fails
	 */
	void convert(String path, InputStream src, OutputStream dest) throws IOException;
}
