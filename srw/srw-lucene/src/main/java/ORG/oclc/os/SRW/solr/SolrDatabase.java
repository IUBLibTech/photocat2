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
package ORG.oclc.os.SRW.solr;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.SRWDatabaseImpl;
import ORG.oclc.os.SRW.shared.CqlToLuceneQueryParser;
import ORG.oclc.os.SRW.shared.FacetExtensionHandler;
import ORG.oclc.os.SRW.shared.SortKeyReader;
import ORG.oclc.os.SRW.shared.SortKeyReader.SortKey;
import edu.indiana.dlib.search.facets.FacetRequestInfo;
import edu.indiana.dlib.search.facets.SearchFacet;
import edu.indiana.dlib.search.indexing.DefaultFieldConfiguration;
import edu.indiana.dlib.search.indexing.FieldConfiguration;
import gov.loc.www.zing.srw.ScanRequestType;
import gov.loc.www.zing.srw.ScanResponseType;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;

/**
 * A database that can back an SRU/W search that is simply a wrapper 
 * around a Solr instance.
 */
public class SolrDatabase extends SRWDatabaseImpl {

    static Log PERFORMANCE_LOG = LogFactory.getLog("performance." + SolrDatabase.class.getName());
    static Log USABILITY_LOG = LogFactory.getLog("usability." + SolrDatabase.class.getName());
    static Log LOG = LogFactory.getLog(SolrDatabase.class);

    private String solrBaseUrl;
    
    private DocumentBuilder documentBuilder;
    
    private FieldConfiguration fieldConfig;
    
    public void init(String dbname, String srwHome, String dbHome, String dbPropertiesFileName, Properties dbProperties) throws Exception {
        super.initDB(dbname, srwHome, dbHome, dbPropertiesFileName, dbProperties);
        this.solrBaseUrl = dbProperties.getProperty("solrBaseUrl");
        try {
            this.documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        }
        this.fieldConfig = new DefaultFieldConfiguration(dbProperties);
    }
    
    public String getExtraResponseData(QueryResult result, SearchRetrieveRequestType request) {
        List<FacetRequestInfo> requestedFacets = FacetExtensionHandler.parseRequest(request);
        SolrQueryResult sqr = (SolrQueryResult) result;
        try {
            List<SearchFacet> facets = sqr.getFacets(requestedFacets, this.fieldConfig); 
            return FacetExtensionHandler.getExtraResponseInfoForFacetsAsString(facets, requestedFacets);
        } catch (TransformerConfigurationException ex) {
            LOG.error(ex);
        } catch (TransformerException ex) {
            LOG.error(ex);
        } catch (TransformerFactoryConfigurationError ex) {
            LOG.error(ex);
        } catch (Throwable t) {
            LOG.error(t);
        }
        return null;
    }
    
    public String getParametersForFacetRequest(SearchRetrieveRequestType request) {
        List<FacetRequestInfo> requestedFacets = FacetExtensionHandler.parseRequest(request);
        if (requestedFacets != null && !requestedFacets.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            for (FacetRequestInfo requestedFacet : requestedFacets) {
                if (sb.length() == 0) {
                    sb.append("&facet=true&facet.mincount=1&facet.sort=false");
                }
                String name = this.fieldConfig.getFieldNameFacet(requestedFacet.facetFieldName);
                sb.append("&facet.field=" + name + "&f." + name + ".facet.limit=" + requestedFacet.facetRequestCount + "&f." + name + ".facet.offset=" + requestedFacet.facetOffset);
            }
            return sb.toString();
        } else {
            return "";
        }
    }
    
