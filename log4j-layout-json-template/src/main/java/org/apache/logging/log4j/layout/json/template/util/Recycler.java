package org.apache.logging.log4j.layout.json.template.util;

public interface Recycler<V> {

    V acquire();

    void release(V value);

}
