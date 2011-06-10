/*
 * Copyright 2011, Trustees of Indiana University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   
 *   Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *   
 *   Neither the name of Indiana University nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */
package edu.indiana.dlib.catalog.vocabulary.impl.srw;

import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A wrapper around an SRW search results set that behaves
 * kind of like a list, except that it is read only and it
 * only fetches elements from the underlying server as 
 * needed.
 */
public class SearchResultsPseudoList {
    
    public static final String SRWNS = "http://www.loc.gov/zing/srw/";
    
    private static Log LOG = LogFactory.getLog(SearchResultsPseudoList.class);
    
    private DocumentBuilder docBuilder;
    
    private Element[] buffer;

    private String query;
    
    private String sruBaseUrl;
    
    private String resultSetId;
    
    private String recordFormatUri;
    
    private int resultSetTTL;
    
    private String sortKeys;
    
    private int pageSize;
    
    public SearchResultsPseudoList(String query, String sruBaseUrl, String recordFormatUri, String sortKeys, int pageSize) throws ParserConfigurationException {
        this.query = query;
        this.sruBaseUrl = sruBaseUrl;
        this.recordFormatUri = recordFormatUri;
        this.sortKeys = sortKeys;
        this.pageSize = pageSize;
        this.resultSetTTL = 30;
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        this.docBuilder = factory.newDocumentBuilder();
    }

    public void printBuffer() {
        if (buffer == null) {
            System.out.println("null");
        }
        for (int i = 0; i < buffer.length && i < 80; i ++) {
            System.out.print(buffer[i] == null ? "0" : "1");
        }
        System.out.println();
    }
    
    /**
     * Gets the index'th record in the result set.  If needed
     * this call will initiate one or MORE SRU requests.
     * @throws SAXException 
     */
    public Element get(int index) throws SAXException {
        if (this.buffer == null || this.getSize() <= index || this.buffer[index] == null) {
            return this.fetchRecords(index);
        } else {
            return this.buffer[index];
        }
    }

    /**
     * Gets the size of the SRU result set.
     * @throws SAXException 
     */
    public int getSize() throws SAXException {
        if (this.buffer == null) {
            this.fetchRecords(0);
        }
        return this.buffer.length;
    }
    
    /**
     * Fetches records including the given index
     * and returns the Element representing the
     * XML record for that index.
     * @throws SAXException 
     */
    private Element fetchRecords(int index) throws SAXException {
        int tries = 0;
        do {
            try {
                URL url = null;
                StringBuffer urlSb = new StringBuffer();
                urlSb.append(this.sruBaseUrl + "?query=");
                if (this.resultSetId != null && tries == 0) {
                    urlSb.append(URLEncoder.encode("cql.resultSetId=" + this.resultSetId, "UTF-8"));
                } else {
                    urlSb.append(URLEncoder.encode(this.query, "UTF-8"));
                }
                urlSb.append("&version=1.1&operation=searchRetrieve&maximumRecords=" + this.pageSize + "&startRecord=" + (index + 1) + (this.recordFormatUri != null ? "&recordSchema=" + URLEncoder.encode(this.recordFormatUri, "UTF-8") : "") + "&recordPacking=xml" + (this.resultSetTTL < 0 ? "" : "&resultSetTTL=" + this.resultSetTTL) + (sortKeys != null ? "&sortKeys=" + this.sortKeys : ""));
                url = new URL(urlSb.toString());
                LOG.info("Fetching Results Page: " + url);
                Document currentPage = this.docBuilder.parse(new InputSource(url.openStream()));

                // get the resultSetId
                //String resultSetId = getValueOfFirstAncestorWithNameNS(SRWNS, "resultSetId", currentPage.getDocumentElement());
                
                // get the total number of records
                int totalRecordCount = Integer.parseInt(getValueOfFirstAncestorWithNameNS(SRWNS, "numberOfRecords", currentPage.getDocumentElement()));
                if (this.buffer == null) {
                    this.buffer = new Element[totalRecordCount];
                } else {
                    if (this.buffer.length != totalRecordCount) {
                        throw new RuntimeException("ResultSet changed length!");
                    }
                }
                
                // get the elements node list
                if (this.buffer.length > 0) {
                    Element recordsEl = (Element) currentPage.getElementsByTagNameNS(SRWNS, "records").item(0);
                    NodeList recordsNl = recordsEl.getElementsByTagNameNS(SRWNS, "record");
                    for (int i = 0; i < recordsNl.getLength(); i ++) {
                        Element recordEl = (Element) recordsNl.item(i);
                        Element recordDataEl = (Element) recordEl.getElementsByTagNameNS(SRWNS, "recordData").item(0);
                        if (recordDataEl == null || recordDataEl.getChildNodes().getLength() == 0) {
                            LOG.error("Search result " + (i + index) + " is not available!");
                            buffer[index + i] = null;
                        } else {
                            buffer[index + i] = (Element) recordDataEl.getFirstChild();
                        } 
                    }
                }
                if (index < this.buffer.length) {
                    return buffer[index];
                } else {
                    return null;
                }
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
        return null;
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
