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
package org.apache.logging.log4j.lucene5.appender;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * {@link LuceneAppender}'s configuration element that logs the log event
 * attributes to the Field in the Lucene document.
 */
@Plugin(name = "IndexField", category = Node.CATEGORY, printObject = true)
final class LuceneIndexField {

	public static class Builder<B extends Builder<B>> extends AbstractFilterable.Builder<B>
			implements org.apache.logging.log4j.core.util.Builder<LuceneIndexField> {

		@PluginConfiguration
		private Configuration configuration;

		@PluginBuilderAttribute
		@Required(message = "No name provided")
		private String name;

		@PluginBuilderAttribute
		@Required(message = "No pattern provided")
		private String pattern;

		@PluginBuilderAttribute
		private String type;

		@Override
		public LuceneIndexField build() {
			// @formatter:off
			final PatternLayout layout = PatternLayout.newBuilder()
					.withPattern(pattern)
					.withConfiguration(configuration)
					.withAlwaysWriteExceptions(false)
					.build();
			// @formatter:on
			return new LuceneIndexField(name, layout, type);
		}

		public B withName(final String name) {
			this.name = name;
			return this.asBuilder();
		}

		public B withPattern(final String pattern) {
			this.pattern = pattern;
			return this.asBuilder();
		}

		public B withType(final String type) {
			this.type = type;
			return this.asBuilder();
		}
	}

	@PluginBuilderFactory
	public static <B extends Builder<B>> B newBuilder() {
		return new Builder<B>().asBuilder();
	}

	private final PatternLayout layout;

	private final String name;

	private final String type;

	private LuceneIndexField(final String name, final PatternLayout layout, final String type) {
		this.name = name;
		this.layout = layout;
		this.type = type;
	}

	public PatternLayout getLayout() {
		return this.layout;
	}

	public String getName() {
		return this.name;
	}

	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return "{name=" + this.name + ", layout=" + this.layout + " }";
	}
}
