package org.apache.logging.log4j.layout.json.template.util;

import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface RecyclerFactory {

    <V> Recycler<V> create(Supplier<V> supplier, Function<V, V> cleaner);

}
