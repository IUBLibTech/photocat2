<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:iudlAdmin="http://dlib.indiana.edu/xml/iudlAdmin/version1.0/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html"/>
    <xsl:template match="iudlAdmin">
        <xsl:variable name="itemID">
            <xsl:call-template name="substring-after-last">
                <xsl:with-param name="string" select="iudlAdmin:fullItemID"/>
                <xsl:with-param name="match">/</xsl:with-param>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="collectionID">
            <xsl:call-template name="substring-before-last">
                <xsl:with-param name="string" select="iudlAdmin:fullItemID"/>
                <xsl:with-param name="match" select="'/'"/>
            </xsl:call-template>
        </xsl:variable>

        <tr>
            <td>
                <xsl:attribute name="rowspan">
                    <xsl:value-of select="count(*) + 1"/>
                </xsl:attribute>
                <img>
                    <xsl:attribute name="src">
                        <xsl:text>http://purl.dlib.indiana.edu/iudl</xsl:text>
                        <xsl:value-of select="$collectionID"/>
                        <xsl:text>/thumbnail/</xsl:text>
                        <xsl:value-of select="$itemID"/>
                    </xsl:attribute>
                </img>
            </td>
            <td colspan="2">
                <strong>
                    <a>
                        <xsl:attribute name="href">
                            <xsl:text>http://purl.dlib.indiana.edu/iudl</xsl:text>
                            <xsl:value-of select="iudlAdmin:fullItemID"/>
                        </xsl:attribute>
                        <xsl:value-of select="iudlAdmin:title"/>
                    </a>
                </strong>
            </td>
        </tr>
        <xsl:apply-templates select="*"/>
    </xsl:template>

    <xsl:template match="iudlAdmin:*">
        <tr>
            <td>
                <xsl:value-of select="name()"/>
            </td>
            <td>
                <xsl:if test="name() = 'iudlAdmin:pid'">
                    <a>
                        <xsl:attribute name="href">
                            <xsl:text>/fedora/get/</xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:attribute>
                        <xsl:value-of select="."/>
                    </a>
                </xsl:if>
                <xsl:if test="not(name() = 'iudlAdmin:pid')">
                    <xsl:value-of select="."/>
                </xsl:if>
            </td>
        </tr>
    </xsl:template>

    <!-- String utility template: Returns the substring after the last occurrence of another string. 
        Particularly useful for returning the last item in a URL.  substring-after-last($URL, '/')
    -->
    <xsl:template name="substring-after-last">
        <!-- takes two parameters - the string and the match -->
        <xsl:param name="string"/>
        <xsl:param name="match"/>
        <xsl:choose>
            <!-- if the string contains the character... -->
            <xsl:when test="contains($string, $match)">
                <!-- call the template recursively... -->
                <xsl:call-template name="substring-after-last">
                    <!-- with the string being the string after the character -->
                    <xsl:with-param name="string" select="substring-after($string, $match)"/>
                    <!-- and the character being the same as before -->
                    <xsl:with-param name="match" select="$match"/>
                </xsl:call-template>
            </xsl:when>
            <!-- otherwise, we've already progressed to the last occurrence of match, just return the string -->
            <xsl:otherwise>
                <xsl:value-of select="$string"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!-- String utility template: Returns the substring before the last occurrence of another string. 
        Particularly useful for removing the last item in a URL.  substring-before-last($URL, '/')
    -->

    <xsl:template name="substring-before-last">
        <!-- takes two parameters - the string and the match -->
        <xsl:param name="string"/>
        <xsl:param name="match"/>
        <xsl:variable name="result-with-match">
            <xsl:call-template name="remove-after-last">
                <xsl:with-param name="string" select="$string"/>
                <xsl:with-param name="match" select="$match"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:value-of select="substring($result-with-match, 0, string-length($result-with-match))"/>
    </xsl:template>

    <!-- String utility template: Remove everything after the last occurence of $char-->
    <xsl:template name="remove-after-last">
        <!-- takes two parameters - the string and the match -->
        <xsl:param name="string"/>
        <xsl:param name="match"/>
        <xsl:choose>
            <!-- if the string contains the character... -->
            <xsl:when test="contains($string, $match)">
                <!-- output the part of the string before the character, including the character -->
                <xsl:value-of select="substring-before($string, $match)"/>
                <xsl:value-of select="$match"/>
                <!-- call the template recursively... -->
                <xsl:call-template name="remove-after-last">
                    <!-- with the string being the string after the character
                    -->
                    <xsl:with-param name="string" select="substring-after($string, $match)"/>
                    <!-- and the character being the same as before -->
                    <xsl:with-param name="match" select="$match"/>
                </xsl:call-template>
            </xsl:when>
            <!-- otherwise, return nothing -->
            <xsl:otherwise/>
        </xsl:choose>
    </xsl:template>


</xsl:stylesheet>
