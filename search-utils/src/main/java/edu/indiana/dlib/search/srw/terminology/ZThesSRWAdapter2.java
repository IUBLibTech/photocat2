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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.indiana.dlib.search.srw.SRUResponseParsingException;
import edu.indiana.dlib.search.srw.SRUSearchResultIterator;
import edu.indiana.dlib.search.srw.terminology.ZThesTerm.TermRelation;

public class ZThesSRWAdapter2 extends ZThesSRWAdapter {
    private static Log LOG = LogFactory.getLog(ZThesSRWAdapter2.class);
	
    private XPath xpath;
    
	public ZThesSRWAdapter2() {
		super();
        this.xpath = XPathFactory.newInstance().newXPath();
	}
	
	public Iterator<ZThesTerm> getMatchingTerms(String query) {
        return getTerms("dc.title = \"" + query + "\"");		
	}
	
    private Iterator<ZThesTerm> getTerms(String query) {
        if (getSRUBaseUrl() == null) {
            throw new IllegalStateException("sruBaseUrl has not been set!");
        }
        try {
            SRUSearchResultIterator resultsIt = new SRUSearchResultIterator(query, getSRUBaseUrl(), getZthesURI(), "");
            return new TermIterator(resultsIt);
        } catch (ParserConfigurationException ex) {
            LOG.error(ex);
            return new ArrayList<ZThesTerm>().iterator();
        }
    }

    /**
     * An Iterator over the term names from an SRU 
     * search result of zthes records.
     */
    public class TermIterator implements Iterator<ZThesTerm> {

        private SRUSearchResultIterator resultIterator;
		private XPathExpression termXPath;
        
        public TermIterator(SRUSearchResultIterator results) {
            this.resultIterator = results;

            String xpathStr = "term";
            try {
                this.termXPath = xpath.compile(xpathStr);
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
        
        public ZThesTerm next() {
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
                nodelist = (NodeList) termXPath.evaluate(resultEl, XPathConstants.NODESET);
            } catch (XPathExpressionException ex) {
                LOG.error(ex);
            }
            
            // nodelist should contain only one 
            if (nodelist != null && nodelist.getLength() != 0) {
                if (nodelist.getLength() != 1) {
                	// this should not have happened
                    throw new NoSuchElementException("SRW response contains multiple records");
                }
                
                // create a term here
                return generateTermFromResponse((Element) nodelist.item(0));
            } else {
                LOG.error("No term names found in record " + (this.resultIterator.getNextRecordIndex() - 1) + "!");
                throw new NoSuchElementException();
            }
        }

        private ZThesTerm generateTermFromResponse(Element el) {
        	ZThesTerm term = new ZThesTerm();
        	
        	NodeList nl = el.getElementsByTagName("termId");
        	if (nl.getLength() > 0) {
        		String termId = getNodeValue(nl.item(0));
        		term.setTermId(termId);
        	}
        	
        	nl = el.getElementsByTagName("termName");
        	if (nl.getLength() > 0) {
        		String termName = getNodeValue(nl.item(0));
        		term.setTermName(termName);
        	}
        	
        	nl = el.getElementsByTagName("termType");
        	if (nl.getLength() > 0) {
        		String termType = getNodeValue(nl.item(0));
        		term.setTermType(termType);
        	}

        	
        	nl = el.getElementsByTagName("termNote");
        	List<String> notes = new ArrayList<String>();
        	for (int i = 0; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				String note = getNodeValue(n);
				notes.add(note);
			}
        	term.setTermNotes(notes);
        
        	List<String> usedForTermNames = new ArrayList<String>();
        	List<String> broaderTermNames = new ArrayList<String>();
        	List<String> narrowerTermNames = new ArrayList<String>();
        	List<String> useTermNames = new ArrayList<String>();
        	List<String> relatedTermNames = new ArrayList<String>();
        	List<TermRelation> termRelations = new ArrayList<TermRelation>();
        	
        	nl = el.getElementsByTagName("relation");
        	for (int i = 0; i < nl.getLength(); i++) {
				Element relationEl = (Element) nl.item(i);
				
				String relationType = null;
				NodeList relationTypeNL = relationEl.getElementsByTagName("relationType");
				if (relationTypeNL.getLength() > 0)
					relationType = getNodeValue(relationTypeNL.item(0));
				
				NodeList termNameNL = relationEl.getElementsByTagName("termName");
				String relatedTermName = null;
				if (termNameNL.getLength() > 0)
					relatedTermName = getNodeValue(termNameNL.item(0));
				
				if (relationType == null || relatedTermName == null) {
					LOG.error("Missing relation type (" + relationType + ") or related term name (" + relatedTermName + ")");
					continue;
				}
				
				termRelations.add(term.new TermRelation(relationType, relatedTermName));
				
				if ("UF".equals(relationType)) {
					usedForTermNames.add(relatedTermName);
				} else if ("BT".equals(relationType)) {
					broaderTermNames.add(relatedTermName);
				} else if ("NT".equals(relationType)) {
					narrowerTermNames.add(relatedTermName);
				} else if ("USE".equals(relationType)) {
					useTermNames.add(relatedTermName);
				} else if ("RT".equals(relationType)) {
					relatedTermNames.add(relatedTermName);
				} else {
					LOG.error("Unknown relationship (" + relationType + ")");
				}
        	}
        	
        	term.setRelatedTermNames(relatedTermNames);
        	term.setBroaderTermNames(broaderTermNames);
        	term.setNarrowerTermNames(narrowerTermNames);
        	term.setUsedForTermNames(usedForTermNames);
        	term.setUseTermNames(useTermNames);
        	
        	return term;
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }
    
    private String getNodeValue(Node node) {
        Node child = node.getFirstChild();
        if (child == null) {
            return null;
        } else {
            return child.getTextContent();
        }
    }

}
