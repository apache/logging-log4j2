package org.apache.logging.log4j.plugins.convert.plugins;

import org.apache.logging.log4j.plugins.processor.PluginEntry;
import org.apache.logging.log4j.plugins.processor.PluginService;

public class Log4jPlugins extends PluginService {

    private static PluginEntry[] entries = new PluginEntry[] {
        new PluginEntry("bigdecimal", "org.apache.logging.log4j.plugins.convert.TypeConverters$BigDecimalConverter", "BigDecimal", false, false, "TypeConverter"),
        new PluginEntry("biginteger", "org.apache.logging.log4j.plugins.convert.TypeConverters$BigIntegerConverter", "BigInteger", false, false, "TypeConverter"),
        new PluginEntry("boolean", "org.apache.logging.log4j.plugins.convert.TypeConverters$BooleanConverter", "Boolean", false, false, "TypeConverter"),
        new PluginEntry("bytearray", "org.apache.logging.log4j.plugins.convert.TypeConverters$ByteArrayConverter", "ByteArray", false, false, "TypeConverter"),
        new PluginEntry("byte", "org.apache.logging.log4j.plugins.convert.TypeConverters$ByteConverter", "Byte", false, false, "TypeConverter"),
        new PluginEntry("character", "org.apache.logging.log4j.plugins.convert.TypeConverters$CharacterConverter", "Character", false, false, "TypeConverter"),
        new PluginEntry("characterarray", "org.apache.logging.log4j.plugins.convert.TypeConverters$CharArrayConverter", "CharacterArray", false, false, "TypeConverter"),
        new PluginEntry("charset", "org.apache.logging.log4j.plugins.convert.TypeConverters$CharsetConverter", "Charset", false, false, "TypeConverter"),
        new PluginEntry("class", "org.apache.logging.log4j.plugins.convert.TypeConverters$ClassConverter", "Class", false, false, "TypeConverter"),
        new PluginEntry("double", "org.apache.logging.log4j.plugins.convert.TypeConverters$DoubleConverter", "Double", false, false, "TypeConverter"),
        new PluginEntry("file", "org.apache.logging.log4j.plugins.convert.TypeConverters$FileConverter", "File", false, false, "TypeConverter"),
        new PluginEntry("float", "org.apache.logging.log4j.plugins.convert.TypeConverters$FloatConverter", "Float", false, false, "TypeConverter"),
        new PluginEntry("inetaddress", "org.apache.logging.log4j.plugins.convert.TypeConverters$InetAddressConverter", "InetAddress", false, false, "TypeConverter"),
        new PluginEntry("integer", "org.apache.logging.log4j.plugins.convert.TypeConverters$IntegerConverter", "Integer", false, false, "TypeConverter"),
        new PluginEntry("level", "org.apache.logging.log4j.plugins.convert.TypeConverters$LevelConverter", "Level", false, false, "TypeConverter"),
        new PluginEntry("long", "org.apache.logging.log4j.plugins.convert.TypeConverters$LongConverter", "Long", false, false, "TypeConverter"),
        new PluginEntry("path", "org.apache.logging.log4j.plugins.convert.TypeConverters$PathConverter", "Path", false, false, "TypeConverter"),
        new PluginEntry("pattern", "org.apache.logging.log4j.plugins.convert.TypeConverters$PatternConverter", "Pattern", false, false, "TypeConverter"),
        new PluginEntry("securityprovider", "org.apache.logging.log4j.plugins.convert.TypeConverters$SecurityProviderConverter", "SecurityProvider", false, false, "TypeConverter"),
        new PluginEntry("short", "org.apache.logging.log4j.plugins.convert.TypeConverters$ShortConverter", "Short", false, false, "TypeConverter"),
        new PluginEntry("string", "org.apache.logging.log4j.plugins.convert.TypeConverters$StringConverter", "String", false, false, "TypeConverter"),
        new PluginEntry("uri", "org.apache.logging.log4j.plugins.convert.TypeConverters$UriConverter", "URI", false, false, "TypeConverter"),
        new PluginEntry("url", "org.apache.logging.log4j.plugins.convert.TypeConverters$UrlConverter", "URL", false, false, "TypeConverter"),
        new PluginEntry("uuid", "org.apache.logging.log4j.plugins.convert.TypeConverters$UuidConverter", "UUID", false, false, "TypeConverter")
    };
    @Override
    public PluginEntry[] getEntries() { return entries;}
}
