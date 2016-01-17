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
package ORG.oclc.os.SRW.Lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.axis.MessageContext;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.types.PositiveInteger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLTermNode;

import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.SRWDatabaseImpl;
import ORG.oclc.os.SRW.SRWDiagnostic;
import ORG.oclc.os.SRW.shared.FacetExtensionHandler;
import ORG.oclc.os.SRW.shared.HighlighterExtensionHandler;
import edu.indiana.dlib.robusta.cache.SimpleCache;
import edu.indiana.dlib.search.facets.FacetRequestInfo;
import edu.indiana.dlib.search.facets.SearchFacet;
import edu.indiana.dlib.search.indexing.DefaultFieldConfiguration;
import edu.indiana.dlib.search.indexing.FieldConfiguration;
import gov.loc.www.zing.srw.ScanRequestType;
import gov.loc.www.zing.srw.ScanResponseType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;
import gov.loc.www.zing.srw.SearchRetrieveResponseType;
import gov.loc.www.zing.srw.TermType;
import gov.loc.www.zing.srw.TermTypeWhereInList;
import gov.loc.www.zing.srw.TermsType;


/**
 * A "database" connector between the OCLC SRW server and
 * any lucene index. 
 * 
 * This class is a heavily modified version of the DSpace-oriented SRWLuceneDatabase 
 * originally written by Ralph LeVan (ORG.oclc.os.SRW.DSpaceLucene.SRWLuceneDatabase).
 *
 * @author levan
 * @author Ryan Scherle
 * @author midurbin
 */
public class LuceneDatabase extends SRWDatabaseImpl {
    
    static Log PERFORMANCE_LOG = LogFactory.getLog("performance." + LuceneDatabase.class.getName());
    static Log USABILITY_LOG = LogFactory.getLog("usability." + LuceneDatabase.class.getName());
    
    static Log log = LogFactory.getLog(LuceneDatabase.class);
    
    private static final String[] POSSIBLE_RELATIONS = new String[] { "=", "exact", "any", "all", ">", ">=", "<", "<=" };
    
    private FieldConfiguration fieldConfig;

    private Properties dbProperties;
    
    private QueryParser luceneParser;
    
    private Analyzer analyzer;

    /**
     * Indicates the minimum number of millisecond between reloading
     * the index searcher from disc.  If this number is set to zero,
     * searches will always be performed against the current version
     * of the index.  This may seem desirable, but in the event that
     * the index is constantly changing, every search will be performed
     * against a different index, and for the period of time while 
     * that result set is still cached, resources (memory, file handles,
     * etc.) will be retained.  During heavy usage it's possible to
     * quickly use all of the file handles which will result in the
     * entire JVM crashing... not just this web application.  For that
     * reason, it's recommended that this number be set to a period of
     * time that's longer than the time a result set is cached.
     */
    private long minimumMsBetweenIndexRefreshes = 60000;
    
    private long lastIndexRefresh;
    
    private Searcher indexSearcher;
    private List<IndexReader> indexReaders;
    
    private SimpleCache<String, List<String>> facetCache;
    
    public LuceneDatabase() {
        log.debug("initial instantiation");
    }
    
    public void addRenderer(String schemaName, String schemaID, Properties props)
        throws InstantiationException {
    }
    
    protected Object createTransformer(final String schemaName,
      final String fileName) {
        if(fileName.indexOf("default")>=0) {
            transformers.put(schemaName, fileName);
            return fileName;
        }
        return null;
    }
    
    /**
     * <p>
     *   Determines whether any requests for extra data were recognized
     *   on the SearchRetrieveRequest and returns an XML String containing
     *   the response.
     * </p>
     * <p>
     *   The current implementation only recognizes requests with the 
     *   namespace <em>http://www.dlib.indiana.edu/xml/sruFacetedSearch/version1.0/</em>.
     * </p>
     */
    public String getExtraResponseData(QueryResult result, SearchRetrieveRequestType request) {
        List<FacetRequestInfo> requestedFacets = FacetExtensionHandler.parseRequest(request);
        LuceneSearchResult lsr = (LuceneSearchResult) result;
        try {
            List<SearchFacet> facets = FacetCalculator.getFacets(this.luceneParser, lsr.getQuery(), lsr.getHits(), requestedFacets, this.getFacetToValueMap(requestedFacets), this.fieldConfig, this.getSearcher());
            return FacetExtensionHandler.getExtraResponseInfoForFacetsAsString(facets, requestedFacets);
        } catch (TransformerConfigurationException ex) {
            log.error(ex);
        } catch (TransformerException ex) {
            log.error(ex);
        } catch (TransformerFactoryConfigurationError ex) {
            log.error(ex);
        } catch (Throwable t) {
            log.error(t);
        }
        return null;
    }
    
