<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements. See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache license, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License. You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the license for the specific language governing permissions and
  ~ limitations under the license.
  -->
<!--
  To sort a POM file use:
      java -cp Xalan_bin_distribution_jars org.apache.xalan.xslt.Process -IN pom.xml -OUT pom.xml.out -XSL this.file.xslt
  --> 
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:pom="http://maven.apache.org/POM/4.0.0" xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xalan="http://xml.apache.org/xalan" xmlns:exslt="http://exslt.org/common" exclude-result-prefixes="pom xalan">
  <xsl:output method="xml"
              version="1.0"
              encoding="UTF-8"
              indent="yes"
              xalan:indent-amount="2"
              xalan:line-separator="&#10;"/>
  <xsl:strip-space elements="pom:dependencies pom:exclusions pom:plugins"/>
  <xsl:template name="determine-sort-order">
    <xsl:param name="element" />
    <!-- 1. Order by scope -->
    <xsl:choose>
      <xsl:when test="$element/pom:scope = 'import'">
        <xsl:text>1</xsl:text>
      </xsl:when>
      <xsl:when test="$element/pom:scope = 'provided'">
        <xsl:text>2</xsl:text>
      </xsl:when>
      <xsl:when test="$element/pom:scope = 'system'">
        <xsl:text>3</xsl:text>
      </xsl:when>
      <xsl:when test="$element/pom:scope = 'compile'">
        <xsl:text>4</xsl:text>
      </xsl:when>
      <xsl:when test="$element/pom:scope = 'runtime'">
        <xsl:text>5</xsl:text>
      </xsl:when>
      <xsl:when test="$element/pom:scope = 'test'">
        <xsl:text>6</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>4</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text>:</xsl:text>
    <!-- 2. Put Log4j2 artifacts first -->
    <xsl:choose>
      <xsl:when test="$element/pom:groupId = 'org.apache.logging.log4j'">
        <xsl:text>1</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>2</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text>:</xsl:text>
    <!-- 3. Order by artifact id. -->
    <xsl:value-of select="$element/pom:artifactId" />
    <xsl:text>:</xsl:text>
    <!-- 4. Order by group id. -->
    <xsl:value-of select="$element/pom:groupId" />
  </xsl:template>
  <xsl:template match="pom:dependencies|pom:exclusions|pom:plugins">
    <!-- Augment the nodeset with a 'sort-order' attribute -->
    <xsl:variable name="extended">
      <xsl:for-each select="comment()|pom:dependency|pom:exclusion|pom:plugin">
        <xsl:copy>
          <xsl:attribute name="sort-order">
            <xsl:call-template name="determine-sort-order">
              <xsl:with-param name="element" select="." />
            </xsl:call-template>
          </xsl:attribute>
          <xsl:apply-templates/>
        </xsl:copy>
      </xsl:for-each>
    </xsl:variable>
    <xsl:copy>
      <xsl:apply-templates select="exslt:node-set($extended)/node()[not(self::comment())]">
        <xsl:sort select="@sort-order"/>
      </xsl:apply-templates>
    </xsl:copy>
  </xsl:template>
  <!-- Copy with comments -->
  <xsl:template match="pom:dependency">
    <xsl:variable name="current" select="." />
    <xsl:apply-templates
      select="preceding-sibling::comment()[following-sibling::pom:dependency[1] = $current]" />
    <xsl:copy>
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>
  <xsl:template match="pom:exclusion">
    <xsl:variable name="current" select="." />
    <xsl:apply-templates
      select="preceding-sibling::comment()[following-sibling::pom:exclusion[1] = $current]" />
    <xsl:copy>
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>
  <xsl:template match="pom:plugin">
    <xsl:variable name="current" select="." />
    <xsl:apply-templates
      select="preceding-sibling::comment()[following-sibling::pom:plugin[1] = $current]" />
    <xsl:copy>
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>
  <!-- Fixes the license formatting -->
  <xsl:template match="/">
    <xsl:text>&#10;</xsl:text>
    <xsl:copy-of select="comment()"/>
    <xsl:text>&#10;</xsl:text>
    <xsl:apply-templates select="pom:project"/>
  </xsl:template>
  <!-- standard copy template -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
