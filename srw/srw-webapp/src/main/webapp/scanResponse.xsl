<?xml version='1.0'?>

<xsl:stylesheet version="1.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
     xmlns:srw="http://www.loc.gov/zing/srw/"
     xmlns:xcql="http://www.loc.gov/zing/cql/xcql/">

<xsl:import href="stdiface.xsl"/>

<xsl:variable name="title">Result of scan for term: <xsl:value-of select="/srw:scanResponse/srw:echoedScanRequest/srw:scanClause"/></xsl:variable>
<xsl:variable name="maximumTerms"><xsl:value-of select="/srw:scanResponse/srw:echoedScanRequest/srw:maximumTerms"/></xsl:variable>
<xsl:variable name="indexRelation"> <xsl:value-of select="/srw:scanResponse/srw:echoedScanRequest/srw:xScanClause/xcql:index"/><xsl:text> </xsl:text><xsl:value-of select="/srw:scanResponse/srw:echoedScanRequest/srw:xScanClause/xcql:relation/xcql:value"/><xsl:text> </xsl:text></xsl:variable>

<xsl:template match="/">
<xsl:call-template name="stdiface">
</xsl:call-template>
</xsl:template>

<xsl:template match="srw:scanResponse">
<tr><td><h1>Terms</h1></td></tr>
<tr><td>
<xsl:apply-templates/>
</td></tr>
</xsl:template>

<xsl:template match="srw:terms">
  <xsl:call-template name="prev-nextTerms"/>
  <table width="50%" cellspacing="0" class="formtable">
  <tr><th>Term</th><th>Frequency</th></tr>
  <xsl:apply-templates/>
  </table>

  <xsl:call-template name="prev-nextTerms"/>

</xsl:template>

<xsl:template match="srw:term">
  <tr>
    <xsl:apply-templates/>
  </tr>
</xsl:template>

<xsl:template match="srw:value">
  <xsl:variable name="whereInList" select="../srw:whereInList" />
  
<td>
  <a>
    <xsl:attribute name="href">?operation=searchRetrieve&amp;version=1.1&amp;recordPacking=xml&amp;query=<xsl:value-of select="$indexRelation"/>"<xsl:value-of select="."/>"&amp;maximumRecords=1&amp;startRecord=1</xsl:attribute>
    <xsl:value-of select="."/>
  </a> 
  <xsl:if test="$whereInList">
    <xsl:text>   (</xsl:text>
    <xsl:value-of select="$whereInList"/>
    <xsl:text> term)</xsl:text>
  </xsl:if>
</td>
</xsl:template>

<xsl:template match="srw:term/srw:numberOfRecords">
<td><xsl:value-of select="."/></td>
</xsl:template>

<xsl:template match="srw:echoedScanRequest"/>

<xsl:template match="srw:whereInList" />
  
<xsl:template name="prev-nextTerms">
<p>
&lt;--
<a>
<xsl:attribute name="href">
?operation=scan&amp;scanClause=
<xsl:value-of select="$indexRelation"/>"<xsl:value-of select ="./srw:term[1]/srw:value"/>"
&amp;responsePosition=<xsl:value-of select="$maximumTerms"/>
&amp;version=1.1
<xsl:if test="/srw:scanResponse/srw:echoedScanRequest/srw:maximumTerms">
&amp;maximumTerms=<xsl:value-of select="$maximumTerms"/>
</xsl:if>
</xsl:attribute>
Previous
</a>

|

<a>
<xsl:attribute name="href">
?operation=scan&amp;scanClause=
<xsl:value-of select="$indexRelation"/>"<xsl:value-of select ="./srw:term[count(//srw:scanResponse/srw:terms/srw:term)]/srw:value"/>"
&amp;responsePosition=1&amp;version=1.1
<xsl:if test="/srw:scanResponse/srw:echoedScanRequest/srw:maximumTerms">
&amp;maximumTerms=<xsl:value-of select="$maximumTerms"/>
</xsl:if>
</xsl:attribute>
Next 
</a>
--&gt;
</p>
</xsl:template>

</xsl:stylesheet>