    /**
     * Gets the {@code IndexSearcher} for this lucene
     * database.
     * @return a current {@code Searcher}
     * @throws IOException if an error occurs while 
     * refreshing the {@code IndexSearcher}
     */
    public Searcher getSearcher() throws IOException {
        if (this.indexReaders == null || this.indexReaders.isEmpty()) {
            return null;
        } else {
            long msSinceLastRefresh = System.currentTimeMillis() - this.lastIndexRefresh;
            if (msSinceLastRefresh > this.minimumMsBetweenIndexRefreshes) {
                boolean detectedChange = false;
                for (int i = 0; i < this.indexReaders.size(); i ++) {
                    IndexReader reader = this.indexReaders.get(i);
                    if (!reader.isCurrent()) {
                        detectedChange = true;
                        try {
                            this.indexReaders.set(i, IndexReader.open(reader.directory(), true));
                            reader.close();
                        } catch (IOException ex) {
                            log.error("Unable to reopen modified index!", ex);
                        }
                    }
                }
                if (detectedChange || this.indexSearcher == null) {
                    try {
                        this.facetCache.invalidate();
                    } catch (Exception ex) {
                        log.warn("Error invalidating cache!", ex);
                    }
                    long start = System.currentTimeMillis();
                    if (this.indexSearcher != null) {
                        this.indexSearcher.close();
                    }
                    if (this.indexReaders.size() == 1) {
                        this.indexSearcher = new IndexSearcher(this.indexReaders.get(0));
                    } else {
                        this.indexSearcher = createMultiSearcher(this.indexReaders);
                    }
                    long end = System.currentTimeMillis();
                    log.info("Refreshed index Searcher in response to a detected update in " + (end - start) + "ms.");
                    this.lastIndexRefresh = end;
                }
            } else {
                log.debug("Did not consider refreshing searcher because it has only been " + msSinceLastRefresh + "ms since last refresh.");
            }
            return this.indexSearcher;
        }
    }
    
    /**
     * Re-creates all of the constituent Searchers and uses them
     * to create a new MultiSearcher which is returned.
     */
    private static MultiSearcher createMultiSearcher(List<IndexReader> readers) throws IOException {
        Searcher[] searchers = new Searcher[readers.size()];
        for (int i = 0; i < readers.size(); i ++) {
            searchers[i] = new IndexSearcher(readers.get(i));
        }
        return new MultiSearcher(searchers);
    }
    
    private Map<String, List<String>> getFacetToValueMap(List<FacetRequestInfo> facets) throws IOException {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        for (FacetRequestInfo fri : facets) {
            String facet = fri.facetFieldName;
            List<String> values = null;
            try {
                values = this.facetCache.getItem(facet);
            } catch (Exception ex) {
                log.warn("Error getting facets from cache.", ex);
            }
            if (values == null) {
                try {
                    values = FacetCalculator.getSortedFieldsForFacet(facet, this.fieldConfig, this.indexReaders);
                    log.debug(values.size() + " facet values for \"" + facet + "\" calculated and cached.");
                } catch (Exception ex) {
                    log.error("Error getting facet values for \"" + facet + "\".");
                }
                try {
                    this.facetCache.cacheObject(facet, values);
                } catch (Exception ex) {
                    log.error("Error writing facets to cache.", ex);
                }
            } else {
                log.debug(values.size() + " facet values for \"" + facet + "\" loaded from cache.");
            }
            map.put(facet, values);
        }
        return map;
    }

