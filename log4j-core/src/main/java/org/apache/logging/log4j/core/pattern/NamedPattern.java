package org.apache.logging.log4j.core.pattern;

@SuppressWarnings("SpellCheckingInspection")
public enum NamedPattern {
    ABSOLUTE("HH:mm:ss,SSS", "HH:mm:ss,SSS"),
    ABSOLUTE_MICROS("HH:mm:ss,nnnnnn", "HH:mm:ss,SSSSSS"),
    ABSOLUTE_NANOS("HH:mm:ss,nnnnnnnnn", "HH:mm:ss,SSSSSSSSS"),
    ABSOLUTE_PERIOD("HH:mm:ss.SSS", "HH:mm:ss.SSS"),
    COMPACT("yyyyMMddHHmmssSSS", "yyyyMMddHHmmssSSS"),
    DATE("dd MMM yyyy HH:mm:ss,SSS", "dd MMM yyyy HH:mm:ss,SSS"),
    DATE_PERIOD("dd MMM yyyy HH:mm:ss.SSS", "dd MMM yyyy HH:mm:ss.SSS"),
    DEFAULT("yyyy-MM-dd HH:mm:ss,SSS", "yyyy-MM-dd HH:mm:ss,SSS"),
    DEFAULT_MICROS("yyyy-MM-dd HH:mm:ss,nnnnnn", "yyyy-MM-dd HH:mm:ss,SSSSSS"),
    DEFAULT_NANOS("yyyy-MM-dd HH:mm:ss,nnnnnnnnn", "yyyy-MM-dd HH:mm:ss,SSSSSSSSS"),
    DEFAULT_PERIOD("yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss.SSS"),
    ISO8601_BASIC("yyyyMMdd'T'HHmmss,SSS", "yyyyMMdd'T'HHmmss,SSS"),
    ISO8601_BASIC_PERIOD("yyyyMMdd'T'HHmmss.SSS", "yyyyMMdd'T'HHmmss.SSS"),
    ISO8601("yyyy-MM-dd'T'HH:mm:ss,SSS", "yyyy-MM-dd'T'HH:mm:ss,SSS"),
    ISO8601_OFFSET_DATE_TIME_HH("yyyy-MM-dd'T'HH:mm:ss,SSSX", "yyyy-MM-dd'T'HH:mm:ss,SSSx"),
    ISO8601_OFFSET_DATE_TIME_HHMM("yyyy-MM-dd'T'HH:mm:ss,SSSyyyy-MM-dd'T'HH:mm:ss,SSSXX", "yyyy-MM-dd'T'HH:mm:ss,SSSxx"),
    ISO8601_OFFSET_DATE_TIME_HHCMM("yyyy-MM-dd'T'HH:mm:ss,SSSyyyy-MM-dd'T'HH:mm:ss,SSSXXX", "yyyy-MM-dd'T'HH:mm:ss,SSSxxx"),
    ISO8601_PERIOD("yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss.SSS"),
    ISO8601_PERIOD_MICROS("yyyy-MM-dd'T'HH:mm:ss.nnnnnn", "yyyy-MM-dd'T'HH:mm:ss.SSSSSS"),
    US_MONTH_DAY_YEAR2_TIME("dd/MM/yy HH:mm:ss.SSS", "dd/MM/yy HH:mm:ss.SSS"),
    US_MONTH_DAY_YEAR4_TIME("dd/MM/yyyy HH:mm:ss.SSS", "dd/MM/yyyy HH:mm:ss.SSS");

    private final String
            legacyPattern,
            nonLegacyPattern;

    NamedPattern(String legacyPattern, String nonLegacyPattern) {
        this.legacyPattern = legacyPattern;
        this.nonLegacyPattern = nonLegacyPattern;
    }

    public String getLegacyPattern() {
        return legacyPattern;
    }

    public String getNonLegacyPattern() {
        return nonLegacyPattern;
    }
}
