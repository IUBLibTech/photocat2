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
package edu.indiana.dlib.search.srw;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * <p>
 *   A utility class that iterates over search results from an
 *   SRU searchRetrieveResponse.  This class abstracts away
 *   complexities such as paging and simply exposes the XML 
 *   fragment (as a DOM Element) for each record in the
 *   search results.
 * </p> 
 */
public class SRUSearchResultIterator {

    public static final String SRWNS = "http://www.loc.gov/zing/srw/";
    
    private static Log LOG = LogFactory.getLog(SRUSearchResultIterator.class);
    
    private static final int PAGE_SIZE = 25;
    
    /** The query provided by the user. */
    private String query;
    
    /** The base URL for the SRU server. */
    private String sruBaseUrl;

    /** The requested record format. */
    private String formatUri;
    
    /** 
     * The number of seconds the result Set should be retained.
     * Any value less than zero will result in this parameter
     * being omitted from the SRU request.  
     */
    private int resultSetTTL;
    
    /** The requested sort keys. */
    private String sortKeys;
    
    private DocumentBuilder docBuilder;
    
    private int nextRecordIndex;
    
    private int currentPageSize;
    
    private int currentPageStart;
    
    private int totalRecords;
    
    private Document currentPage;
    
    private NodeList recordElements;
    
    private XPath xpath;
    
    public SRUSearchResultIterator(String query, String sruBaseUrl, String recordFormatUri, String sortKeys) throws ParserConfigurationException {
        this(query, sruBaseUrl, recordFormatUri, 15, sortKeys);
    }
    
    public SRUSearchResultIterator(String query, String sruBaseUrl, String recordFormatUri, int resultSetTTL, String sortKeys) throws ParserConfigurationException {
        this.query = query;
        this.sruBaseUrl = sruBaseUrl;
        this.formatUri = recordFormatUri;
        this.resultSetTTL = resultSetTTL;
        this.sortKeys = sortKeys;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        this.docBuilder = factory.newDocumentBuilder();
        
        this.totalRecords = -1;
        this.nextRecordIndex = 0;
        this.currentPageSize = 0;
        this.currentPageStart = 0;
        this.currentPage = null;
        this.recordElements = null;
        
        this.xpath = XPathFactory.newInstance().newXPath();
        this.xpath.setNamespaceContext(new NamespaceContext() {

            public String getNamespaceURI(String prefix) {
                if (prefix.equals("srw")) {
                    return "http://www.loc.gov/zing/srw/";
                } else if (prefix.equals("diag")) {
                    return "http://www.loc.gov/zing/srw/diagnostic/";
                } else {
                    return null;
                }
            }

            public String getPrefix(String namespaceURI) {
                if ("http://www.loc.gov/zing/srw/".equals(namespaceURI)) {
                    return "srw";
                } else if ("http://www.loc.gov/zing/srw/diagnostic/".equals(namespaceURI)) {
                    return "diag";
                } else {
                    return null;
                }
            }

            public Iterator getPrefixes(String namespaceURI) {
                return Arrays.asList(new String[] { "srw", "diag" }).iterator();
            }});
    }
    
