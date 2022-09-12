<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:pom="http://maven.apache.org/POM/4.0.0" xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xalan="http://xml.apache.org/xslt" exclude-result-prefixes="pom xalan">
  <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="2" />
  <xsl:template match="pom:dependencies">
    <dependencies>
      <xsl:apply-templates select="pom:dependency[pom:scope = 'import']">
        <xsl:sort select="concat(pom:artifactId, ' ', pom:groupId)" />
      </xsl:apply-templates>
      <xsl:apply-templates select="pom:dependency[pom:scope = 'provided']">
        <xsl:sort select="concat(pom:artifactId, ' ', pom:groupId)" />
      </xsl:apply-templates>
      <xsl:apply-templates select="pom:dependency[not(pom:scope) or pom:scope = 'compile']">
        <xsl:sort select="concat(pom:artifactId, ' ', pom:groupId)" />
      </xsl:apply-templates>
      <xsl:apply-templates select="pom:dependency[pom:scope = 'runtime']">
        <xsl:sort select="concat(pom:artifactId, ' ', pom:groupId)" />
      </xsl:apply-templates>
      <xsl:apply-templates select="pom:dependency[pom:scope = 'test']">
        <xsl:sort select="concat(pom:artifactId, ' ', pom:groupId)" />
      </xsl:apply-templates>
    </dependencies>
  </xsl:template>
  <xsl:template match="pom:exclusions">
    <exclusions>
      <xsl:apply-templates select="pom:exclusion">
        <xsl:sort select="concat(pom:artifactId, ' ', pom:groupId)" />
      </xsl:apply-templates>
    </exclusions>
  </xsl:template>
  <!-- standard copy template -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>