    public String getIndexInfo() {
        Enumeration enumer = dbProperties.propertyNames();

        // A mapping of set names to set identifiers.
        // Example:
        //   cql --> info:srw/cql-context-set/1/cql-v1.1
        //   dc --> info:srw/cql-context-set/1/dc-v1.1
        Map<String, String> indexMap = new HashMap<String, String>(); 
        
        String index, indexSet, prop;
        StringBuffer sb = new StringBuffer("        <indexInfo>\n");
        StringTokenizer st;
        while(enumer.hasMoreElements()) {
            prop = (String) enumer.nextElement();
            if (prop.startsWith("qualifier.")) {
                st = new StringTokenizer(prop.substring(10));
                index = st.nextToken();
                st = new StringTokenizer(index, ".");
                if (st.countTokens()==1) {
                    indexSet="local";
                    index=prop.substring(10);
                }
                else {
                    indexSet = st.nextToken();
                    index = prop.substring(10+indexSet.length()+1);
                }
                log.debug("indexSet="+indexSet+", index="+index);
                if(!indexMap.containsKey(indexSet)) {
                    // new index set
                    String setId = dbProperties.getProperty("indexSet." + indexSet);
                    String setName = indexSet;
                    sb.append("          <set identifier=\"" + setId + "\" name=\"" + setName + "\"/>\n");
                    indexMap.put(setName, setId);
                }
                String indexTitle = indexSet + "." + index;
                sb.append("          <index" 
                        + (dbProperties.getProperty(prop).indexOf("scan") != -1 ? " scan=\"true\"" : " scan=\"false\"")
                        + (dbProperties.getProperty(prop).indexOf("search") != -1 ? " search=\"true\"" : " search=\"false\"")
                        + ">\n"
                        + "            <title>" + indexTitle + "</title>\n"
                        + "            <map>\n"
                        + "              <name set=\"" + indexSet + "\">" + index + "</name>\n"
                        + "            </map>\n");
                
                sb.append("            <configInfo>\n");
                for (String relation : POSSIBLE_RELATIONS) {
                    if (supportsRelation(relation, indexTitle, this.dbProperties)) {
                        sb.append("              <supports type=\"relation\">" + relation + "</supports>\n");
                    }
                }
                sb.append("            </configInfo>\n");

                sb.append("          </index>\n");
            }
        }
        sb.append("          </indexInfo>\n");
        return sb.toString();
    }
  
    /**
     * Overrides the superclass to add necessary validation and 
     * diagnostics.
     */
    public SearchRetrieveResponseType doRequest(
            SearchRetrieveRequestType request) throws ServletException {
        long start = System.currentTimeMillis();
        // validate the cql version
        
        // validate the presence of mandatory parameters
        
        // check for extra parameters
        
        // parse query and check syntax
        
        // parse bad index (this may be tricky since we secretly support dozens of unpublished indices)
        SearchRetrieveResponseType response = null;
        try {
            response = super.doRequest(request);
            return response;
        } finally {
            long end = System.currentTimeMillis();
            PERFORMANCE_LOG.info(super.dbname + ": " + (end - start) + "ms (Processed search request for query, \"" + request.getQuery() + "\" with sort keys, \"" + request.getSortKeys() + "\", and returned " + (response == null ? " ERROR " : response.getNumberOfRecords()) + " records)");
            USABILITY_LOG.info("usability|" + super.dbname + "|" + request.getQuery() + "|" + (response == null ? "0" : response.getNumberOfRecords()) + "|" + request.getStartRecord() + "|" + request.getMaximumRecords() + "|" + request.getSortKeys() + "|");
        }
        
    }
    
