<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:dc="http://purl.org/dc/elements/1.1/" version="2.0">
    <xsl:output method="xml" indent="yes" standalone="yes" encoding="UTF-8"/>
    <xsl:template match="/">
        <srw_dc:dc  xmlns:srw_dc="info:srw/schema/1/dc-v1.1">
            <xsl:copy-of select="/oai_dc:dc/*" copy-namespaces="no" />
        </srw_dc:dc>
    </xsl:template>
</xsl:stylesheet>
