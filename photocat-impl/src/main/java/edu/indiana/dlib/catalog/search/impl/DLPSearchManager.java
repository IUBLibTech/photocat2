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
package edu.indiana.dlib.catalog.search.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.indiana.dlib.catalog.config.DataFormatException;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.XPathHelper;
import edu.indiana.dlib.catalog.config.impl.fedora.SerializableItem;
import edu.indiana.dlib.catalog.search.BrowseQuery;
import edu.indiana.dlib.catalog.search.BrowseResult;
import edu.indiana.dlib.catalog.search.BrowseResults;
import edu.indiana.dlib.catalog.search.SearchException;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.SearchQuery;
import edu.indiana.dlib.catalog.search.SearchResults;
import edu.indiana.dlib.catalog.search.UnsupportedQueryException;
import edu.indiana.dlib.catalog.search.structured.DefaultPagingSpecification;
import edu.indiana.dlib.catalog.search.structured.DefaultStructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.DefaultStructuredSearchResults;
import edu.indiana.dlib.catalog.search.structured.SearchConstraint;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchResults;
import edu.indiana.dlib.catalog.search.structured.constraints.AndSearchConstraintGroup;
import edu.indiana.dlib.catalog.search.structured.constraints.CollectionSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.FieldPartValueSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.OrSearchConstraintGroup;
import edu.indiana.dlib.catalog.search.structured.constraints.QueryClauseSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.SerializableSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.UserQuerySearchConstraint;
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
    
    private static final String MOD_DATE_SORT_KEY = "modificationDate,,0,,lowValue";
    
    private static final String SPECIAL_COMPLETION_FIELD = "SPECIAL_COMPLETION_FIELD";
    
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
        
        this.xpath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {

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
                if (namespaceURI.equals("http://www.loc.gov/zing/srw/")) {
                    return "srw";
                } else if (namespaceURI.equals("http://www.loc.gov/zing/srw/diagnostic/")) {
                    return "diag";
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

    public StructuredSearchResults search(StructuredSearchQuery query) throws SearchException {
        try {
            this.refreshConfiguration();
            return performSRUSearch(query, null);
        } catch (Exception ex) {
            throw new SearchException(ex);
        }
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
    
    private String compileQuery(List<SearchConstraint> constraints, String resultSetId) throws Exception {
        StringBuffer query = new StringBuffer();
        if (resultSetId != null) {
            query.append("cql.resultSetId=" + resultSetId);
        } else if (constraints == null || constraints.isEmpty()) {
            return "cql.allRecords=1";
        } else {
            for (SearchConstraint constraint : constraints) {
                if (query.length() > 0) {
                    query.append(" and ");
                }
                query.append(getQueryClause(constraint));
            }
        }
        long end = System.currentTimeMillis();
        //System.out.println("JQA query translation took " + (end - start) + "ms");
        return query.toString();
    }
    
    private String getQueryClause(SearchConstraint constraint) throws ParseException, UnsupportedQueryException {
        if (constraint instanceof CollectionSearchConstraint) {
            return getCollectionIdIndexName() + "=\"" + ((CollectionSearchConstraint) constraint).getCollectionId() + "\"";
        } else if (constraint instanceof UserQuerySearchConstraint) {
            LOGGER.debug("parsing user query: " + ((UserQuerySearchConstraint) constraint).getUserQuery());
            // TODO: the parser crashes (throws a NPE) when there's a ":" and the field isn't specified.
            //       for now, we strip colon's to avoid this error.
            Parser parser = Parser.newParser(((UserQuerySearchConstraint) constraint).getUserQuery().replace(":", ""));
            ParserVisitor queryBuilderVisitor = new BuildQueryVisitor(queryMapping);
            ASTStart startNode = parser.Start();
            String clause = (String) startNode.jjtAccept(queryBuilderVisitor, "");
            if (clause.trim().length() > 0) {
                return "(" + clause + ")";
            } else {
                return "cql.allRecords=1";
            }
        } else if (constraint instanceof QueryClauseSearchConstraint) {
            return "(" + ((QueryClauseSearchConstraint) constraint).getQueryClause() + ")";
        } else if (constraint instanceof OrSearchConstraintGroup) {
            OrSearchConstraintGroup orGroup = (OrSearchConstraintGroup) constraint;
            if (orGroup.getOredConstraints() != null && !orGroup.getOredConstraints().isEmpty()) {
                StringBuffer query = new StringBuffer();
                for (SearchConstraint childConstraint : orGroup.getOredConstraints()) {
                    // recursive call
                    if (query.length() > 0) {
                        query.append(" or ");
                    }
                    query.append(getQueryClause(childConstraint));
                }
                return "(" + query.toString() + ")";
            } else {
                return "cql.allRecords=1";
            }
        } else if (constraint instanceof AndSearchConstraintGroup) {
            AndSearchConstraintGroup andGroup = (AndSearchConstraintGroup) constraint;
            if (andGroup.getAndedConstraints() != null && !andGroup.getAndedConstraints().isEmpty()) {
                StringBuffer query = new StringBuffer();
                for (SearchConstraint childConstraint : andGroup.getAndedConstraints()) {
                    // recursive call
                    if (query.length() > 0) {
                        query.append(" and ");
                    }
                    query.append(getQueryClause(childConstraint));
                }
                return "(" + query.toString() + ")";
            } else {
                return "cql.allRecords=1";
            }
        } else if (constraint instanceof FieldPartValueSearchConstraint) {
            FieldPartValueSearchConstraint sc = (FieldPartValueSearchConstraint) constraint;
            return this.getFieldPartIndexName(sc.getFieldType(), sc.getPartName()) + " exact \"" + sc.getValue().replace("\"", "\\\"") + "\"";
        } else {
            throw new UnsupportedQueryException("Unsupported query Constraint type: " + constraint.getClass().getName());
        }
    }
    
    private boolean constraintsContainsUserQuery(Collection<SearchConstraint> constraints) {
        for (SearchConstraint constraint : constraints) {
            if (constraint instanceof UserQuerySearchConstraint) {
                UserQuerySearchConstraint c = (UserQuerySearchConstraint) constraint;
                if (!c.getUserQuery().trim().equals("")) {
                    return true;
                }
            }
            if (constraint instanceof AndSearchConstraintGroup) {
                AndSearchConstraintGroup g = (AndSearchConstraintGroup) constraint;
                Collection<SearchConstraint> cc = new ArrayList<SearchConstraint>();
                for (SerializableSearchConstraint ssc : g.getAndedConstraints()) {
                    cc.add(ssc);
                }
                if (constraintsContainsUserQuery(cc)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private StructuredSearchResults performSRUSearch(StructuredSearchQuery searchQuery, String resultSetId) throws Exception {
        String query = compileQuery(searchQuery.getSearchConstraints(), resultSetId);

        long start = System.currentTimeMillis();
        String sortKeys = RECORD_ID_SORT_KEY;
        if (searchQuery.getSearchConstraints() != null && !constraintsContainsUserQuery(searchQuery.getSearchConstraints())) {
            sortKeys = MOD_DATE_SORT_KEY;
        }
        String url = this.srwBaseUrl + "?query=" + URLEncoder.encode(query.toString(), "UTF-8") + "&version=1.1&operation=&operation=searchRetrieve&recordSchema=" + URLEncoder.encode(XPathHelper.I_URI, "UTF-8") + "&maximumRecords=" + searchQuery.getPagingSpecification().getMaxRecords() + "&startRecord=" + (searchQuery.getPagingSpecification().getStartingIndex() + 1) + "&resultSetTTL=90&recordPacking=xml&sortKeys=" + sortKeys;
        LOGGER.debug("Requesting search results from: " + url);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        long end = System.currentTimeMillis();
        //System.out.println("Document builder and URL created in " + (end - start) + "ms");
        
        start = System.currentTimeMillis();
        Document searchResultsDoc = builder.parse(new URL(url).openStream());
        end = System.currentTimeMillis();
        //System.out.println("Search took " + (end - start) + "ms");
        //System.out.println(url);
        // determine if the result set timed out
        if ((Boolean) xpath.evaluate("srw:searchRetrieveResponse/srw:diagnostics/diag:diagnostic/diag:uri[text() = 'info:srw/diagnostic/1/51']", searchResultsDoc, XPathConstants.BOOLEAN)) {
            LOGGER.debug("Result set timed out, issuing original query.");
            return performSRUSearch(searchQuery, null);
        } else if (Boolean.TRUE.equals((Boolean) xpath.evaluate("srw:searchRetrieveResponse/srw:diagnostics/diag:diagnostic/diag:uri", searchResultsDoc, XPathConstants.BOOLEAN))) {
            String errorMessage = (String) xpath.evaluate("srw:searchRetrieveResponse/srw:diagnostics/diag:diagnostic/diag:uri", searchResultsDoc, XPathConstants.STRING);
            LOGGER.warn("Search returned diagnostic result: " + errorMessage);
            throw new RuntimeException("Search system returned error: " + errorMessage);
        }
        
        // parse the response
        start = System.currentTimeMillis();
        String parsedResultSetId = (String) xpath.evaluate("srw:searchRetrieveResponse/srw:resultSetId", searchResultsDoc, XPathConstants.STRING);
        String totalStr = (String) xpath.evaluate("srw:searchRetrieveResponse/srw:numberOfRecords", searchResultsDoc, XPathConstants.STRING);
        int total = new Integer(totalStr);
        List<Item> resultList = new ArrayList<Item>();
        NodeList recordsNl = (NodeList) xpath.evaluate("srw:searchRetrieveResponse/srw:records/srw:record/srw:recordData/*", searchResultsDoc, XPathConstants.NODESET);
        for (int i = 0; i < recordsNl.getLength(); i ++) {
            try {
                resultList.add(new SerializableItem(recordsNl.item(i)));
            } catch (DataFormatException ex) {
                LOGGER.error(this.srwBaseUrl + "?query=" + query.toString() + " resulted in an uparsible record!", ex);
                throw ex;
            }
        }
        end = System.currentTimeMillis();
        //System.out.println("Result parsing took " + (end - start) + "ms");
        return new DefaultStructuredSearchResults(searchQuery, resultList, total);
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
                    query = getCollectionIdIndexName() + "=\"" + searchQuery.getCollectionId() + "\"";
                    //query = "cql.allRecords=\"1\"";
                } else {
                    Parser parser = Parser.newParser(searchQuery.getEnteredQuery());
                    ParserVisitor queryBuilderVisitor = new BuildQueryVisitor(this.queryMapping);
                    ASTStart startNode = parser.Start();
                    query = (String) startNode.jjtAccept(queryBuilderVisitor, "");
                    query = query + " and " + getCollectionIdIndexName() + "=\"" + searchQuery.getCollectionId() + "\"";
                }
                if (searchQuery.getFilterQuery() != null) {
                    query = query + " and (" + searchQuery.getFilterQuery() + ")";
                }
            }
        }
        long end = System.currentTimeMillis();
        //System.out.println("JQA query translation took " + (end - start) + "ms");

        start = System.currentTimeMillis();
        String url = this.srwBaseUrl + "?query=" + URLEncoder.encode(query.toString(), "UTF-8") + "&version=1.1&operation=&operation=searchRetrieve&recordSchema=" + URLEncoder.encode(XPathHelper.I_URI, "UTF-8") + "&maximumRecords=" + searchQuery.getMaxRecords() + "&startRecord=" + (searchQuery.getStartingIndex() + 1) + "&resultSetTTL=90&recordPacking=xml&sortKeys=" + RECORD_ID_SORT_KEY;
        LOGGER.debug(url);
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
        List<Item> resultList = new ArrayList<Item>();
        NodeList recordsNl = (NodeList) xpath.evaluate("srw:searchRetrieveResponse/srw:records/srw:record/srw:recordData/*", searchResultsDoc, XPathConstants.NODESET);
        for (int i = 0; i < recordsNl.getLength(); i ++) {
            try {
                resultList.add(new SerializableItem(recordsNl.item(i)));
            } catch (DataFormatException ex) {
                LOGGER.error(this.srwBaseUrl + "?query=" + query.toString() + " resulted in an uparsible record!", ex);
                throw ex;
            }
        }
        end = System.currentTimeMillis();
        //System.out.println("Result parsing took " + (end - start) + "ms");
        return new DefaultSearchResults(searchQuery.getStartingIndex(), total, searchQuery, resultList);
    }

    public BrowseResults browse(BrowseQuery browseQuery) throws SearchException, UnsupportedQueryException {
        if (browseQuery.getBrowseSet() instanceof DateBrowseSet) {
            DateBrowseSet dbs = (DateBrowseSet) browseQuery.getBrowseSet();
            String indexName = getFieldPartIndexName(dbs.getFieldType(), dbs.getPartName());
            try {
                String query = compileQuery(browseQuery.getSearchConstraints(), null);
                String urlString = this.srwBaseUrl + "?query=" + URLEncoder.encode(query, "UTF-8") + "&version=1.1&operation=searchRetrieve&recordSchema=info%3Aphotocat%2Fitem&maximumRecords=20&startRecord=1&resultSetTTL=300&recordPacking=xml&sortKeys=&x-iudl-requestFacetInformation=" + indexName + "," + browseQuery.getPagingSpecification().getMaxRecords() + "," + browseQuery.getPagingSpecification().getStartingIndex();
                LOGGER.debug(urlString);
                
                // now do the date grouping
                SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
                List<BrowseResult> decades = new ArrayList<BrowseResult>();
                for (BrowseResult result : requestFacets(indexName, urlString)) {
                    DateFormat format = new SimpleDateFormat(dbs.getDateFormat());
                    Date date = format.parse(result.getFieldValue());
                    if (date == null) {
                        LOGGER.debug("Result \"" + result.getFieldValue() + " could not be parsed as a date!");
                    } else {
                        //  find/create the appropriate decade
                        String decadeString = yearFormat.format(date);
                        String firstYear = (decadeString.length() > 1 ? decadeString.substring(0, decadeString.length() - 1) + "0" : "0");
                        String lastYear = (decadeString.length() > 1 ? decadeString.substring(0, decadeString.length() - 1) + "9" : "9");
                        if (decadeString.length() > 1) {
                            decadeString = firstYear + "-" + lastYear;
                        }
                        DefaultBrowseResult decadeBrowseResult = null;
                        for (BrowseResult br : decades) {
                            DefaultBrowseResult d = (DefaultBrowseResult) br;
                            if (d.getFieldDisplayLabel().equals(decadeString)) {
                                decadeBrowseResult = d;
                                break;
                            }
                        }
                        if (decadeBrowseResult == null) {
                            decadeBrowseResult = new DefaultBrowseResult(decadeString, decadeString, 0, getFieldPartIndexName(dbs.getFieldType(), dbs.getPartName()) + " cql.within \"" + firstYear + " " + lastYear + "\"");
                            // TODO: better handle short dates
                            decades.add(decadeBrowseResult);
                        }
                        
                        decadeBrowseResult.addBrowseResult(result);
                    }
                }
                return new DefaultBrowseResults(browseQuery, decades);
            } catch (Exception ex) {
                throw new SearchException(ex);
            }
        } else if (browseQuery.getBrowseSet() instanceof FieldPartBrowseSet) {
            FieldPartBrowseSet fpbs = (FieldPartBrowseSet) browseQuery.getBrowseSet();
            String indexName = getFieldPartIndexName(fpbs.getFieldType(), fpbs.getPartName());
            try {
                String query = compileQuery(browseQuery.getSearchConstraints(), null);
                ArrayList<BrowseResult> results = new ArrayList<BrowseResult>();

                if (fpbs.includeEmptyValues()) {
                    BrowseResult emptyResult = getMissingValueBrowseResult(query, fpbs);
                    if (emptyResult.getHitCount() > 0) {
                        results.add(emptyResult);
                    }
                }
                
                String urlString = this.srwBaseUrl + "?query=" + URLEncoder.encode(query, "UTF-8") + "&version=1.1&operation=searchRetrieve&recordSchema=info%3Aphotocat%2Fitem&maximumRecords=20&startRecord=1&resultSetTTL=300&recordPacking=xml&sortKeys=&x-iudl-requestFacetInformation=" + indexName + "," + browseQuery.getPagingSpecification().getMaxRecords() + "," + browseQuery.getPagingSpecification().getStartingIndex();
                LOGGER.debug(urlString);
                results.addAll(requestFacets(indexName, urlString));
                return new DefaultBrowseResults(browseQuery, results);
            } catch (Exception ex) {
                throw new SearchException(ex);
            }
        } else if (browseQuery.getBrowseSet() instanceof CollectionBrowseSet) {
            try {
                CollectionBrowseSet cbs = (CollectionBrowseSet) browseQuery.getBrowseSet();
                String query = compileQuery(browseQuery.getSearchConstraints(), null);
                String urlString = this.srwBaseUrl + "?query=" + URLEncoder.encode(query, "UTF-8") + "&version=1.1&operation=searchRetrieve&recordSchema=info%3Aphotocat%2Fitem&maximumRecords=20&startRecord=1&resultSetTTL=300&recordPacking=xml&sortKeys=&x-iudl-requestFacetInformation=" + getCollectionIdIndexName() + "," + browseQuery.getPagingSpecification().getMaxRecords() + "," + browseQuery.getPagingSpecification().getStartingIndex();
                LOGGER.debug(urlString);
                return new DefaultBrowseResults(browseQuery, requestFacets(getCollectionIdIndexName(), urlString));
            } catch (Exception ex) {
                throw new SearchException(ex);
            } 
        } else if (browseQuery.getBrowseSet() instanceof EnumeratedBrowseSet) {
            // This complex type of browse cannot be serviced using a single 
            // request to the SRU server but instead requires one search for
            // each enumerated value.
            //
            // Furthermore, because of it's potentially limitless hierarchy,
            // this is implemented using a recrusive helper method.
            
            try {
                EnumeratedBrowseSet set = (EnumeratedBrowseSet) browseQuery.getBrowseSet();
                List<BrowseResult> results = new ArrayList<BrowseResult>();
                for (EnumeratedBrowseSet.Entry entry : set.getBrowseValues()) {
                    BrowseResult result = getEnumeratedBrowseResult(browseQuery, entry);
                    if (result.getHitCount() > 0) {
                        results.add(result);
                    }
                }
                return new DefaultBrowseResults(browseQuery, results);
            } catch (Exception ex) {
                throw new SearchException(ex);
            }
        } else {
            throw new UnsupportedQueryException("Unsupported query Constraint type: " + browseQuery.getBrowseSet().getClass().getName());
        }
    }
    
    private BrowseResult getMissingValueBrowseResult(String query, FieldPartBrowseSet set) throws ParserConfigurationException, MalformedURLException, SAXException, IOException, XPathExpressionException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        // Get the number of records with NO value for this browse field and
        // add a BrowseResult for it if there are more than zero.
        String emptyQuery = URLEncoder.encode("(" + query + ")" + " not " + SPECIAL_COMPLETION_FIELD + " = \"" + getFieldPartIndexName(set.getFieldType(), set.getPartName()) + "\"", "UTF-8"); 
        String url = this.srwBaseUrl + "?query=" + emptyQuery + "&version=1.1&operation=&operation=searchRetrieve&recordSchema=" + URLEncoder.encode(XPathHelper.I_URI, "UTF-8") + "&maximumRecords=0&startRecord=1&resultSetTTL=0&recordPacking=xml&sortKeys=";
        LOGGER.debug(url);
        Document searchResultsDoc = builder.parse(new URL(url).openStream());
        String totalStr = (String) xpath.evaluate("srw:searchRetrieveResponse/srw:numberOfRecords", searchResultsDoc, XPathConstants.STRING);
        int count = Integer.parseInt(totalStr);
        return new DefaultBrowseResult("", count, emptyQuery);
    }
        
    private BrowseResult getEnumeratedBrowseResult(BrowseQuery browseQuery, EnumeratedBrowseSet.Entry entry) throws Exception {
        if (entry.getValue() != null) {
            // simple case, do a single search, count the results
            String queryClause = getPartExactMatchQueryClause(entry.getFieldType(), entry.getPartName(), entry.getValue());
            List<SearchConstraint> constraints = new ArrayList<SearchConstraint>(browseQuery.getSearchConstraints());
            constraints.add(new QueryClauseSearchConstraint(queryClause));
            StructuredSearchQuery searchQuery = new DefaultStructuredSearchQuery(new DefaultPagingSpecification().pageSize(0), constraints);
            int hitCount = performSRUSearch(searchQuery, null).getTotalResultsCount();
            return new DefaultBrowseResult(entry.getValue(), entry.getDisplayName(), hitCount, queryClause);
        } else {
            // more complex case, make recursive calls for each child entry and
            // add up the counts and combine the query clauses
            StringBuffer queryClause = new StringBuffer();
            int hitCount = 0;
            List<BrowseResult> results = new ArrayList<BrowseResult>();
            for (EnumeratedBrowseSet.Entry childEntry : entry.getEntries()) {
                BrowseResult result = getEnumeratedBrowseResult(browseQuery, childEntry);
                if (result.getHitCount() > 0) {
                    results.add(result);
                    hitCount += result.getHitCount();
                    if (queryClause.length() > 0) {
                        queryClause.append(" OR " + result.getQuery());
                    } else {
                        queryClause.append(result.getQuery());
                    }
                }
            }
            return new DefaultBrowseResult(entry.getValue(), entry.getDisplayName(), hitCount, queryClause.toString(), results);
        }
        
    }

    private String getCollectionIdIndexName() {
        return "collectionId";
    }

    private List<BrowseResult> requestFacets(String indexName, String urlString) throws ParserConfigurationException, MalformedURLException, SAXException, IOException {
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
                                    String value = "";
                                    if (valueEl != null && valueEl.getFirstChild() != null) {
                                        value = valueEl.getFirstChild().getNodeValue();
                                    }
                                    results.add(new DefaultBrowseResult(value, Integer.parseInt(valueEl.getAttribute("hits")), URLEncoder.encode(indexName + " exact \"" + value + "\"", "UTF-8")));
                                }
                            }
                        }
                    }
                    return results;
                }
            }
        }
        return Collections.emptyList();
    }    

    public String getFieldAttributeIndexName(String fieldType, String attributeName) {
        return translate(fieldType, INVALID_CHARS, VALID_ALTERNATIVE_CHARS) + "-attribute-" + translate(attributeName, INVALID_CHARS, VALID_ALTERNATIVE_CHARS);
    }
    
    public String getFieldPartIndexName(String fieldType, String partName) {
        return translate(fieldType, INVALID_CHARS, VALID_ALTERNATIVE_CHARS) + "-part-" + translate(partName, INVALID_CHARS, VALID_ALTERNATIVE_CHARS);
    }
    
    public String getAttributeExactMatchQueryClause(String fieldType, String attributeName, String value) {
        return getFieldAttributeIndexName(fieldType, attributeName) + " exact \"" + value.replace("\"", "\\\"") + "\""; 
    }

    public String getPartExactMatchQueryClause(String fieldType, String partName, String value) {
        return getFieldPartIndexName(fieldType, partName) + " exact \"" + value.replace("\"", "\\\"") + "\""; 
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
    
    public static String translateIndexName(String indexName) {
        return translate(indexName, INVALID_CHARS, VALID_ALTERNATIVE_CHARS);
    }
    
}