    public QueryResult getQueryResult(String query, SearchRetrieveRequestType request) throws InstantiationException {
        log.debug("entering getQueryResult");
      
        MessageContext msgContext=MessageContext.getCurrentContext();

        try {
            String pathInfo=((HttpServletRequest)msgContext.getProperty(org.apache.axis.transport.http.HTTPConstants.MC_HTTP_SERVLETREQUEST)).getPathInfo();
            log.debug("pathInfo="+pathInfo);

            // convert the query from CQL form to Lucene form
            log.info("Query:          " +query);
            CQLNode root = parser.parse(query);
            //logQueryTree(root);
            String luceneStringQuery = LuceneHelper.makeLuceneQuery(root, this.fieldConfig, this.oldResultSets);
            log.info("Lucene query:   " + luceneStringQuery);
            
            // send the query to Lucene
            Query luceneQuery = luceneParser.parse(luceneStringQuery);
            Hits hits = null;
            log.info("SortKeys:       " + request.getSortKeys());
            if (request.getSortKeys() != null) {
                try {
                    //docs = this.getSearcher().search(luceneQuery, LuceneHelper.getLuceneSort(request.getSortKeys(), this.fieldConfig, fieldConfig), ??);
                    hits = this.getSearcher().search(luceneQuery, LuceneHelper.getLuceneSort(request.getSortKeys(), this.fieldConfig));
                } catch (Throwable t) {
                    log.error("Error performing search!", t);
                    hits = this.getSearcher().search(luceneQuery);
                }
            } else {
                hits = this.getSearcher().search(luceneQuery);
            }
            log.debug(hits.length() + " total matching documents");

            // now instantiate the results and put them in their buckets
            log.info("Analyzed Query: " + luceneQuery);
            // Rewrite query to allow highlighting
            if (this.indexReaders.size() == 1) {
            	luceneQuery = luceneQuery.rewrite(this.indexReaders.get(0));
            }
            log.info("Rewritten Query: " + luceneQuery);
            return new LuceneSearchResult(hits, luceneQuery, this.analyzer, luceneStringQuery, dbProperties, HighlighterExtensionHandler.parseRequest(request), this.fieldConfig);
        } catch(Exception e) {
            LuceneSearchResult eresult = new LuceneSearchResult(null, null, null, query, dbProperties, null, null);
            log.error("problem getting results", e);
            return eresult;
        }
    }
    
    /**
     * Takes a (short-form) schemaName and returns the equivalent (long-form) schemaID.
     * If the schemaName isn't defined for this server, but it does match an existing
     * schemaID, that schemaID is returned. This method overrides getSchemaID in SRWDatabaseImpl.
     */
    public String getSchemaID(String schemaName) {
        log.debug("finding mapping for schemaName |" + schemaName + "|");
        String schemaID = dbProperties.getProperty(schemaName + ".identifier");
        
        if(schemaID == null) {
                // check if the name matches an existing ID, if so, just return the ID
                for (Map.Entry<Object, Object> entry : dbProperties.entrySet()) {
                        if(entry.getValue().toString().trim().equals(schemaName)) {
                                schemaID = schemaName;
                        }
                }
        }
        
        if(schemaID == null && schemaName.equals("default")) {
            // delegate to the superclass
            schemaID = super.getSchemaID(schemaName);
        }
        
        return schemaID;
    }
    
