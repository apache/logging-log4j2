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
  To add external references run
      java -cp Xalan_bin_distribution_jars org.apache.xalan.xslt.Process \
        -IN pom.xml \
        -OUT pom.xml.out \
        -XSL this.file.xslt \
        -PARAM sbom.serialNumber e87ab1a5-3d29-48d5-82fa-211b7e913851
        -PARAM vdr.serialNumber 2496f0fa-91af-48cc-869f-ef1e03c97018
        -PARAM vdr.url https://logging.apache.org/log4j/vulnerabilities
  --> 
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://cyclonedx.org/schema/bom/1.5"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:cdx14="http://cyclonedx.org/schema/bom/1.4"
                xmlns:cdx15="http://cyclonedx.org/schema/bom/1.5"
                exclude-result-prefixes="xalan cdx14 cdx15">
  <xsl:param name="sbom.serialNumber"/>
  <xsl:param name="vdr.serialNumber"/>
  <xsl:param name="vdr.url"/>
  <xsl:output method="xml"
              version="1.0"
              encoding="UTF-8"
              indent="yes"
              xalan:indent-amount="2"
              xalan:line-separator="&#10;"/>
  <!-- Fixes the license formatting -->
  <xsl:template match="/">
    <xsl:text>&#10;</xsl:text>
    <xsl:apply-templates />
  </xsl:template>
  <!-- Standard copy template -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>
  <xsl:template match="cdx14:*">
    <xsl:element name="{local-name()}" namespace="http://cyclonedx.org/schema/bom/1.5">
      <xsl:apply-templates select="@*" />
      <xsl:apply-templates />
    </xsl:element>
  </xsl:template>
  <!-- Main element -->
  <xsl:template match="cdx14:bom">
    <bom>
      <xsl:attribute name="version">
        <xsl:value-of select="1"/>
      </xsl:attribute>
      <xsl:attribute name="serialNumber">
        <xsl:value-of select="$sbom.serialNumber"/>
      </xsl:attribute>
      <xsl:apply-templates select="cdx14:metadata|cdx14:components"/>
      <externalReferences>
        <reference>
          <xsl:attribute name="type">vulnerability-assertion</xsl:attribute>
          <url>
            <xsl:text>urn:cdx:</xsl:text>
            <xsl:value-of select="$vdr.serialNumber"/>
          </url>
        </reference>
        <reference>
          <xsl:attribute name="type">vulnerability-assertion</xsl:attribute>
          <url>
            <xsl:value-of select="$vdr.url"/>
          </url>
        </reference>
      </externalReferences>
      <xsl:apply-templates select="cdx14:dependencies"/>
    </bom>
  </xsl:template>
  <xsl:template match="cdx14:externalReferences[preceding-sibling::cdx14:group/text() = 'org.apache.logging.log4j']">
    <xsl:apply-templates/>
    <reference>
      <xsl:attribute name="type">vulnerability-assertion</xsl:attribute>
      <url>
        <xsl:text>urn:cdx:</xsl:text>
        <xsl:value-of select="$vdr.serialNumber"/>
      </url>
    </reference>
    <reference>
      <xsl:attribute name="type">vulnerability-assertion</xsl:attribute>
      <url>
        <xsl:value-of select="$vdr.url"/>
      </url>
    </reference>
  </xsl:template>
</xsl:stylesheet>
