package org.apache.logging.log4j.core.script;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.core.util.IOUtils;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Container for the language and body of a script file.
 */
@Plugin(name = "ScriptFile", category = Node.CATEGORY, printObject = true)
public class ScriptFile extends AbstractScript {

    private static final Logger logger = StatusLogger.getLogger();

    public ScriptFile(String name, String language, String scriptText) {
        super(name, language, scriptText);
    }

    @PluginFactory
    public static ScriptFile createScript(
            // @formatter:off
            @PluginAttribute("name") final String name,
            @PluginAttribute("language") String language, 
            @PluginAttribute("path") final String filePathOrUri,
            @PluginAttribute("charset") final Charset charset) {
            // @formatter:on
        if (language == null) {
            logger.info("No script language supplied, defaulting to {}", DEFAULT_LANGUAGE);
            language = DEFAULT_LANGUAGE;
        }
        if (filePathOrUri == null) {
            logger.error("No script path provided for ScriptFile {}", name);
            return null;
        }
        final Charset actualCharset = charset == null ? Charset.defaultCharset() : charset;
        final URI uri = NetUtils.toURI(filePathOrUri);
        final File file = FileUtils.fileFromUri(uri);
        String scriptText;
        try (final Reader reader = file != null ? new FileReader(file)
                : new InputStreamReader(uri.toURL().openStream(), actualCharset)) {
            scriptText = IOUtils.toString(reader);
        } catch (IOException e) {
            logger.error("{}: name={}, language={}, path={}, actualCharset={}", e.getClass().getSimpleName(), name,
                    language, filePathOrUri, actualCharset);
            return null;
        }
        return new ScriptFile(name, language, scriptText);

    }
}
