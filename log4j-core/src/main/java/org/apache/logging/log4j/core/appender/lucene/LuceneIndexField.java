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
package org.apache.logging.log4j.core.appender.lucene;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * {@link LuceneAppender}'s configuration element that logs the log event attributes to the Field in the Lucene document.
 */
@Plugin(name="IndexField", category=Node.CATEGORY, printObject=true)
public final class LuceneIndexField {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final String name;
    private final PatternLayout layout;
    private final String type;

    private LuceneIndexField(final String columnName, final PatternLayout layout,final String type) {
        this.name = columnName;
        this.layout = layout;
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public PatternLayout getLayout() {
        return this.layout;
    }


	public String getType() {
		return type;
	}

	@Override
    public String toString() {
        return "{name=" + this.name + ", layout=" + this.layout + " }";
    }

    /**
     * Factory method for creating a LuceneIndexField config within the plugin manager.
     *
     * @param config The configuration object
     * @param name The name of the index field as it exists within the lucene document.
     * @param pattern The {@link PatternLayout} pattern to insert in this indexfield. Mutually exclusive with
     *                {@code value!=null} and {@code isEventDate=true}
     * @return the created indexField config.
     */
    @PluginFactory
    public static LuceneIndexField createIndexFieldConfig(
            @PluginConfiguration final Configuration config,
            @PluginAttribute("name") final String name,
            @PluginAttribute("pattern") final String pattern,
            @PluginAttribute("type") final String type) {
        if (Strings.isEmpty(name) || Strings.isEmpty(pattern)) {
            LOGGER.error("IndexField标签必须包含name和pattern属性。");
            return null;
        }

        final PatternLayout layout = PatternLayout.newBuilder()
        		.withPattern(pattern)
        		.withConfiguration(config)
        		.withAlwaysWriteExceptions(false)
        		.build();
        return new LuceneIndexField(name,layout,type);
    }
}
