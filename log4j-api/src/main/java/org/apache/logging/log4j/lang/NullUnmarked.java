/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
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
package org.apache.logging.log4j.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.When;

/**
 * Annotates an element to indicate all its members may be null. When applied to a package, this applies
 * to all classes in the package.
 *
 * @see org.jspecify.annotations.NullUnmarked
 */
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
@TypeQualifierDefault({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_PARAMETER})
@Nonnull(when = When.MAYBE)
@org.jspecify.annotations.NullUnmarked
public @interface NullUnmarked {}