    private void fetchNextPage() throws SRUResponseParsingException, IOException {
        try {
            int tries = 0;
            do {
                try {
                    URL url = null;
                    if (this.currentPage != null && tries == 0) {
                        // attempt to fetch the next page of the
                        // current result set
                        
                        // get the current page result set id
                        String resultSetId = getValueOfFirstAncestorWithNameNS(SRWNS, "resultSetId", this.currentPage.getDocumentElement());
                
                        String urlStr = this.sruBaseUrl + "?query=" + URLEncoder.encode("cql.resultSetId=" + resultSetId, "UTF-8") + "&version=1.1&operation=searchRetrieve&maximumRecords=" + PAGE_SIZE + "&startRecord=" + (this.nextRecordIndex + 1) + (this.formatUri != null ? "&recordSchema=" + URLEncoder.encode(this.formatUri, "UTF-8") : "") + "&recordPacking=xml" + (resultSetTTL < 0 ? "" : "&resultSetTTL=" + resultSetTTL) + (sortKeys != null ? "&sortKeys=" + this.sortKeys : "");
                        url = new URL(urlStr);
                    } else {
                        String urlStr = this.sruBaseUrl + "?query=" + URLEncoder.encode(this.query, "UTF-8") + "&version=1.1&operation=searchRetrieve&maximumRecords=" + PAGE_SIZE + "&startRecord=" + (this.nextRecordIndex + 1) + (this.formatUri != null ? "&recordSchema=" + URLEncoder.encode(this.formatUri, "UTF-8") : "") + "&recordPacking=xml" + (resultSetTTL < 0 ? "" : "&resultSetTTL=" + resultSetTTL) + (sortKeys != null ? "&sortKeys=" + this.sortKeys : "");
                        url = new URL(urlStr);
                    }
                    LOG.info("Fetching Results Page: " + url);
                    this.currentPage = this.docBuilder.parse(new InputSource(url.openStream()));
            
                    this.currentPageStart = this.nextRecordIndex; 

                    // get any diagnostics
                    String diagnosticsUri = (String) this.xpath.evaluate("/srw:searchRetrieveResponse/srw:diagnostics/diag:diagnostic/diag:uri", this.currentPage, XPathConstants.STRING);
                    if (diagnosticsUri != null && diagnosticsUri != "") {
                        if (diagnosticsUri.equals("info:srw/diagnostic/1/51")) {
                            throw new RuntimeException("info:srw/diagnostic/1/51 (Result set does not exist)");
                        }
                    }
                    
                    // get the total number of records
                    this.totalRecords = Integer.parseInt(getValueOfFirstAncestorWithNameNS(SRWNS, "numberOfRecords", this.currentPage.getDocumentElement()));
            
                    // get the elements node list
                    if (this.totalRecords > 0) {
                        Element recordsEl = (Element) this.currentPage.getElementsByTagNameNS(SRWNS, "records").item(0);
                        this.recordElements = recordsEl.getElementsByTagNameNS(SRWNS, "record");
                        this.currentPageSize = this.recordElements.getLength();
                    }
                    return;
                } catch (SAXException ex) {
                    throw ex;
                } catch (Throwable t) {
                    // Some sort of error occurred, probably a result-set timeout
                    // so we should try again with the original search URL.
                    LOG.warn("Error fetching results (attempt " + (tries + 1) + "): " + t);
                    t.printStackTrace();
                }
                tries ++;
            } while (tries < 2);            
        } catch (SAXException ex) {
            throw new SRUResponseParsingException(ex);
        }
    }
    
    public int getSize() throws SRUResponseParsingException, IOException {
        if (this.totalRecords == -1) {
            this.fetchNextPage();
        }
        return this.totalRecords;
    }
    
    public int getNextRecordIndex() {
        return this.nextRecordIndex;
    }
    
    public boolean hasNext() throws SRUResponseParsingException, IOException {
        if (this.currentPage == null) {
            this.fetchNextPage();
        }
        return this.nextRecordIndex < this.totalRecords;
    }

    public Element next() throws SRUResponseParsingException, IOException {
        if (this.nextRecordIndex < this.totalRecords) {
            if (this.nextRecordIndex - this.currentPageStart >= this.currentPageSize) {
                this.fetchNextPage();
            }
            int currentPageOffset = this.nextRecordIndex - this.currentPageStart;
            this.nextRecordIndex ++;
            Element recordEl = (Element) this.recordElements.item(currentPageOffset);
            Element recordDataEl = (Element) recordEl.getElementsByTagNameNS(SRWNS, "recordData").item(0);
            if (recordDataEl == null || recordDataEl.getChildNodes().getLength() == 0) {
                LOG.error("Search result " + this.nextRecordIndex + " is not available!");
                return null;
            } else {
                return (Element) recordDataEl.getFirstChild();
            } 
        } else {
            throw new NoSuchElementException();
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    public static String getValueOfFirstAncestorWithNameNS(String namespace, String name, Element el) {
        NodeList elements = el.getElementsByTagNameNS(namespace, name);
        if (elements.getLength() == 0) {
            return null;
        } else {
            StringBuffer sb = new StringBuffer();
            NodeList children = elements.item(0).getChildNodes();
            for (int i = 0; i < children.getLength(); i ++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    sb.append(child.getNodeValue());
                } else {
                    throw new IllegalStateException();
                }
            }
            return sb.toString();
        }
    }
}
