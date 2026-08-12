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

/**
 * ArchUnit fitness functions for {@code log4j-core} layer and module boundaries.
 *
 * <h2>Approach</h2>
 *
 * <p>These tests analyze compiled production bytecode of {@code org.apache.logging.log4j.core} via
 * ArchUnit. They use baseline thresholds rather than zero-tolerance rules so CI passes on the
 * current codebase while blocking <em>new</em> architectural regressions.
 *
 * <h2>Measured baselines (2026-08-12, {@code feat/forge-modernization-swarm})</h2>
 *
 * <ul>
 *   <li>{@link LayerBoundaryTest}: 57 direct util-to-config dependency edges (ForgeScore estimated
 *       1,214 total cross-layer couplings using broader static metrics; the measured ArchUnit edge
 *       count is authoritative for CI).
 *   <li>{@link CircularDependencyTest}: 100 ArchUnit cycle diagnostic entries among top-level core
 *       packages.
 *   <li>{@link ModuleBoundaryTest}: 1 integration-to-{@code util.internal} dependency edge.
 * </ul>
 *
 * <h2>Baseline trajectory (EPIC-08 / WO-053)</h2>
 *
 * <p>Decrease each baseline constant in the same PR that removes the corresponding dependencies.
 * Never increase a baseline without architecture review.
 */
package org.apache.logging.log4j.core.architecture;
