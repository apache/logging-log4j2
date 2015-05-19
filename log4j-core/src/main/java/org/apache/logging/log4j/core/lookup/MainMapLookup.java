package org.apache.logging.log4j.core.lookup;

import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;

/**
 * A map-based lookup for main arguments.
 * 
 * See {@link #setMainArguments(String[])}.
 * 
 * @since 2.4
 */
@Plugin(name = "main", category = StrLookup.CATEGORY)
public class MainMapLookup extends MapLookup {

    /**
     * A singleton used by a main method to save its arguments.
     */
    static final MapLookup MAIN_SINGLETON = new MapLookup(MapLookup.newMap(0));

    /**
     * An application's {@code public static main(String[])} method calls this method to make its main arguments
     * available for lookup with the prefix {@code main}.
     * <p>
     * The map provides two kinds of access: First by index, starting at {@code "0"}, {@code "1"} and so on. For
     * example, the command line {@code --file path/file.txt -x 2} can be accessed from a configuration file with:
     * </p>
     * <ul>
     * <li>{@code "main:0"} = {@code "--file"}</li>
     * <li>{@code "main:1"} = {@code "path/file.txt"}</li>
     * <li>{@code "main:2"} = {@code "-x"}</li>
     * <li>{@code "main:3"} = {@code "2"}</li>
     * </ul>
     * <p>
     * Second using the argument at position n as the key to access the value at n+1.
     * </p>
     * <ul>
     * <li>{@code "main:--file"} = {@code "path/file.txt"}</li>
     * <li>{@code "main:-x"} = {@code "2"}</li>
     * </ul>
     *
     * @param args
     *        An application's {@code public static main(String[])} arguments.
     */
    public static void setMainArguments(final String[] args) {
        if (args == null) {
            return;
        }
        initMap(args, MainMapLookup.MAIN_SINGLETON.getMap());
    }

    /**
     * Constructor when used directly as a plugin.
     */
    public MainMapLookup() {
        // no-init
    }

    public MainMapLookup(final Map<String, String> map) {
        super(map);
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return MAIN_SINGLETON.getMap().get(key);
    }

    @Override
    public String lookup(String key) {
        return MAIN_SINGLETON.getMap().get(key);
    }

}
