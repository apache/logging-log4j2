package io.github.ashr123.logging;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.Loader;

@Plugin(name = "Json3ConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(50)
public class Json3ConfigurationFactory extends ConfigurationFactory {

	/**
	 * The file extensions supported by this factory.
	 */
	private static final String[] SUFFIXES = {".json", ".jsn"};

	private static final String[] dependencies = {
			"tools.jackson.databind.json.JsonMapper",
			"tools.jackson.databind.JsonNode"
	};

	private final boolean isActive;

	public Json3ConfigurationFactory() {
		for (final String dependency : dependencies) {
			if (!Loader.isClassAvailable(dependency)) {
				LOGGER.debug(
						"Missing dependencies for Json support, ConfigurationFactory {} is inactive",
						getClass().getName()
				);
				isActive = false;
				return;
			}
		}
		isActive = true;
	}

	@Override
	protected boolean isActive() {
		return isActive;
	}

	@Override
	public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
		return isActive
				? new Json3Configuration(loggerContext, source)
				: null;
	}

	@Override
	public String[] getSupportedTypes() {
		return SUFFIXES;
	}
}