    public void init(String dbname, String srwHome, String dbHome,
      String dbPropertiesFileName, Properties dbProperties) throws IOException {
        log.debug("entering init, dbname="+dbname);
        super.initDB(dbname, srwHome, dbHome, dbPropertiesFileName, dbProperties);
        this.dbProperties = dbProperties;

        String cacheSizeStr = this.dbProperties.getProperty("cacheSize");
        if (cacheSizeStr == null) {
            log.debug("cache size = 16 (default)");
            this.facetCache = new SimpleCache<String, List<String>>(16);
        } else {
            log.debug("cache size = " + cacheSizeStr);
            try {
                this.facetCache = new SimpleCache<String, List<String>>(Integer.parseInt(cacheSizeStr));
            } catch (Throwable t) {
                this.facetCache = new SimpleCache<String, List<String>>(16);
                log.error("Invalid cache size!", t);
            }
        }
        
        // Set the minimum refresh interval
        String minimumRefreshIntervalStr = this.dbProperties.getProperty("minimumRefreshInterval");
        if (minimumRefreshIntervalStr == null) {
            this.minimumMsBetweenIndexRefreshes = 60000;
        } else {
            try {
                this.minimumMsBetweenIndexRefreshes = Long.parseLong(minimumRefreshIntervalStr);
            } catch (Throwable t) {
                this.minimumMsBetweenIndexRefreshes = 60000;
                log.error("Invalid \"minimumRefreshInterval\"!", t);
            }
        }
        
        
        // Set up the IndexReader(s) for searching and for
        // facet information gathering
        this.indexReaders = new ArrayList<IndexReader>();
        if (Boolean.parseBoolean(this.dbProperties.getProperty("multipleIndexMode"))) {
            /*
             * Multiple search index mode: load multiple readers from the
             * specified base directory.
             */
            String baseSearchIndexDir = this.dbProperties.getProperty("multipleIndexBaseDirectory");
            log.debug("base search index directory = " + baseSearchIndexDir);
            for (File file : new File(baseSearchIndexDir).listFiles()) {
                try {
                    Directory dir = new NIOFSDirectory(file);
                    this.indexReaders.add(IndexReader.open(dir, true));
                    log.info("Opened index " + file + " for searching.");
                } catch (Throwable t) {
                    log.error("Failed to open \"" + file.getAbsolutePath() + "\" as a lucene index!", t);
                }
            }
            if (this.indexReaders.isEmpty()) {
                log.error("There are no indexes to search!");
            }
        } else {
            /*
             * Single search index mode: load a single IndexReader
             * against the specified dbHome.
             */
            Directory dir = new NIOFSDirectory(new File(dbHome));
            log.debug("search index directory = " + dbHome);
            this.indexReaders.add(IndexReader.open(dir, true));
        }
        
        // set up the query Analyzer
        try {
            this.analyzer = (Analyzer) Class.forName(dbProperties.getProperty("queryAnalyzerClass")).newInstance();
        } catch (Throwable t) {
            log.error("Specified analyzer, " + dbProperties.getProperty("queryAnalyzerClass") + ", could not be initialized; using StandardAnalyzer.", t);
            this.analyzer = new StandardAnalyzer();
        }
        log.debug("Analyzer: " + this.analyzer.getClass().getName());
        
        // set up the QueryParser
        luceneParser = new CqlContextSetQueryParser("cql.serverChoice", this.analyzer);

        // Set up the field configuration
        this.fieldConfig = new DefaultFieldConfiguration(this.dbProperties, this.indexReaders);
        
        log.debug("leaving init");
    }
    
    

    /*
    private void logQueryTree(CQLNode node) {
        if(node instanceof CQLBooleanNode) {
            CQLBooleanNode cbn=(CQLBooleanNode)node;
            logQueryTree(cbn.left);
            if(node instanceof CQLAndNode)
                log.debug(" AND ");
            else if(node instanceof CQLNotNode)
                log.debug(" NOT ");
            else if(node instanceof CQLOrNode)
                log.debug(" OR ");
            else log.debug(" UnknownBoolean("+cbn+") ");
            logQueryTree(cbn.right);
        }
        else if(node instanceof CQLTermNode) {
            CQLTermNode ctn=(CQLTermNode)node;
            log.debug("term(qualifier=\""+ctn.getQualifier()+"\" relation=\""+
                ctn.getRelation().getBase()+"\" term=\""+ctn.getTerm()+"\")");
        }
        else log.debug("UnknownCQLNode("+node+")");
    }
    */
    
