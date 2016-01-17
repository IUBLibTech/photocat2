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
package edu.indiana.dlib.search.srw.terminology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.indiana.dlib.search.srw.SRUResponseParsingException;
import edu.indiana.dlib.search.srw.SRUScanResponseParser;
import edu.indiana.dlib.search.srw.SRUSearchResultIterator;
import edu.indiana.dlib.search.srw.SRUScanResponseParser.ScanTerm;

/**
 * <p>
 *   A helper class to iterate through terms as related to 
 *   other terms that are provided by an underlying SRU web
 *   service that uses the SRU/W protocol and the ZThes 
 *   context set (with IUDL extensions).
 * </p>
 * <p>
 *   For full functionality, the SRU server must support the
 *   following index names and the "exact" relation: zthes.nt, 
 *   zthes.bt, zthes.rt, zthes.uf, zthes.use, iudl.ntr, iudl.btr.
 * </p>
 * <p>
 *   Before an instance of this class is fully operable the
 *   caller must provide a base URL indicating the SRW server
 *   that will be providing the search results.  This may be
 *   provided by invoking setSRUBaseUrl, or by invoking setProperty
 *   and providing the value "SRUBaseURL" as the property name. 
 * </p>
 */
public class ZThesSRWAdapter implements ThesaurusAdapter {
    
    private static Log LOG = LogFactory.getLog(ZThesSRWAdapter.class);

    public static final String SRU_BASE_URL_PROPERTY_NAME = "sruBaseUrl";
    
    private XPath xpath;
    
    private String sruBaseUrl;
    
    private String zthesURI;

    public ZThesSRWAdapter() {
        this.xpath = XPathFactory.newInstance().newXPath();
        this.sruBaseUrl = null;
        this.zthesURI = "http://zthes.z3950.org/xml/1.0/";
        
        NamespaceContext context = new NamespaceContext() {

            public String getNamespaceURI(String prefix) {
                if (prefix.equals("zthes")) {
                    return zthesURI;
                } else {
                    return null;
                }
            }

            public String getPrefix(String namespaceURI) {
                if (namespaceURI.equals(zthesURI)) {
                    return "zthes";
                } else {
                    return null;
                }
            }

            public Iterator getPrefixes(String namespaceURI) {
                return Collections.singleton(zthesURI).iterator();
            }
        };
        this.xpath.setNamespaceContext(context);
    }

    public void setSRUBaseUrl(String url) {
        this.sruBaseUrl = url;
    }
    
    public String getSRUBaseUrl() {
        return this.sruBaseUrl;
    }
    
    public void setProperty(String name, String value) {
        if (name.equals(SRU_BASE_URL_PROPERTY_NAME)) {
            this.sruBaseUrl = value;
        } else {
            throw new IllegalArgumentException("Unknown property name: " + name);
        }
    }
    
    public boolean exists(String term) {
        return this.getTermNames("dc.title exact \"" + term + "\"").hasNext();
    }
    
    public Iterator<String> getMatchingTermNames(String term) {
        return this.getTermNames("dc.title = \"" + term + "\"");
    }
    
    public Iterator<String> getNarrowerTermNames(String term) {
        return this.getTermNames("zthes.nt exact \"" + term + "\"");
    }
    
    public Iterator<String> getNarrowerTermNamesRecursive(String term) {
        return this.getTermNames("iudl.ntr exact \"" + term + "\"");
    }
    
    public Iterator<String> getBroaderTermNames(String term) {
        return this.getTermNames("zthes.bt exact \"" + term + "\"");
    }
    
    public Iterator<String> getBroaderTermNamesRecursive(String term) {
        return this.getTermNames("iudl.btr exact \"" + term + "\"");
    }

    
    public Iterator<String> getRelatedTermNames(String term) {
       return this.getTermNames("zthes.rt exact \"" + term + "\"");
    }
    
    public Iterator<String> getUseForTermNames(String term) {
        return this.getTermNames("zthes.uf exact \"" + term + "\"");
    }
    
    public Iterator<String> getUseTermNames(String term) {
        return this.getTermNames("zthes.use exact \"" + term + "\"");
    }
    
    public List<ScanTerm> getNearbyTerms(String term, int termsBefore, int termsAfter) {
        try {
            return SRUScanResponseParser.getScanResponse(this.sruBaseUrl, "dc.title = \"" + term + "\"", termsBefore, termsBefore + termsAfter);
        } catch (IOException ex) {
            LOG.error(ex);
            return new ArrayList<ScanTerm>();
        }
    }
    
    private Iterator<String> getTermNames(String query) {
        if (this.sruBaseUrl == null) {
            throw new IllegalStateException("sruBaseUrl has not been set!");
        }
        try {
            SRUSearchResultIterator resultsIt = new SRUSearchResultIterator(query, this.sruBaseUrl, this.zthesURI, null);
            return new TermNameIterator(resultsIt);
        } catch (ParserConfigurationException ex) {
            LOG.error(ex);
            return new ArrayList<String>().iterator();
        }
    }
    

    /**
     * An Iterator over the term names from an SRU 
     * search result of zthes records.
     */
    public class TermNameIterator implements Iterator<String> {

        private SRUSearchResultIterator resultIterator;
        
        private XPathExpression termNameXPath;
        
        public TermNameIterator(SRUSearchResultIterator results) {
            this.resultIterator = results;
            
            String xpathStr = "term/termName";
            try {
                this.termNameXPath = xpath.compile(xpathStr);
            } catch (XPathExpressionException ex) {
                // shouldn't happen with hard-coded xpath
                LOG.error("Error building xpath expression: " + xpathStr, ex);
            }
        }
        
        public boolean hasNext() {
            try {
                return this.resultIterator.hasNext();
            } catch (SRUResponseParsingException ex) {
                LOG.error(ex);
                return false;
            } catch (IOException ex) {
                LOG.error(ex);
                return false;
            }
        }
        
        public String next() {
            Element resultEl;
            try {
                resultEl = this.resultIterator.next();
            } catch (SRUResponseParsingException ex) {
                LOG.error(ex);
                return null;
            } catch (IOException ex) {
                LOG.error(ex);
                return null;
            }
            if (resultEl == null) {
                return null;
            }
            NodeList nodelist = null;
            try {
                nodelist = (NodeList) termNameXPath.evaluate(resultEl, XPathConstants.NODESET);
            } catch (XPathExpressionException ex) {
                LOG.error(ex);
            }
            if (nodelist != null && nodelist.getLength() != 0) {
                if (nodelist.getLength() != 1) {
                    StringBuffer names = new StringBuffer();
                    for (int i = 0; i < nodelist.getLength(); i ++) {
                        if (names.length() > 0) {
                            names.append(", ");
                        }
                        names.append(nodelist.item(i).getFirstChild().getNodeValue());
                    }
                    LOG.error(nodelist.getLength() + " term names (" + names.toString() + ") found for record " + (this.resultIterator.getNextRecordIndex() - 1) + "!");
                    return "MISSING RECORD";
                }
                return nodelist.item(0).getFirstChild().getNodeValue();
            } else {
                LOG.error("No term names found in record " + (this.resultIterator.getNextRecordIndex() - 1) + "!");
                return "MISSING VALUE";
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }


	/**
	 * @return the zthesURI
	 */
	public String getZthesURI() {
		return zthesURI;
	}

	/**
	 * @param zthesURI the zthesURI to set
	 */
	public void setZthesURI(String zthesURI) {
		this.zthesURI = zthesURI;
	}
}
