<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:srw="http://www.loc.gov/zing/srw/"
  xmlns:diag="http://www.loc.gov/zing/srw/diagnostic/">

<xsl:output method="html" doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN" doctype-system="http://www.w3.org/TR/html4/loose.dtd"/>

<xsl:template name="stdiface">
<html>
<head>
<title><xsl:value-of select="$title"/></title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<link href="../iu.css" rel="stylesheet" type="text/css"/>
</head>
<body>
<div align="center">
<table cellspacing="0" id="header">
<tr>
<td id="title">
<h2><xsl:value-of select="$title"/></h2>
</td>
</tr>
</table>
</div>
<table cellspacing="0" class="layout">
<xsl:apply-templates/>
</table>
<p>
<a href="?">Home</a>
</p>
<p>
<a href="http://www.oclc.org/research/software/srw">
<img src="http://www.oclc.org/research/images/badges/oclc_srwu.gif" alt="Powered by OCLC SRW/U" width="80" height="15"/>
</a>
</p>
</body>
</html>

</xsl:template>

<xsl:template match="srw:version">
</xsl:template>

<xsl:template match="srw:diagnostics">
<tr><td><h2>Diagnostics</h2></td></tr>
<tr><td width="50%" style="padding-right: 10px;">
<xsl:apply-templates/>
</td><td></td></tr>
</xsl:template>

<xsl:template match="diag:diagnostic">
<table cellspacing="0" class="formtable">
<xsl:apply-templates/>
</table>
</xsl:template>

<xsl:template match="diag:uri">
<tr><th>Identifier:</th><td><xsl:value-of select="."/></td></tr>
<tr><th>Meaning:</th>
<xsl:variable name="diag" select="."/>
<td>
<xsl:choose>
  <xsl:when test="$diag='info:srw/diagnostic/1/1'">
    <xsl:text>General System Error</xsl:text>
    </xsl:when>
  <xsl:when test="$diag='info:srw/diagnostic/1/6'">
    <xsl:text>Unsupported Parameter Value</xsl:text>
    </xsl:when>
  <xsl:when test="$diag='info:srw/diagnostic/1/7'">
    <xsl:text>Mandatory Parameter Not Supplied</xsl:text>
    </xsl:when>
  <xsl:when test="$diag='info:srw/diagnostic/1/10'">
    <xsl:text>Query Syntax Error</xsl:text>
    </xsl:when>
  <xsl:when test="$diag='info:srw/diagnostic/1/16'">
    <xsl:text>Unsupported Index</xsl:text>
    </xsl:when>
  <xsl:when test="$diag='info:srw/diagnostic/1/22'">
    <xsl:text>Unsupported Combination of Relation and Index</xsl:text>
    </xsl:when>
  <xsl:when test="$diag='info:srw/diagnostic/1/51'">
    <xsl:text>Result Set Does Not Exist</xsl:text>
    </xsl:when>
  <xsl:when test="$diag='info:srw/diagnostic/1/61'">
    <xsl:text>First Record Position Out Of Range</xsl:text>
    </xsl:when>
  <xsl:when test="$diag='info:srw/diagnostic/1/64'">
    <xsl:text>Record temporarily unavailable</xsl:text>
    </xsl:when>
  <xsl:when test="$diag='info:srw/diagnostic/1/66'">
    <xsl:text>Unknown Schema For Retrieval</xsl:text>
    </xsl:when>
  <xsl:when test="$diag='info:srw/diagnostic/1/71'">
    <xsl:text>Unsupported record packing</xsl:text>
    </xsl:when>
  <xsl:when test="$diag='info:srw/diagnostic/1/93'">
    <xsl:text>Sort Ended Due To Missing Value</xsl:text>
    </xsl:when>
  <xsl:when test="$diag='info:srw/diagnostic/1/94'">
    <xsl:text>When resultSetTTL=0, Sort Only Legal When startRec=1</xsl:text>
    </xsl:when>
  <xsl:when test="$diag='info:srw/diagnostic/1/110'">
    <xsl:text>Stylesheets Not Supported</xsl:text>
    </xsl:when>
  <xsl:when test="$diag='info:srw/diagnostic/1/120'">
    <xsl:text>Response Position Out Of Range</xsl:text>
    </xsl:when>
  <xsl:when test="$diag='info:srw/diagnostic/1/130'">
    <xsl:text>Too Many Terms Matched By Masked Query Term</xsl:text>
    </xsl:when>
  </xsl:choose>
</td>
</tr>
</xsl:template>

<xsl:template match="diag:details">
<tr><th>Details:</th><td><xsl:value-of select="."/></td></tr>
</xsl:template>

<xsl:template match="diag:message"><tr><td><b>Message:</b></td><td><xsl:value-of select="."/></td></tr></xsl:template>

</xsl:stylesheet>