    /**
     * Implements SCAN.  This functionality works only in the cases
     * where a single index is being scanned and there's a single
     * reader.  In other cases and diagnostic is returned.  The
     * current implementation is case insensitive.
     */
    public ScanResponseType doRequest(ScanRequestType request) throws ServletException {
        log.debug("entering doScanRequest");
        
        ScanResponseType response=new ScanResponseType();

        try {
            int maxTerms = 10;
            int position = 0;
            PositiveInteger pi = request.getMaximumTerms();
            if (pi != null) {
                maxTerms = pi.intValue();
            }
            NonNegativeInteger nni = request.getResponsePosition();
            if (nni != null) {
                position = nni.intValue();
            }

            String scanTerm = request.getScanClause();
            log.debug("Scan Clause: " + scanTerm);

            CQLNode root = parser.parse(scanTerm);
            CQLTermNode ctn = (CQLTermNode) root;
            String rawIndex = ctn.getQualifier();
            
            String value = ctn.getTerm();
            
            String[] baseIndices = this.fieldConfig.resolveFieldName(rawIndex);
            
            log.debug("Scan indices: ");
            for (String index : baseIndices) {
                log.debug("  " + index);
            }
            
            if (baseIndices.length == 1) {
                
                String fieldName = this.fieldConfig.getFieldNameFacet(baseIndices[0]);
                
                // get terms
                ArrayList<String> termValues = new ArrayList<String>(maxTerms);
                String[] lowerValues = null;
                if (position > 0) {
                    lowerValues = new String[position];
                }
                boolean startsAtBeginning = false;
                boolean endsAtEnd = false;
                int index = 0;
                if (this.indexReaders.size() != 1) {
                    // return a diagnostic indicating that scan doesn't work 
                    // for the specified index
                    return diagnostic(6, rawIndex, response);
                } else {
                    TermEnum termEnum = this.indexReaders.get(0).terms(new Term(fieldName, ""));
                    do {
                        Term term = termEnum.term();
                        if (lowerValues != null) {
                            lowerValues[(index ++)%lowerValues.length] = term.text();
                        }
                        if (term.text().toLowerCase().compareTo(value.toLowerCase()) >= 0) {
                            if (lowerValues != null) {
                                // we've reached the term.. build the list
                                if (index - 1 < lowerValues.length) {
                                    startsAtBeginning = true;
                                    for (int i = 0; i < index; i ++) {
                                        termValues.add(lowerValues[i]);
                                    }
                                } else {
                                    for (int i = 0; i < lowerValues.length; i ++) {
                                        termValues.add(lowerValues[(i + index) % lowerValues.length]);
                                    }
                                }
                            } else {
                                for (int i = position; i < 0; i ++) {
                                    if (!nextTerm(termEnum, fieldName)) {
                                        endsAtEnd = true;
                                        break;
                                    }
                                }
                            }
                            for (int i = termValues.size(); i < maxTerms; i ++) {
                                if (nextTerm(termEnum, fieldName)) {
                                    termValues.add(termEnum.term().text());
                                } else {
                                    endsAtEnd = true;
                                    break;
                                }
                            }
                            break;
                            
                        }
                    } while (nextTerm(termEnum, fieldName));
                }
                
                TermsType terms = new TermsType();
                TermType  term[] = new TermType[termValues.size()];
                log.debug(termValues.size() + " terms found");
                for (int i = 0; i < termValues.size(); i ++) {
                    term[i] = new TermType();
                    if (startsAtBeginning && i == 0) {
                        term[i].setWhereInList(TermTypeWhereInList.first);
                    }
                    term[i].setValue(termValues.get(i));
                    log.debug(termValues.get(i));
                    term[i].setNumberOfRecords(new NonNegativeInteger(String.valueOf(this.indexReaders.get(0).docFreq(new Term(this.fieldConfig.getFieldNameFacet(baseIndices[0]), termValues.get(i))))));
                }
                if (endsAtEnd) {
                    term[term.length - 1].setWhereInList(TermTypeWhereInList.last);
                }
                terms.setTerm(term);
                response.setTerms(terms);
                return response;
            } else {
                // return a diagnostic indicating that scan doesn't work 
                // for the specified index
                return diagnostic(6, rawIndex, response);
            }
        } catch(CQLParseException e) {
            log.error(e, e);
            return diagnostic(SRWDiagnostic.QuerySyntaxError,
                        e.getMessage(), response);
        } catch(IOException e) {
            log.error(e, e);
            return diagnostic(SRWDiagnostic.QuerySyntaxError,
                        e.getMessage(), response);
        }
    }
    
    private static boolean nextTerm(TermEnum termEnum, String fieldName) throws IOException {
        boolean value = termEnum.next();
        if (!value) {
            return false;
        }
        if (termEnum.term().field().equals(fieldName)) {
            return true;
        }
        return false;
    }
    
    /**
     * A helper method to determine if a particular relation type is
     * supported on a particular field.
     */
    private static boolean supportsRelation(String relation, String field, Properties configuration) {
        if (relation.equals("=") || relation.equals("scr") || relation.equals("all") || relation.equals("any")) {
            return true;
        } else if (relation.equals("exact")) {
            return configuration.containsKey("exactSuffix"); 
        } else {
            return false;
        }
        
    }
    
    public boolean supportsSort() {
        return true;
    }
}
