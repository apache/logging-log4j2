<!-- SPDX-License-Identifier: Apache-2.0 -->
# Gradle metadata consumer experiment

With JDK 17 and Gradle 8.14.6, run from the repository root:

```bash
./mvnw -pl :log4j-iostreams,:log4j-jul,:log4j-slf4j-impl -am -DskipTests package
gradle -p src/it/gradle-metadata --refresh-dependencies --rerun-tasks check
```

The fixture checks generated metadata, compiles strict classpath and modular consumers, and writes the resolved compile/runtime graphs to `target/dependency-graph.json` under the fixture directory. It runs separately from Maven verification.

For comparison, `-Plog4jRepository=/path/to/repository` selects a previously captured publication, and `-Pbaseline` expects the original five direct annotation dependencies. `-Plog4jVersion=...` selects another producer version.
