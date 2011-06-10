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
package edu.indiana.dlib.catalog.search.impl;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.indiana.dlib.catalog.config.DataFormatException;
import edu.indiana.dlib.catalog.config.ItemMetadata;
import edu.indiana.dlib.catalog.config.XPathHelper;
import edu.indiana.dlib.catalog.config.impl.ElementItemMetadata;
import edu.indiana.dlib.catalog.search.BrowseQuery;
import edu.indiana.dlib.catalog.search.BrowseResult;
import edu.indiana.dlib.catalog.search.BrowseResults;
import edu.indiana.dlib.catalog.search.SearchException;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.SearchQuery;
import edu.indiana.dlib.catalog.search.SearchResults;
import edu.indiana.dlib.catalog.search.UnsupportedQueryException;
import edu.indiana.dlib.jqa.parser.ASTStart;
import edu.indiana.dlib.jqa.parser.ParseException;
import edu.indiana.dlib.jqa.parser.Parser;
import edu.indiana.dlib.jqa.parser.ParserVisitor;
import edu.indiana.dlib.jqa.parser.conf.ConfigurationException;
import edu.indiana.dlib.jqa.parser.conf.Configurator;
import edu.indiana.dlib.jqa.parser.visitor.BuildQueryVisitor;

/**
 * A SearchManager implementation that uses SRW for searching 
 * and JQA for query parsing.
 */
public class DLPSearchManager implements SearchManager {

    private Logger LOGGER = Logger.getLogger(DLPSearchManager.class);

    /**
     * Characters not supported in index names by some layer of the
     * underlying search implementation.
     */
    private static final String INVALID_CHARS = " ~`@#$%^&*()+=-\\|]}[{;:'\"?/>.<;,";

    /**
     * Characters that should be used instead of the invalid characters
     * specified in INVALID_CHARS.
     */
    private static final String VALID_ALTERNATIVE_CHARS = "_";
    
    private static final String RECORD_ID_SORT_KEY = "dc.identifier,,1,,lowValue";
    
    private long lastRefreshed;
    
    private String parserConfigLocation;

    private String queryMapping;
    
    private String srwBaseUrl;
    
    private XPath xpath;
    
