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
package org.apache.logging.log4j;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class Marker implements Serializable {

  private static ConcurrentMap<String, Marker> markerMap = new ConcurrentHashMap<String, Marker>();

  public static Marker getMarker(String name) {
    return markerMap.putIfAbsent(name, new Marker(name));
  }

  public static Marker getMarker(String name, String parent) {
    Marker parentMarker = markerMap.get(parent);
    if (parentMarker == null) {
      throw new IllegalArgumentException("Parent Marker " + parent + " has not been defined");
    }
    return getMarker(name, parentMarker);
  }

  public static Marker getMarker(String name, Marker parent) {
    return markerMap.putIfAbsent(name, new Marker(name, parent));
  }

  private String name;
  private Marker parent;

  private Marker(String name) {
    this.name = name;
  }

  private Marker(String name, Marker parent) {
    this.name = name;
    this.parent = parent;
  }

  public String getName() {
    return this.name;
  }

  public Marker getParent() {
    return this.parent;
  }

}
