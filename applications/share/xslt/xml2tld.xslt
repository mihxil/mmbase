<?xml version="1.0"?>
<xsl:stylesheet 
  id="xml2tld"  
  version="1.0"	
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  >

  <xsl:param name="version">1.2</xsl:param>
  <xsl:param name="uri">http://www.mmbase.org/mmbase-taglib-1.0</xsl:param>
  <xsl:output
    omit-xml-declaration="yes"
    method="xml" 
    indent="yes" />

  
  
  <!-- main entry point -->
  <xsl:template match="taglib">
    <xsl:if test="$version = '1.2'">        
        <xsl:text disable-output-escaping="yes">&lt;!DOCTYPE taglib  PUBLIC "-//Sun Microsystems, Inc.//DTD JSP Tag Library 1.2//EN" 
        "http://java.sun.com/dtd/web-jsptaglibrary_1_2.dtd"&gt;
</xsl:text>
        <taglib>
          <xsl:call-template name="taglib"  />
        </taglib>
      </xsl:if>
      <xsl:if test="$version = '2.0'">       
      <taglib  xmlns="http://java.sun.com/xml/ns/j2ee">
        <xsl:attribute namespace="http://www.w3.org/2001/XMLSchema-instance" name="schemaLocation">http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-jsptaglibrary_2_0.xsd</xsl:attribute>
        <xsl:attribute name="version">2.0</xsl:attribute>
        <xsl:call-template name="taglib"  />
        </taglib>
      </xsl:if>
  </xsl:template>
  

  <xsl:template name="taglib">
      <xsl:comment>
        This is the MMBase tag library descriptor (http://www.mmbase.org). This file is automaticly generated by
        xml2tld.xslt.
      </xsl:comment>
      <xsl:apply-templates select="tlibversion | shortname | jspversion " />
      <xsl:if test="not(jspversion)">
        <jspversion><xsl:value-of select="$version" /></jspversion>
      </xsl:if>
      <xsl:apply-templates select="uri" />
      <xsl:if test="not(uri)">
        <uri><xsl:value-of select="$uri" /></uri>
      </xsl:if>
      <xsl:apply-templates select="info"/>
      <xsl:if test="$version = '2.0'">
        <xsl:apply-templates select="function" mode="base" />
      </xsl:if>
      <xsl:apply-templates select="tag" mode="base" />    
  </xsl:template>
  
  <xsl:template match="info">
    <info><xsl:value-of select="." /></info>
  </xsl:template>
  
  <xsl:template match="tag" mode="base" >
    <tag>
      <xsl:apply-templates select="name | tagclass | teiclass | bodycontent | info" />
      <xsl:apply-templates select="extends" />
      <xsl:apply-templates select="attribute"/> 
    </tag>
  </xsl:template>

  <xsl:template match="function" mode="base" >
    <function>
      <xsl:apply-templates select="name | description | function-class | function-signature | example" />
      <xsl:apply-templates select="extends" />
      <xsl:apply-templates select="attribute"/> 
    </function>
  </xsl:template>
  
  <xsl:template match="tag|taginterface" mode="extends">
    <xsl:apply-templates select="extends" />
    <xsl:apply-templates select="attribute"/> 
  </xsl:template>
  
  <xsl:template match="attribute">
    <attribute><xsl:apply-templates select="name | required | rtexprvalue" /></attribute>
  </xsl:template>
  
  <xsl:template match="extends">
    <xsl:apply-templates select="/taglib/*[name()='tag' or name()='taginterface']/name[.=current()]/parent::*" mode="extends" />
  </xsl:template>
  
  <xsl:template match="tlibversion|shortname|jspversion|uri|name|tagclass|teiclass|bodycontent|required|rtexprvalue|example|function-class|function-signature">
    <xsl:copy-of select="." />
  </xsl:template>
  
</xsl:stylesheet>
