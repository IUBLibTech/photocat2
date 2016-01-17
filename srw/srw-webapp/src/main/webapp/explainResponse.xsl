<?xml version="1.0"?>

<!--
  License:
    Copyright 2006-2008 OCLC Online Computer Library Center, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.

    You may obtain a copy of the License at

      <http://www.apache.org/licenses/LICENSE-2.0>

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied.  See the License for the specific
    language governing permissions and limitations under the License.

  -->

<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:srw="http://www.loc.gov/zing/srw/"
    xmlns:zr="http://explain.z3950.org/dtd/2.0/">

<xsl:import href="stdiface.xsl"/>

<xsl:output
  method="html" 
  encoding="utf-8" 
  media-type="application/xhtml+xml"
  doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN" 
  doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
/>

<xsl:variable name="title"><xsl:value-of select="/srw:explainResponse/srw:record/srw:recordData/zr:explain/zr:databaseInfo/zr:title"/></xsl:variable>
<xsl:variable name="dbname"><xsl:value-of select="/srw:explainResponse/srw:record/srw:recordData/zr:explain/zr:databaseInfo/zr:title"/></xsl:variable>

<xsl:template match="/">
<xsl:call-template name="stdiface">
<xsl:with-param name="title" select="$title"/>
<xsl:with-param name="dbname" select="$dbname"/>
</xsl:call-template>
</xsl:template>

<xsl:template match="srw:explainResponse">
  <div id="content">
<script>
  <xsl:text>
    function mungeForm() {
      inform = document.CQLForm;
      outform = document.SRUForm;
      max = inform.maxIndex.value;
	if(outform.resultSetTTL.value==0) {
        if(outform.sortKeys.value.indexOf(',')) {
          outform.resultSetTTL.value=300;
        }
      }
      cql = "";
      prevIdx = 0;
      // Step through elements in form to create CQL
      for (var idx = 1; idx &lt;= max; idx++) {
        term = inform["term"+idx].value;
        if (term) {
          if (prevIdx) {
            cql += " " + inform["bool" + prevIdx].value + " "
          }
          if (term.indexOf(' ')) {
            term = '"' + term + '"';
          }
          cql += inform["index" + idx].value + " " + inform["relat" + idx].value + " " + term
          prevIdx = idx
        }
      }
      if (!cql) {
        alert("At least one term is required to search.");
        return false;
      }
      outform.query.value = cql
      outform.submit();
      return false;
    }

    function mungeScanForm() {
      inform = document.ScanIndexes;
      outform = document.ScanSubmit;
      index = inform.scanIndex.value;
      term = inform.term.value;
      relat = inform.relat.value;
      outform.scanClause.value = index + " " + relat +" \"" + term + "\""
      outform.submit();
      return false;
    }
</xsl:text>
</script>

<h2 class="dbname">
<xsl:value-of select="srw:record/srw:recordData/zr:explain/zr:databaseInfo/zr:title"/>
</h2>

<xsl:apply-templates select="srw:diagnostics"/>

<table cellspacing="0" class="layout">
<tr> 
<td><h1>Search</h1></td>
<td><h1>Browse</h1></td>
</tr>
<tr> 
<td width="60%" style="padding-right: 10px;"> 
  <xsl:call-template name="SearchForm"/>
</td>

<td width="40%">
  <xsl:call-template name="BrowseForm"/>
</td>
</tr>
</table>
</div> <!--content-->
</xsl:template>


<xsl:template name="BrowseForm">
<xsl:call-template name="BrowseFormPart1"/>
<xsl:call-template name="BrowseFormPart2"/>
</xsl:template>


<xsl:template name="SearchForm">
<xsl:call-template name="SearchFormPart1"/>
<xsl:call-template name="SearchFormPart2"/>
</xsl:template>


<xsl:template name="SearchFormPart1">
<form name="CQLForm" onSubmit="return mungeForm();">
<input type="submit" value="Search" onClick="return mungeForm();"/>
<input type="hidden" name="maxIndex" value="{count(srw:record/srw:recordData/zr:explain/zr:indexInfo/zr:index[not(@search='false')])}"/>
<table cellspacing="0" class="formtable">
<tr>
<th>Index</th>
<th>Relation</th>
<th>Term</th>
<th>Boolean</th>
</tr>

