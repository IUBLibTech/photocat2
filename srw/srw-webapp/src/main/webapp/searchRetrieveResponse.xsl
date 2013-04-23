<?xml version='1.0'?>

<xsl:stylesheet version="1.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
     xmlns:srw="http://www.loc.gov/zing/srw/"
     xmlns:rs="info:srw/extension/5/restrictorSummary">

<xsl:import href="ber.xsl"/>
<xsl:import href="MarcXmlToTaggedText.xsl"/>
<xsl:import href="stdiface.xsl"/>
<xsl:import href="FedoraAdminXmlToPreview.xsl" />
<!--xsl:import href="lomRecord.xsl"/>
<xsl:import href="zthesRecord.xsl"/>
<xsl:import href="dublinCoreRecord.xsl"/>
<xsl:import href="OaiHeaderToTaggedText.xsl"/>
<xsl:import href="WikiRepositoryToTaggedText.xsl"/-->

<xsl:variable name="title">Result of search: <xsl:value-of select="/srw:searchRetrieveResponse/srw:echoedSearchRetrieveRequest/srw:query"/></xsl:variable>

<xsl:template match="/">
<xsl:call-template name="stdiface">
<xsl:with-param name="title" select="$title"/>
</xsl:call-template>
</xsl:template>

<xsl:template match="srw:searchRetrieveResponse">
<xsl:apply-templates select="srw:diagnostics"/>
<tr><td><h1>Search Results</h1></td></tr>
<tr><td>
<xsl:apply-templates select="srw:resultSetId"/>
<xsl:apply-templates select="srw:numberOfRecords"/>
<xsl:apply-templates select="srw:extraResponseData"/>
<xsl:apply-templates select="srw:records"/>
</td></tr>
</xsl:template>

<xsl:template match="srw:numberOfRecords">
  Records found:<xsl:text> </xsl:text><xsl:value-of select="."/><br/>
</xsl:template>

<xsl:template match="srw:resultSetId">
  Result Set Identifier:<xsl:text> </xsl:text><xsl:value-of select="."/><br/>
  <xsl:apply-templates select="srw:resultSetIdleTime"/>
</xsl:template>

<xsl:template match="srw:resultSetIdleTime">
  <xsl:text> </xsl:text>(Will last for<xsl:text> </xsl:text><xsl:value-of select="."/><xsl:text> </xsl:text>seconds)
</xsl:template>

<xsl:template match="srw:records">
<tr><td><h3>Records</h3></td></tr>
<tr><td><xsl:call-template name="prev-nextRecord"/></td></tr>
<xsl:apply-templates/>
<tr><td><xsl:call-template name="prev-nextRecord"/></td></tr>
</xsl:template>

<xsl:template match="srw:record">
<tr><td>
    <xsl:apply-templates select="child::srw:recordPosition"/>
    <xsl:apply-templates select="child::srw:recordSchema"/>
    <xsl:apply-templates select="child::srw:recordData"/>
</td></tr>
</xsl:template>

<xsl:template match="srw:record/srw:recordSchema">
  Schema:
<xsl:variable name="schema" select="."/> 
  <xsl:choose>
      <xsl:when test="$schema = 'http://www.openarchives.org/OAI/2.0/#header'">
	      OAI Header
      </xsl:when>
      <xsl:when test="$schema = 'info:srw/schema/1/dc-v1.1'">
	      Dublin Core
      </xsl:when>
      <xsl:when test="$schema = 'info:srw/schema/1/marcxml-v1.1'">
	      MARC XML
      </xsl:when>
      <xsl:when test="$schema = 'info:srw/schema/1/mods-v3.0'">
	      MODS
      </xsl:when>
      <xsl:when test="$schema = 'http://srw.o-r-g.org/schemas/ccg/1.0/'">
	      Collectable Card Schema
      </xsl:when>
      <xsl:otherwise>
	      <xsl:value-of select="$schema"/>
      </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="srw:recordPosition">
  Position: <xsl:value-of select="."/> <xsl:text> </xsl:text>
</xsl:template>

<xsl:template match="srw:nextRecordPosition">
  <!-- Not used -->
</xsl:template>

<xsl:template match="srw:recordData">
  <table width="100%" style="vertical-align: top; border: 1px solid; padding: 3px; border-collapse: collapse; background-color: #eefdff">

<xsl:choose>
<xsl:when test="../srw:recordPacking = 'string'">
<tr><td style="border: 1px solid">
<pre><xsl:value-of select="."/></pre>
</td></tr>
</xsl:when>
<xsl:otherwise>
<xsl:apply-templates/>
</xsl:otherwise>
</xsl:choose>

</table>
</xsl:template>


<xsl:template match="srw:extraResponseData">
  <xsl:apply-templates select="rs:restrictorSummary"/>
</xsl:template>


<xsl:template match="rs:restrictorSummary">
  <table width="100%">
    <tr>
      <xsl:for-each select="rs:restrictor">
        <th><xsl:value-of select="@index"/>(<xsl:value-of select="@count"/>)</th>
        </xsl:for-each>
      </tr>
    <xsl:call-template name="loop">
      <xsl:with-param name="i" select="1"/>
      <xsl:with-param name="limit" select="5"/>
      </xsl:call-template>
    </table>
