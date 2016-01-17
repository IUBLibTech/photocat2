/**
 * Copyright 2015 Trustees of Indiana University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE TRUSTEES OF INDIANA UNIVERSITY ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE TRUSTEES OF INDIANA UNIVERSITY OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of the Trustees of Indiana University.
 */
package edu.indiana.dlib.fedoraindexer.server.index;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.lucene.document.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.indiana.dlib.fedora.client.iudl.DLPFedoraClient;
import edu.indiana.dlib.fedoraindexer.server.FedoraObjectAdministrativeMetadata;
import edu.indiana.dlib.fedoraindexer.server.IndexInitializationException;
import edu.indiana.dlib.fedoraindexer.server.index.converters.CQLStyleLuceneDocumentConverter;

/**
 * <p>
 *   A general purpose (proof of concept) Index for the full text of 
 *   a paged object (book, journal, etc.).  A document is stored for
 *   each page containing the fulltext as well as information about 
 *   the page.  Specifically, the following fields are stored:
 *   <ul>
 *     <li>
 *     
 *     </li>
 *   </ul>
 * </p> 
 */
public class PageKeyedFullTextIndex extends CompoundObjectLuceneIndex {
    
    private static final String PURL_ROOT = "http://purl.dlib.indiana.edu/iudl/";
    
    private String metsUrlPattern = null;
    
    /**
     * A namespace aware document builder.
     */
    private DocumentBuilder documentBuilder;

    /**
     * An XPath instance configured to recognize the "mets" and "xlink" 
     * namespaces.
     */
    private XPath xpath;
    
    public PageKeyedFullTextIndex(Properties config, DLPFedoraClient fc) throws IndexInitializationException {
        super(config);
        this.metsUrlPattern = fc.getServerUrl() + "/get/%PID%/METADATA";
        
        // set up the document builder
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            this.documentBuilder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            throw new IndexInitializationException("Unable to initialize a DocumentBuilder!");
        }
        