<xsl:for-each select="srw:record/srw:recordData/zr:explain/zr:indexInfo/zr:index[not(@search='false')]">
  <xsl:sort select="."/>
  <xsl:choose>
    <xsl:when test="not(zr:configInfo) or zr:configInfo/zr:supports">
      <tr>
        <td>
          <xsl:value-of select="zr:title"/>
          <input type="hidden" name="index{position()}" value="{zr:map[1]/zr:name/@set}.{zr:map[1]/zr:name}"/>
          </td>
        <td>
          <select name="relat{position()}">
            <xsl:choose>
              <xsl:when test="zr:configInfo">
                <xsl:for-each select="zr:configInfo/zr:supports">
                  <option value="{.}"><xsl:value-of select="."/></option>
                  </xsl:for-each>
                </xsl:when>
              <xsl:otherwise>
                <option value="=">=</option>
                <option value="exact">exact</option>
                <option value="any">any</option>
                <option value="all">all</option>
                <option value="&lt;">&lt;</option>
                <option value="&gt;">&gt;</option>
                <option value="&lt;=">&lt;=</option>
                <option value="&gt;=">&gt;=</option>
                <option value="&lt;&gt;">not</option>
                </xsl:otherwise>
              </xsl:choose>
            </select>
          </td>
        <td>
          <input type="text" value="" name="term{position()}"/>
          </td>
        <td>
          <select name="bool{position()}">
            <option value="and">and</option>
            <option value="or">or</option>
            <option value="not">not</option>
            </select>
          </td>
        </tr>
      </xsl:when>
    <xsl:otherwise>
      <input type="hidden" value="" name="term{position()}"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:for-each>
</table>
</form>
</xsl:template>


<xsl:template name="SearchFormPart2">
<form method="GET" name="SRUForm" onSubmit="mungeForm()">
  <input type="hidden" name="query" value=""/>
  <input type="hidden" name="version" value="1.1"/>
  <input type="hidden" name="operation" value="searchRetrieve"/>
  <table cellspacing="0" class="formtable">
    <tr>
      <td>Record Schema:</td>
      <td>
        <select name="recordSchema">
          <xsl:for-each select="srw:record/srw:recordData/zr:explain/zr:schemaInfo/zr:schema">
            <option value="{@identifier}">
              <xsl:if test="normalize-space(@identifier) = normalize-space(../../zr:configInfo/zr:default[@type = 'retrieveSchema'][1])">
                <xsl:attribute name="selected">
                  <xsl:value-of select="string('selected')" />
                </xsl:attribute>
              </xsl:if>
              <xsl:value-of select="zr:title"/>
            </option>
          </xsl:for-each>
        </select>
      </td>
    </tr>
    <tr>
      <td>Number of Records:</td>
      <td>
        <input type="text" name="maximumRecords">
          <xsl:attribute name="value">
            <xsl:choose>
              <xsl:when test='srw:record/srw:recordData/zr:explain/zr:configInfo/zr:default[@type="numberOfRecords"]'>
                <xsl:value-of select='srw:record/srw:recordData/zr:explain/zr:configInfo/zr:default[@type="numberOfRecords"]'/>
                </xsl:when>
              <xsl:otherwise>
                <xsl:text>1</xsl:text>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:attribute>
          </input>
        </td>
      </tr>

    <tr>
      <td>Record Position:</td>
      <td><input type="text" name="startRecord" value="1"/></td>
      </tr>
    <tr>
      <td>Result Set TTL:</td>
      <td><input type="text" name="resultSetTTL" value="0"/></td>
      </tr>
    <tr>
      <td>Record Packing:</td>
      <td>
        <select name="recordPacking">
          <option value="xml">XML</option>
          <option value="string">String</option>
          </select>
        </td>
      </tr>
    <tr>
	<td>Sort Keys:</td>
	<td><input type="text" name="sortKeys" value = ""/></td>
	</tr>	
    </table>
  <input type="submit" value="Search" onClick="return mungeForm();"/>
  </form>
</xsl:template>

<xsl:template name="BrowseFormPart1">
  <form name="ScanIndexes" onSubmit="return mungeScanForm();">
    <input type="submit" value="Browse" onClick="return mungeScanForm();"/>
    <table cellspacing="0" class="formtable">
      <tr>
        <th>Index</th>
        <th>Relation</th>
        <th>Term</th>
        </tr>
      <tr>
        <td>
          <select name="scanIndex">
            <xsl:for-each select="srw:record/srw:recordData/zr:explain/zr:indexInfo/zr:index[not(@scan='false')]">
              <xsl:sort select="."/>
              <option value="{zr:map[1]/zr:name/@set}.{zr:map[1]/zr:name}">
                <xsl:value-of select="zr:title"/>
                </option>
              </xsl:for-each>
            </select>
          </td>
        <td>
          <select name="relat">
            <option value="=">=</option>
            </select>
          </td>
        <td>
          <input name="term" type="text" value = ""/>
          </td>
        </tr>
      </table>
    </form>
  </xsl:template>

<xsl:template name="BrowseFormPart2">
  <form name="ScanSubmit" method="GET"  onSubmit="mungeScanForm()">
    <input type="hidden" name="operation" value="scan"/>
    <input type="hidden" name="scanClause" value=""/>
    <input type="hidden" name="version" value="1.1"/>
    <table cellspacing="0" class="formtable">
      <tr>
        <td>Response Position:</td>
        <td>
          <input type="text" name="responsePosition" value="1" size="3"/>
          </td>
        </tr>
      <tr>
        <td>Maximum Terms:</td>
        <td>
          <input type="text" name="maximumTerms" value="100" size="3"/>
          </td>
        </tr>
      </table>
    <input type="submit" value="Browse" onClick="return mungeScanForm();"/>
    </form>
  </xsl:template>

</xsl:stylesheet>
