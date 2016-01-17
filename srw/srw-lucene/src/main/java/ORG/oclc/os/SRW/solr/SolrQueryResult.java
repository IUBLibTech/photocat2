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

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ORG.oclc.os.SRW.QueryResult;
import ORG.oclc.os.SRW.Record;
import ORG.oclc.os.SRW.RecordIterator;
import edu.indiana.dlib.search.facets.DefaultSearchFacet;
import edu.indiana.dlib.search.facets.DefaultSearchFacetValue;
import edu.indiana.dlib.search.facets.FacetRequestInfo;
import edu.indiana.dlib.search.facets.SearchFacet;
import edu.indiana.dlib.search.facets.SearchFacetValue;
import edu.indiana.dlib.search.indexing.FieldConfiguration;

public class SolrQueryResult extends QueryResult {

    static Log log = LogFactory.getLog(SolrQueryResult.class);
    
    private Document solrResponseDom;
    
    private XPath xpath;
    
    private String query;
    
    public SolrQueryResult(Document solrDom, String query) {
        this.solrResponseDom = solrDom;
        this.query = query;
        
        XPathFactory xpathFactory = XPathFactory.newInstance();
        this.xpath = xpathFactory.newXPath(); 
    }
    
    public String getQuery() {
        return this.query;
    }

    public List<SearchFacet> getFacets(List<FacetRequestInfo> requestedFacets, FieldConfiguration fc) {
        try {
            List<SearchFacet> facets = new ArrayList<SearchFacet>();
            if (requestedFacets != null && !requestedFacets.isEmpty()) {
                for (FacetRequestInfo requestedFacet : requestedFacets) {
                    String facetName = fc.getFieldNameFacet(requestedFacet.facetFieldName);
                    XPathExpression expr = this.xpath.compile("/response/lst[@name='facet_counts']/lst[@name='facet_fields']/lst[@name='" + escapeForXPath(facetName) + "']/*");
                    NodeList result = (NodeList) expr.evaluate(this.solrResponseDom, XPathConstants.NODESET);
                    List<SearchFacetValue> values = new ArrayList<SearchFacetValue>();
                    int max = result.getLength();
                    for (int i = 0; i < max; i ++) {
                        Element el = (Element) result.item(i);
                        int count = Integer.parseInt(el.getFirstChild().getNodeValue());
                        if (count > 0) {
                            values.add(new DefaultSearchFacetValue(el.getAttribute("name"), el.getAttribute("name"), count));
                        }
                    }
                    facets.add(new DefaultSearchFacet(requestedFacet.facetFieldName, facetName, values));
                }
            }
            return facets;
        } catch (XPathExpressionException ex) {
            // TODO: log this error
            return null;
        } catch (Throwable t) {
            // TODO: log this error
            return null;
        }
    }
    
    public long getNumberOfRecords() {
        Element n = this.solrResponseDom.getDocumentElement();
        try {
            String result = xpath.evaluate("//response[1]/result[1]/@numFound", n);
            return Long.parseLong(result);
        } catch (XPathExpressionException ex) {
            log.error("Unable to parse numFound!", ex);
        } catch (Throwable t) {
            log.error("Unable to parse numFound!", t);
        }
        return 0;
    }

    public RecordIterator newRecordIterator(long index, int numRecs, String schemaId) throws InstantiationException {
        List<Record> recList = new ArrayList<Record>(numRecs);
        for (long i = index; i <= numRecs; i ++) {
            Element n = this.solrResponseDom.getDocumentElement();
            try {
                String recordStr = xpath.evaluate("//response[1]/result[1]/doc[" + (i) + "]/str[@name='dc']", n);
                recList.add(new Record(recordStr.toString(), "info:srw/schema/1/dc-v1.1"));
            } catch (XPathExpressionException ex) {
                ex.printStackTrace();
                recList.add(new Record("<iudlAdmin></iudlAdmin>", "UNSUPPORTED_SCHEMA"));
            }
        }
        return new SimpleRecordIterator(recList.iterator());
    }
    
    /**
     * A placeholder method that escapes a string to be placed within
     * single quotes in an xpath expression.
     */
    public static String escapeForXPath(String string) {
        return string;
    }
}
