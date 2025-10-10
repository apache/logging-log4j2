package io.github.ashr123.logging;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.core.util.Patterns;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Json3Configuration extends AbstractConfiguration implements Reconfigurable {

	private final List<Json3Configuration.Status> status = new ArrayList<>();
	private JsonNode root = null;

	public Json3Configuration(final LoggerContext loggerContext, final ConfigurationSource configSource) {
		super(loggerContext, configSource);
//		final File configFile = configSource.getFile();
		byte[] buffer;
		try {
			try (final InputStream configStream = configSource.getInputStream()) {
				buffer = toByteArray(configStream);
			}
			final InputStream is = new ByteArrayInputStream(buffer);
			root = new JsonMapper().readTree(is);
			if (root.size() == 1) {
				for (final JsonNode node : root) {
					root = node;
				}
			}
			processAttributes(rootNode, root);
			final StatusConfiguration statusConfig = new StatusConfiguration().withStatus(getDefaultStatus());
			int monitorIntervalSeconds = 0;
			for (final Map.Entry<String, String> entry :
					rootNode.getAttributes().entrySet()) {
				final String key = entry.getKey();
				final String value = getConfigurationStrSubstitutor().replace(entry.getValue());
				// TODO: this duplicates a lot of the XmlConfiguration constructor
				if ("status".equalsIgnoreCase(key)) {
					statusConfig.withStatus(value);
				} else if ("dest".equalsIgnoreCase(key)) {
					statusConfig.withDestination(value);
				} else if ("shutdownHook".equalsIgnoreCase(key)) {
					isShutdownHookEnabled = !"disable".equalsIgnoreCase(value);
				} else if ("shutdownTimeout".equalsIgnoreCase(key)) {
					shutdownTimeoutMillis = Long.parseLong(value);
				} else if ("packages".equalsIgnoreCase(key)) {
					pluginPackages.addAll(Arrays.asList(value.split(Patterns.COMMA_SEPARATOR)));
				} else if ("name".equalsIgnoreCase(key)) {
					setName(value);
				} else if ("monitorInterval".equalsIgnoreCase(key)) {
					monitorIntervalSeconds = Integers.parseInt(value);
				} else if ("advertiser".equalsIgnoreCase(key)) {
					createAdvertiser(value, configSource, buffer, "application/json");
				}
			}
			initializeWatchers(this, configSource, monitorIntervalSeconds);
			statusConfig.initialize();
			if (getName() == null) {
				setName(configSource.getLocation());
			}
		} catch (final Exception ex) {
			LOGGER.error("Error parsing {}", configSource.getLocation(), ex);
		}
	}

	@Override
	public void setup() {
		final List<Node> children = rootNode.getChildren();
		root.propertyStream()
				.forEach(entry -> {
					if (entry.getValue().isObject()) {
						LOGGER.debug("Processing node for object {}", entry.getKey());
						children.add(constructNode(entry.getKey(), rootNode, entry.getValue()));
					} else if (entry.getValue().isArray()) {
						LOGGER.error("Arrays are not supported at the root configuration.");
					}
				});
		LOGGER.debug("Completed parsing configuration");
		if (!status.isEmpty()) {
			for (final Json3Configuration.Status s : status) {
				LOGGER.error("Error processing element {}: {}", s.name(), s.errorType());
			}
		}
	}

	@Override
	public Configuration reconfigure() {
		try {
			final ConfigurationSource source = getConfigurationSource().resetInputStream();
			if (source == null) {
				return null;
			}
			return new Json3Configuration(getLoggerContext(), source);
		} catch (final IOException ex) {
			LOGGER.error("Cannot locate file {}", getConfigurationSource(), ex);
		}
		return null;
	}

	private Node constructNode(final String name, final Node parent, final JsonNode jsonNode) {
		final PluginType<?> type = pluginManager.getPluginType(name);
		final Node node = new Node(parent, name, type);
		processAttributes(node, jsonNode);
		final List<Node> children = node.getChildren();
		jsonNode.propertyStream()
				.forEach(entry -> {
					final JsonNode n = entry.getValue();
					if (n.isArray() || n.isObject()) {
						if (type == null) {
							status.add(new Json3Configuration.Status(name, n, Json3Configuration.ErrorType.CLASS_NOT_FOUND));
						}
						if (n.isArray()) {
							LOGGER.debug("Processing node for array {}", entry.getKey());
							for (int i = 0; i < n.size(); ++i) {
								final String pluginType = getType(n.get(i), entry.getKey());
								final Node item = new Node(
										node,
										entry.getKey(),
										pluginManager.getPluginType(pluginType)
								);
								processAttributes(item, n.get(i));
								if (pluginType.equals(entry.getKey())) {
									LOGGER.debug("Processing {}[{}]", entry.getKey(), i);
								} else {
									LOGGER.debug("Processing {} {}[{}]", pluginType, entry.getKey(), i);
								}
								final List<Node> itemChildren = item.getChildren();
								n.get(i)
										.propertyStream()
										.forEach(itemEntry -> {
											if (itemEntry.getValue().isObject()) {
												LOGGER.debug("Processing node for object {}", itemEntry.getKey());
												itemChildren.add(constructNode(itemEntry.getKey(), item, itemEntry.getValue()));
											} else if (itemEntry.getValue().isArray()) {
												final JsonNode array = itemEntry.getValue();
												final String entryName = itemEntry.getKey();
												LOGGER.debug("Processing array for object {}", entryName);
												for (int j = 0; j < array.size(); ++j) {
													itemChildren.add(constructNode(entryName, item, array.get(j)));
												}
											}
										});

								children.add(item);
							}
						} else {
							LOGGER.debug("Processing node for object {}", entry.getKey());
							children.add(constructNode(entry.getKey(), node, n));
						}
					} else {
						LOGGER.debug("Node {} is of type {}", entry.getKey(), n.getNodeType());
					}
				});

		LOGGER.debug(
				"Returning {} with parent {} of type {}",
				node.getName(),
				node.getParent() == null
						? "null"
						: node.getParent().getName() == null
						? LoggerConfig.ROOT
						: node.getParent().getName(),
				type == null
						? "null"
						: type.getElementName() + ':' + type.getPluginClass()
		);
		return node;
	}

	private static String getType(final JsonNode node, final String name) {
		return node.propertyStream()
				.filter(entry -> "type".equalsIgnoreCase(entry.getKey()) &&
						entry.getValue().isValueNode())
				.findFirst()
				.map(Map.Entry::getValue)
				.map(JsonNode::asString)
				.orElse(name);
	}

	private static void processAttributes(final Node parent, final JsonNode node) {
		final Map<String, String> attrs = parent.getAttributes();
		node.propertyStream()
				.filter(entry -> !"type".equalsIgnoreCase(entry.getKey()) &&
						entry.getValue().isValueNode())
				.forEach(entry -> attrs.put(entry.getKey(), entry.getValue().asString()));
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[location=" + getConfigurationSource() + "]";
	}

	/**
	 * The error that occurred.
	 */
	private enum ErrorType {
		CLASS_NOT_FOUND
	}

	/**
	 * Status for recording errors.
	 */
	private record Status(String name, JsonNode node, ErrorType errorType) {
	}
}
