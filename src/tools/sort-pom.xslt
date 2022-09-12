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