</xsl:template>

<xsl:template name="loop">
  <xsl:param name="i" select="1"/>
  <xsl:param name="limit" select="1"/>
  <xsl:variable name="testPassed">
    <xsl:choose>
      <xsl:when test="$i &lt;= $limit">
        <xsl:text>true</xsl:text>
        </xsl:when>
      <xsl:otherwise>
        <xsl:text>false</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

  <xsl:if test="$testPassed='true'">
    <tr>
      <xsl:for-each select="rs:restrictor">
        <td><xsl:if test="rs:entry[$i]"><xsl:value-of select="rs:entry[$i]"/>(<xsl:value-of select="rs:entry[$i]/@count"/>)</xsl:if></td>
        </xsl:for-each>
      </tr>
    <xsl:call-template name="loop">
      <xsl:with-param name="i" select="$i+1"/>
      <xsl:with-param name="limit" select="$limit"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

<xsl:template match="srw:echoedSearchRetrieveRequest"/>


<xsl:template name="prev-nextRecord">
  <xsl:variable name="startRecord"
    select="number(/srw:searchRetrieveResponse/srw:echoedSearchRetrieveRequest/srw:startRecord)"/>

  <xsl:variable name="resultSetTTL">
    <xsl:if test="/srw:searchRetrieveResponse/srw:echoedSearchRetrieveRequest/srw:resultSetTTL">
      <xsl:text>&amp;resultSetTTL=</xsl:text>
      <xsl:value-of select="/srw:searchRetrieveResponse/srw:echoedSearchRetrieveRequest/srw:resultSetTTL"/>
      </xsl:if>
    </xsl:variable>

  <xsl:variable name="recordPacking">
    <xsl:text>&amp;recordPacking=</xsl:text>
    <xsl:value-of select="/srw:searchRetrieveResponse/srw:echoedSearchRetrieveRequest/srw:recordPacking"/>
    </xsl:variable>

  <xsl:variable name="numRecs">
    <xsl:value-of select="number(/srw:searchRetrieveResponse/srw:echoedSearchRetrieveRequest/srw:maximumRecords)"/>
    </xsl:variable>

  <xsl:variable name="maximumRecords">
    <xsl:text>&amp;maximumRecords=</xsl:text>
    <xsl:value-of select="$numRecs"/>
    </xsl:variable>

  <xsl:variable name="prev" select="$startRecord - $numRecs"/>

  <xsl:variable name="recordSchema">
    <xsl:if test="/srw:searchRetrieveResponse/srw:echoedSearchRetrieveRequest/srw:recordSchema">&amp;recordSchema=<xsl:value-of select="/srw:searchRetrieveResponse/srw:echoedSearchRetrieveRequest/srw:recordSchema"/></xsl:if>
    </xsl:variable>

  <xsl:variable name="sortKeys"><xsl:if test="/srw:searchRetrieveResponse/srw:echoedSearchRetrieveRequest/srw:sortKeys">&amp;sortKeys=<xsl:value-of select="/srw:searchRetrieveResponse/srw:echoedSearchRetrieveRequest/srw:sortKeys"/></xsl:if></xsl:variable>
  <xsl:variable name="query">
    <xsl:choose>
      <xsl:when test="/srw:searchRetrieveResponse/srw:resultSetId">
          <xsl:text>&amp;query=cql.resultSetId=</xsl:text>
          <xsl:value-of select="/srw:searchRetrieveResponse/srw:resultSetId"/>
          </xsl:when>
      <xsl:otherwise>
        <xsl:text>&amp;query=</xsl:text>
        <xsl:value-of select="/srw:searchRetrieveResponse/srw:echoedSearchRetrieveRequest/srw:query"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

  <xsl:if test="$prev>0">
    <a>
      <xsl:attribute name="href">
        <xsl:text>?operation=searchRetrieve</xsl:text>
        <xsl:value-of select="$query"/>
        <xsl:value-of select="$maximumRecords"/>
        <xsl:value-of select="$resultSetTTL"/>
        <xsl:value-of select="$recordSchema"/>
        <xsl:value-of select="$sortKeys"/>
        <xsl:value-of select="$recordPacking"/>
        <xsl:text>&amp;startRecord=</xsl:text><xsl:value-of select="$prev"/>
        </xsl:attribute>
      <xsl:text>Previous Record(s)</xsl:text>
      </a>
      <xsl:text> </xsl:text>
    </xsl:if>


  <xsl:if test="/srw:searchRetrieveResponse/srw:nextRecordPosition">
    <a>
      <xsl:attribute name="href">
        <xsl:text>?operation=searchRetrieve</xsl:text>
        <xsl:value-of select="$query"/>
        <xsl:value-of select="$maximumRecords"/>
        <xsl:value-of select="$resultSetTTL"/>
        <xsl:value-of select="$recordSchema"/>
        <xsl:value-of select="$sortKeys"/>
        <xsl:value-of select="$recordPacking"/>
        <xsl:text>&amp;startRecord=</xsl:text>
        <xsl:value-of select="/srw:searchRetrieveResponse/srw:nextRecordPosition"/>
        </xsl:attribute>
      <xsl:text>Next Record(s)</xsl:text>
      </a>
    </xsl:if>
</xsl:template>

</xsl:stylesheet>