        // create the xpath with necessary namespaces built in
        this.xpath = XPathFactory.newInstance().newXPath();
        this.xpath.setNamespaceContext(new NamespaceContext() {

            public String getNamespaceURI(String prefix) {
                if (prefix.equals("mets")) {
                    return "http://www.loc.gov/METS/";
                } else if (prefix.equals("xlink")) {
                    return "http://www.w3.org/1999/xlink";
                } else {
                    return null;
                }
            }

            public String getPrefix(String namespaceURI) {
                if (namespaceURI.equals("http://www.loc.gov/METS/")) {
                    return "mets";
                } else if (namespaceURI.equals("http://www.w3.org/1999/xlink")) {
                    return "xlink";
                } else {
                    return null;
                }
            }

            public Iterator getPrefixes(String namespaceURI) {
                return Arrays.asList(new String[] { "http://www.loc.gov/METS/", "http://www.w3.org/1999/xlink" }).iterator();
            }});
    }

    /**
     * <p>
     *   Fetches several documents for the given paged object, 
     *   to build the appropriate xml records.  For each page 
     *   a lucene Document is created containing the following
     *   fields:
     *   <ul>
     *     <li>itemId</li>
     *     <li>itemLabel</li>
     *     <li>itemType</li>
     *     <li>pageIndex</li>
     *     <li>pageText*</li>
     *     <li>titleId</li>
     *     <li>titleLabel</li>
     *   </ul>
     *   For all items except pageText, an exact, facet, present, sort
     *   present and stemmed version is created as specified in the {@link
     *   edu.indiana.dlib.fedoraindexer.server.index.converters.CQLStyleLuceneDocumentConverter}
     *   <br />
     *   For the pageText field, only the normal, stemmed and stored version
     *   are created.
     * </p>
     */
    protected List<Document> createIndexDocuments(FedoraObjectAdministrativeMetadata adminData) throws Exception {
        List<Document> documents = new ArrayList<Document>();
        if (!adminData.getContentModel().contains("info:fedora/cmodel:paged")) {
            throw new IllegalArgumentException(adminData.getPid() + " is not a PAGED item!");
        }
        
        // fetch "little" METS (METS for objects with the info:fedora/cmodel:paged content model)
        String metsURL = this.metsUrlPattern.replace("%PID%", adminData.getPid());
        org.w3c.dom.Document littleMetsDoc = this.documentBuilder.parse(metsURL);
        
        // extract values from the "little" METS
        String itemLabel = (String) xpath.evaluate("/mets:mets/mets:structMap[@TYPE='physical']/mets:div[1]/@LABEL", littleMetsDoc, XPathConstants.STRING);
        String itemId = (String) xpath.evaluate("/mets:mets/@ID", littleMetsDoc, XPathConstants.STRING);
        String bigMetsUrl = (String) xpath.evaluate("/mets:mets/mets:structMap[@TYPE='related']//mets:mptr[@LOCTYPE='PURL']/@xlink:href", littleMetsDoc, XPathConstants.STRING);

        // fetch and extract values from the "big" METS
        String titleLabel = null;
        String titleId = null;
        String itemType = "item";
        if (bigMetsUrl != null && bigMetsUrl.trim().length() > 0) {
            String collectionId = bigMetsUrl.substring(0, bigMetsUrl.indexOf("/mets/")).replace(PURL_ROOT, "");
            org.w3c.dom.Document bigMetsDoc = this.documentBuilder.parse(bigMetsUrl);
            titleId = (String) xpath.evaluate("/mets:mets/@ID", bigMetsDoc, XPathConstants.STRING);
            
            String xpathToEvaluate = "/mets:mets/mets:structMap[@TYPE='logical']//mets:div[mets:mptr/@xlink:href='" + PURL_ROOT + collectionId + "/mets/" + itemId + "']"; 
            Element itemDiv = (Element) xpath.evaluate(xpathToEvaluate, bigMetsDoc, XPathConstants.NODE);
            if (itemDiv == null) {
                throw new RuntimeException("Unable to parse logicl struct map of the big mets! (" + xpathToEvaluate + ")");
            }
            titleLabel = (String) xpath.evaluate("@LABEL", itemDiv, XPathConstants.STRING);
            itemType = (String) xpath.evaluate("@TYPE", itemDiv, XPathConstants.STRING);
        }
        
        // fetch and index every page
        NodeList nl = (NodeList) xpath.evaluate("/mets:mets/mets:fileSec/mets:fileGrp[@USE='screen']/mets:file", littleMetsDoc, XPathConstants.NODESET);
        for (int i = 0; i < nl.getLength(); i ++) {
            Element fileEl = (Element) nl.item(i);
            String largeUrl = (String) xpath.evaluate("mets:FLocat/@xlink:href", fileEl, XPathConstants.STRING);
            String pageId = (String) xpath.evaluate("@GROUPID", fileEl, XPathConstants.STRING);
            String pageTextUrl = largeUrl.replace("screen", "text");
            System.out.println(pageTextUrl);
            StringBuffer fulltext = new StringBuffer();
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new URL(pageTextUrl).openConnection().getInputStream()));
                String line = reader.readLine();
                while (line != null) {
                    fulltext.append(line);
                    fulltext.append("\n");
                    line = reader.readLine();
                }
            } catch (Throwable t) {
                // error reading page (likely because page doesn't exist)
                // skip it...
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }

            if (fulltext.length() == 0) {
                LOGGER.debug("No page text for " + pageTextUrl + ", no index document created.");
            } else {
                LOGGER.debug("Fulltext for " + pageTextUrl + " is " + fulltext.length() + " characters, creating index document.");
                String sequence = (String) xpath.evaluate("@SEQ", fileEl, XPathConstants.STRING);

                Document indexDoc = new Document();
                CQLStyleLuceneDocumentConverter.addKeywordField("itemLabel", itemLabel, indexDoc);
                CQLStyleLuceneDocumentConverter.addKeywordField("itemId", itemId, indexDoc);
                CQLStyleLuceneDocumentConverter.addKeywordField("pageId", pageId, indexDoc);
                if (titleId != null) {
                    CQLStyleLuceneDocumentConverter.addKeywordField("titleId", titleId, indexDoc);
                    CQLStyleLuceneDocumentConverter.addKeywordField("titleLabel", titleLabel, indexDoc);
                } else {
                    CQLStyleLuceneDocumentConverter.addKeywordField("titleId", itemId, indexDoc);
                    CQLStyleLuceneDocumentConverter.addKeywordField("titleLabel", itemLabel, indexDoc);
                }
                CQLStyleLuceneDocumentConverter.addKeywordField("itemType", itemType, indexDoc);
                CQLStyleLuceneDocumentConverter.addKeywordField("pageIndex", sequence, indexDoc);
                CQLStyleLuceneDocumentConverter.addFullTextField("pagetext", fulltext.toString(), indexDoc);
                documents.add(indexDoc);
            }
        }
        return documents;
    }
}