    public QueryResult getQueryResult(String query, SearchRetrieveRequestType request) throws InstantiationException {
        try {
            long start;
            long end;
            
            // 1. Convert the query
            start = System.currentTimeMillis();
            String luceneQuery = CqlToLuceneQueryParser.getLuceneQuery(query, this.fieldConfig);
            end = System.currentTimeMillis();
            PERFORMANCE_LOG.info("Query Generated in " + (end - start) + "ms.");
            
            // 2. Generate the url
            StringBuffer sortParamSB = new StringBuffer();
            List<SortKey> keys = SortKeyReader.parseSortKeys(request.getSortKeys());
            for (int i = 0; i < keys.size(); i ++) {
                String key = keys.get(i).path;
                if (key != null && key.trim().length() > 0) {
                    for (String fieldName : fieldConfig.resolveFieldName(keys.get(i).path)) {
                        if (sortParamSB.length() > 0) {
                            sortParamSB.append(", ");
                        }
                        sortParamSB.append(fieldConfig.getFieldNameSort(fieldName) + (keys.get(i).ascending ? "asc" : "desc"));
                        LOG.info("Sorting by:     " + fieldConfig.getFieldNameSort(fieldName) + (keys.get(i).ascending ? " ascending " : " descending"));
                    }
                }
            }
            // TODO: handle result sets
            // TODO: handle cql.allRecords
            String solrUrl = this.solrBaseUrl + "select?indent=on&version=2.2&q=" + URLEncoder.encode(luceneQuery, "UTF-8") + (sortParamSB.length() > 0 ? URLEncoder.encode(sortParamSB.toString(), "UTF-8") : "") + "&start=" + request.getStartRecord() + "&rows=" + request.getMaximumRecords() + this.getParametersForFacetRequest(request);
            LOG.info("Solr URL: " + solrUrl);
            // 3. GET the url
            start = System.currentTimeMillis();
            HttpURLConnection conn = (HttpURLConnection) (new URL(solrUrl)).openConnection();
            Document dom = this.documentBuilder.parse(new InputSource(new InputStreamReader(conn.getInputStream())));
            end = System.currentTimeMillis();
            PERFORMANCE_LOG.info("Solr Search performed in " + (end - start) + "ms.");
            
            // 4. Parse the response into a QueryResult
            start = System.currentTimeMillis();
            QueryResult result = new SolrQueryResult(dom, luceneQuery);
            end = System.currentTimeMillis();
            PERFORMANCE_LOG.info("Response parsed in " + (end - start) + "ms.");
            return result;
        } catch (UnsupportedEncodingException ex) {
            // Can't happen because UTF-8 is supported.
            return null;
        } catch (MalformedURLException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
            return null;
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
            return null;
        } catch (SAXException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
            return null;
        }

    }

    public void addRenderer(String schemaName, String schemaID, Properties props) throws InstantiationException {
    }
    
    public ScanResponseType doRequest(ScanRequestType request) throws ServletException {
        return diagnostic(4, "not fully implemented", new ScanResponseType());
    }

    /**
     * Generates an XML fragment containing the indexInfo element
     * for a typical explain response.  The element is built 
     */
    public String getIndexInfo() {
        Enumeration     enumer=dbProperties.propertyNames();
        Hashtable       sets=new Hashtable();
        String          index, indexSet, prop;
        StringBuffer    sb=new StringBuffer("        <indexInfo>\n");
        StringTokenizer st;
        while(enumer.hasMoreElements()) {
            prop=(String)enumer.nextElement();
            if(prop.startsWith("qualifier.")) {
                st=new StringTokenizer(prop.substring(10));
                index=st.nextToken();
                st=new StringTokenizer(index, ".");
                if(st.countTokens()==1) {
                    indexSet="local";
                    index=prop.substring(10);
                }
                else {
                    indexSet=st.nextToken();
                    index=prop.substring(10+indexSet.length()+1);
                }
                if(sets.get(indexSet)==null) {  // new set
                    sb.append("          <set identifier=\"")
                      .append(dbProperties.getProperty("indexSet."+indexSet))
                      .append("\" name=\"").append(indexSet).append("\"/>\n");
                    sets.put(indexSet, indexSet);
                }
                sb.append("          <index>\n")
                  .append("            <title>").append(indexSet).append('.').append(index).append("</title>\n")
                  .append("            <map>\n")
                  .append("              <name set=\"").append(indexSet).append("\">").append(index).append("</name>\n")
                  .append("              </map>\n")
                  .append("            </index>\n");
            }
        }
        sb.append("          </indexInfo>\n");
        return sb.toString();
    }

    public boolean supportsSort() {
        return true;
    }

}