    public DLPSearchManager(String parserConfigLocation, String queryMapping, String srwBaseUrl) {
        this.parserConfigLocation = parserConfigLocation;
        this.queryMapping = queryMapping;
        this.srwBaseUrl = srwBaseUrl;
        this.lastRefreshed = 0;
        
        this.xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {

            public String getNamespaceURI(String prefix) {
                if (prefix.equals("srw")) {
                    return "http://www.loc.gov/zing/srw/";
                } else {
                    return null;
                }
            }

            public String getPrefix(String namespaceURI) {
                if (namespaceURI.equals("http://www.loc.gov/zing/srw/")) {
                    return "srw";
                } else {
                    return null;
                }
            }

            public Iterator getPrefixes(String namespaceURI) {
                return null;
            }});
    }
    
    private void refreshConfiguration() throws ConfigurationException {
        File parserConfigFile = new File(this.parserConfigLocation);
        if (this.lastRefreshed < parserConfigFile.lastModified()) {
            synchronized (this) {
                Configurator.configure(this.parserConfigLocation);
            }
        }

    }
    
    public String getSyntaxNotes() {
        return "In addition to simple searches, complex searches can be conducted. Wildcards (*), exact phrase (\"\") and Boolean (AND, OR, NOT) can be combined to form a query";
    }

    public SearchResults search(SearchQuery query) throws SearchException, UnsupportedQueryException {
        try {
            this.refreshConfiguration();
            //return performSRWSearch(query, null);
            return performSRUSearch(query, null);
        } catch (ConfigurationException ex) {
            throw new SearchException(ex);
        } catch (RemoteException ex) {
            throw new SearchException(ex);
        } catch (MalformedURLException ex) {
            throw new SearchException(ex);
        } catch (ParseException ex) {
            throw new UnsupportedQueryException(ex);
        } catch (DataFormatException ex) {
            throw new SearchException(ex);
        } catch (Exception ex) {
            throw new SearchException(ex);
        }
    }
    private SearchResults performSRUSearch(SearchQuery searchQuery, String resultSetId) throws Exception {
        long start = System.currentTimeMillis();
        String query = null;
        if (resultSetId != null) {
            query = "cql.resultSetId=" + resultSetId;
        } else {
            // Expand the query using JQA
            synchronized (this) {
                if (searchQuery.getEnteredQuery() == null || searchQuery.getEnteredQuery().equals("")) {
                    query = "collectionId=\"" + searchQuery.getCollectionId() + "\"";
                    //query = "cql.allRecords=\"1\"";
                } else {
                    Parser parser = Parser.newParser(searchQuery.getEnteredQuery());
                    ParserVisitor queryBuilderVisitor = new BuildQueryVisitor(this.queryMapping);
                    ASTStart startNode = parser.Start();
                    query = (String) startNode.jjtAccept(queryBuilderVisitor, "");
                    query = query + " and collectionId=\"" + searchQuery.getCollectionId() + "\"";
                }
                if (searchQuery.getFilterQuery() != null) {
                    query = query + " and (" + searchQuery.getFilterQuery() + ")";
                }
            }
        }
        long end = System.currentTimeMillis();
        //System.out.println("JQA query translation took " + (end - start) + "ms");

        start = System.currentTimeMillis();
        String url = this.srwBaseUrl + "?query=" + URLEncoder.encode(query.toString(), "UTF-8") + "&version=1.1&operation=&operation=searchRetrieve&recordSchema=" + URLEncoder.encode(XPathHelper.M_URI, "UTF-8") + "&maximumRecords=" + searchQuery.getMaxRecords() + "&startRecord=" + (searchQuery.getStartingIndex() + 1) + "&resultSetTTL=90&recordPacking=xml&sortKeys=" + RECORD_ID_SORT_KEY;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        end = System.currentTimeMillis();
        //System.out.println("Document builder and URL created in " + (end - start) + "ms");
        
        start = System.currentTimeMillis();
        Document searchResultsDoc = builder.parse(new URL(url).openStream());
        end = System.currentTimeMillis();
        //System.out.println("Search took " + (end - start) + "ms");
        //System.out.println(url);
        // determine if the result set timed out
        if ((Boolean) xpath.evaluate("srw:searchRetrieveResponse/srw:diagnostics/srw:diagnostic/srw:uri[text() = 'info:srw/diagnostic/1/51']", searchResultsDoc, XPathConstants.BOOLEAN)) {
            LOGGER.debug("Result set timed out, issuing original query.");
            return performSRUSearch(searchQuery, null);
        }
        
        // parse the response
        start = System.currentTimeMillis();
        String parsedResultSetId = (String) xpath.evaluate("srw:searchRetrieveResponse/srw:resultSetId", searchResultsDoc, XPathConstants.STRING);
        String totalStr = (String) xpath.evaluate("srw:searchRetrieveResponse/srw:numberOfRecords", searchResultsDoc, XPathConstants.STRING);
        int total = new Integer(totalStr);
        List<ItemMetadata> resultList = new ArrayList<ItemMetadata>();
        NodeList recordsNl = (NodeList) xpath.evaluate("srw:searchRetrieveResponse/srw:records/srw:record/srw:recordData/*", searchResultsDoc, XPathConstants.NODESET);
        for (int i = 0; i < recordsNl.getLength(); i ++) {
            try {
                resultList.add(new ElementItemMetadata(recordsNl.item(i).getParentNode()));
            } catch (DataFormatException ex) {
                LOGGER.error(this.srwBaseUrl + "?query=" + query.toString() + " resulted in an uparsible record!", ex);
                throw ex;
            }
        }
        end = System.currentTimeMillis();
        //System.out.println("Result parsing took " + (end - start) + "ms");
        return new DefaultSearchResults(searchQuery.getStartingIndex(), total, searchQuery, resultList);
    }

    public BrowseResults browse(BrowseQuery browseQuery) throws SearchException {
        /*
         * The following uses the facet extension.
         */
        String query = "collectionId=\"" + browseQuery.getCollectionId() + "\"";
        try {
            String urlString = this.srwBaseUrl + "?query=" + URLEncoder.encode(query, "UTF-8") + "&version=1.1&operation=searchRetrieve&recordSchema=info%3Aphotocat%2Fmetadata&maximumRecords=20&startRecord=1&resultSetTTL=300&recordPacking=xml&sortKeys=&x-iudl-requestFacetInformation=" + getFieldPartIndexName(browseQuery.getFieldType(), browseQuery.getPartName()) + "," + browseQuery.getMaxRecords() + "," + browseQuery.getStartingIndex();
            LOGGER.debug(urlString);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document searchResultsDoc = builder.parse(new URL(urlString).openStream());
            NodeList extraResponseDataNL = searchResultsDoc.getElementsByTagName("extraResponseData");
            List<BrowseResult> results = new ArrayList<BrowseResult>();
            if (extraResponseDataNL.getLength() != 0) {
                Element extraResponseDataEl = (Element) extraResponseDataNL.item(0);
                NodeList childrenOfExtraResponseNL = extraResponseDataEl.getChildNodes();
                for (int i = 0; i < childrenOfExtraResponseNL.getLength(); i ++) {
                    Node node = childrenOfExtraResponseNL.item(i);
                    if (node instanceof Element && ((Element) node).getNodeName().endsWith("facetInformation")) {
                        NodeList facetChildrenNL = ((Element) node).getChildNodes();
                        for (int j = 0; j < facetChildrenNL.getLength(); j ++) {
                            Node facetInfoChildNode = facetChildrenNL.item(j);
                            if (facetInfoChildNode instanceof Element && ((Element) facetInfoChildNode).getNodeName().endsWith("field")) {
                                Element fieldEl = (Element) facetInfoChildNode;
                                //List<SearchFacetValue> values = new ArrayList<SearchFacetValue>();
                                NodeList fieldChildrenNL = fieldEl.getChildNodes();
                                for (int k = 0; k < fieldChildrenNL.getLength(); k ++) {
                                    Node fieldChildNode = fieldChildrenNL.item(k);
                                    if (fieldChildNode instanceof Element && ((Element) fieldChildNode).getNodeName().endsWith("value")) {
                                        Element valueEl = (Element) fieldChildNode;
                                        if (valueEl != null && valueEl.getFirstChild() != null) {
                                            results.add(new DefaultBrowseResult(valueEl.getFirstChild().getNodeValue(), Integer.parseInt(valueEl.getAttribute("hits")), URLEncoder.encode(getFieldPartIndexName(browseQuery.getFieldType(), browseQuery.getPartName()) + " exact \"" + fieldEl.getAttribute("name") + "\"", "UTF-8")));
                                        } else {
                                            results.add(new DefaultBrowseResult("--", Integer.parseInt(valueEl.getAttribute("hits")), URLEncoder.encode(getFieldPartIndexName(browseQuery.getFieldType(), browseQuery.getPartName()) + " exact \"" + fieldEl.getAttribute("name") + "\"", "UTF-8")));
                                        }
                                    }
                                }
                            }
                        }
                        return new DefaultBrowseResults(browseQuery, results);
                    }
                }
            }
        } catch (Exception ex) {
            throw new SearchException(ex);
        }
        /*
         * This is a SCAN based implementation that won't work because it can't limit to a collection
        try {
            for (ScanTerm term : SRUScanResponseParser.getScanResponse(this.srwBaseUrl, getIndexName(browseQuery.getFieldType(), browseQuery.getPartName()) + "=\"\"", browseQuery.getStartingIndex(), browseQuery.getMaxRecords())) {
                System.out.println(term.getValue() + " " + term.getNumberOfRecords());
            }
        } catch (IOException ex) {
            throw new SearchException(ex);
        }
        */
        List<BrowseResult> noResults = Collections.emptyList();
        return new DefaultBrowseResults(browseQuery, noResults);
    }
    
    public String getFieldAttributeIndexName(String fieldType, String attributeName) {
        return translate(fieldType, INVALID_CHARS, VALID_ALTERNATIVE_CHARS) + "-attribute-" + translate(attributeName, INVALID_CHARS, VALID_ALTERNATIVE_CHARS);
    }
    
    public String getFieldPartIndexName(String fieldType, String partName) {
        return translate(fieldType, INVALID_CHARS, VALID_ALTERNATIVE_CHARS) + "-part-" + translate(partName, INVALID_CHARS, VALID_ALTERNATIVE_CHARS);
    }
    
    public String getAttributeExactMatchQueryClause(String fieldType, String attributeName, String value) {
        return getFieldAttributeIndexName(fieldType, attributeName) + " exact \"" + value.replace("\"", "\\\""); 
    }

    public String getPartExactMatchQueryClause(String fieldType, String partName, String value) {
        return getFieldPartIndexName(fieldType, partName) + " exact \"" + value.replace("\"", "\\\""); 
    }
    
    /**
     * Behaves like the "translate()" xpath function.
     */
    private static String translate(String source, String invalidChars, String alternativeChars) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < source.length(); i ++) {
            char c = source.charAt(i);
            int replacementIndex = invalidChars.indexOf(c);
            if (replacementIndex == -1) {
                result.append(c);
            } else {
                if (alternativeChars.length() > replacementIndex) {
                    result.append(alternativeChars.charAt(replacementIndex));
                }
            }
        }
        return result.toString();
    }


    
}
