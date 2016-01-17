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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
import org.xml.sax.SAXException;

import edu.indiana.dlib.robusta.cache.SimpleCache;
import edu.indiana.dlib.search.srw.SRUScanResponseParser;
import edu.indiana.dlib.search.srw.SRWNamespaceContext;
import edu.indiana.dlib.search.srw.SRUScanResponseParser.ScanTerm;

/**
 * <p>
 *   An interface to extract meaningful vocabulary relationship 
 *   information and term information from OCLC Research's
 *   terminology services SRU servers.
 * </p>
 */
public class OCLCResearchTSSRWAdapter implements ThesaurusAdapter {
    
    private static Log LOG = LogFactory.getLog(OCLCResearchTSSRWAdapter.class);
    
    private static final String TERM_REPLACEMENT_TOKEN = "(TERM)";
    private static final String START_INDEX_REPLACEMENT_TOKEN = "(START_INDEX)";

    private static final String AUTHORIZED_TERMS_URL_PARAMETERS = "?query=oclcts.rootHeading+exact+%22(TERM)%22&version=1.1&operation=searchRetrieve&recordSchema=http%3A%2F%2Fzthes.z3950.org%2Fxml%2F1.0%2F&maximumRecords=1&startRecord=(START_INDEX)&resultSetTTL=0&;recordPacking=xml&recordXPath=&sortKeys=";
    private static final String AUTHORIZED_TERMS_XPATH_EXPRESSION = "//srw:searchRetrieveResponse/srw:records/srw:record/srw:recordData/Zthes/term[./relation/relationType='UF' and translate(./relation/termName, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')=translate('(TERM)', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')]/termName";
    private static final String NARROWER_TERMS_URL_PARAMETERS = AUTHORIZED_TERMS_URL_PARAMETERS;
    private static final String NARROWER_TERMS_XPATH_EXPRESSION = "//srw:searchRetrieveResponse/srw:records/srw:record/srw:recordData/Zthes/term/relation[relationType='NT']/termName";
    private static final String BROADER_TERMS_URL_PARAMETERS = AUTHORIZED_TERMS_URL_PARAMETERS;
    private static final String BROADER_TERMS_XPATH_EXPRESSION = "//srw:searchRetrieveResponse/srw:records/srw:record/srw:recordData/Zthes/term/relation[relationType='BT']/termName";
    private static final String RELATED_TERMS_URL_PARAMETERS = AUTHORIZED_TERMS_URL_PARAMETERS;
    private static final String RELATED_TERMS_XPATH_EXPRESSION = "//srw:searchRetrieveResponse/srw:records/srw:record/srw:recordData/Zthes/term/relation[relationType='RT']/termName";
    private static final String UNAUTHORIZED_TERMS_URL_PARAMETERS = "?query=oclcts.alternativeTerms+=+%22(TERM)%22&amp;version=1.1&amp;operation=searchRetrieve&amp;recordSchema=http%3A%2F%2Fzthes.z3950.org%2Fxml%2F1.0%2F&amp;maximumRecords=10&amp;startRecord=(START_INDEX)&amp;resultSetTTL=0&amp;recordPacking=xml&amp;recordXPath=&amp;sortKeys=";
    private static final String UNAUTHORIZED_TERMS_XPATH_EXPRESSION = "//srw:searchRetrieveResponse/srw:records/srw:record/srw:recordData/Zthes/term/termName";
    public static final String RECORD_COUNT_XPATH = "//srw:searchRetrieveResponse/srw:numberOfRecords";
    public static final String PAGE_SIZE_XPATH = "//srw:searchRetrieveResponse/srw:echoedSearchRetrieveRequest/srw:maximumRecords";
    
    public static final String SRU_BASE_URL_PROPERTY_NAME = "sruBaseUrl";
    
    private XPath xpath;
    
    private DocumentBuilder docBuilder;
    
    private String baseUrl;
    
    /**
     * A small cache of search parameters and their resulting
     * search document.  Because the backing SRU server supplies
     * information for multiple types of queries using the same
     * search (ie, BT, NT, RT etc.) a small cache of these
     * responses can save duplicate search requests while still
     * exposing a general and useful interface.
     */
    private SimpleCache<String, Document> searchCache;
    
    public OCLCResearchTSSRWAdapter() {
        this.xpath = XPathFactory.newInstance().newXPath();
        this.xpath.setNamespaceContext(new SRWNamespaceContext());

        // A cache with a size of 2 is ideal for our use because
        // there are two distinct search queries (those for 
        // authorized terms and those for unauthorized terms)
        this.searchCache = new SimpleCache<String, Document>(2);
        
        DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
        dFactory.setNamespaceAware(true);
        try {
            this.docBuilder = dFactory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            LOG.error(ex);
            throw new RuntimeException(ex);
        }
    }
    
    public void setBaseUrl(String url) {
        this.baseUrl = url;
    }
    
    public String getBaseUrl() {
        return this.baseUrl;
    }
    
    public void setProperty(String name, String value) {
        if (name.equals(SRU_BASE_URL_PROPERTY_NAME)) {
            this.baseUrl = value;
        } else {
            throw new IllegalArgumentException("Unknown property name: " + name);
        }
    }
    
    private XPathExpression getXPathExpression(String expressionPattern, String termName) throws XPathExpressionException, UnsupportedEncodingException {
        return this.xpath.compile(expressionPattern.replace(TERM_REPLACEMENT_TOKEN, URLEncoder.encode(termName, "UTF-8")));
    }
    
    private Document requestTermInformation(String queryPattern, int startIndex, String term) throws ParserConfigurationException, SAXException, IOException {
        // 1.  Request information on the term
        if (this.baseUrl == null) {
            throw new IllegalStateException("sruBaseUrl has not been set!");
        }
        String urlStr = this.baseUrl + queryPattern.replace(TERM_REPLACEMENT_TOKEN, URLEncoder.encode(term, "UTF-8")).replace(START_INDEX_REPLACEMENT_TOKEN, String.valueOf(startIndex));
        LOG.debug("Building URL: " + urlStr);
        try {
            Document cachedDoc = this.searchCache.getItem(urlStr);
            if (cachedDoc != null) {
                LOG.debug("Returned cached search result.");
                return cachedDoc;
            }
        } catch (Exception ex) {
            // won't happen because we haven't attached listeners
            // even if it does happen, it's not important, we'll
            // just reperform the search
            LOG.warn("Unable to fetch items from cache!", ex);
        }
        URL url = new URL(urlStr);
        LOG.info("Querying the thesaurus for \"" + term + "\": " + url);
        Document doc = this.docBuilder.parse(url.toString());
        try {
            this.searchCache.cacheObject(urlStr, doc);
        } catch (Exception ex) {
            // Oh well, we can't cache it... log it and fall through.
            LOG.warn("Unable to cache search results!", ex);
        }
        return doc;
    }
    
    private int getRecordCount(Document doc) {
        try {
            return Integer.parseInt((String) this.getXPathExpression(RECORD_COUNT_XPATH, "").evaluate(doc, XPathConstants.STRING));
        } catch (NumberFormatException ex) {
            LOG.debug("No record count specified.");
            return -1;
        } catch (XPathExpressionException ex) {
            LOG.error(ex);
            ex.printStackTrace();
            return -1;
        } catch (UnsupportedEncodingException ex) {
            LOG.error(ex);
            ex.printStackTrace();
            return -1;
        }
    }
    
    private int getPageSize(Document doc) {
        try {
            return Integer.parseInt((String) this.getXPathExpression(PAGE_SIZE_XPATH, "").evaluate(doc, XPathConstants.STRING));
        } catch (NumberFormatException ex) {
            LOG.debug("No page size identified!");
            return -1;
        } catch (XPathExpressionException ex) {
            LOG.error(ex);
            ex.printStackTrace();
            return -1;
        } catch (UnsupportedEncodingException ex) {
            LOG.error(ex);
            ex.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Queries the vocabulary service and parses the response to extract
     * all narrower terms.  This method returns an empty list in the 
     * event that the term is unrecognized or no narrower terms exist.
     */
    public Iterator<String> getNarrowerTermNames(String term) throws IOException {
        List<String> narrowerTerms = new ArrayList<String>();
        try {
            // 1.  Query the vocabulary service.
            Document sruDocument = this.requestTermInformation(NARROWER_TERMS_URL_PARAMETERS, 1, term);
            
            // 2.  Extract narrower terms 
            switch (getRecordCount(sruDocument)) {
                case 0:
                    LOG.debug("\"" + term + "\" was not found in the thesaurus!");
                    return narrowerTerms.iterator();
                case 1:
                    LOG.debug("\"" + term + "\" was found in the thesaurus!");
                    NodeList nodelist = (NodeList) this.getXPathExpression(NARROWER_TERMS_XPATH_EXPRESSION, term).evaluate(sruDocument, XPathConstants.NODESET);
                    if (nodelist != null) {
                        for (int i = 0; i < nodelist.getLength(); i ++) {
                            Element el = (Element) nodelist.item(i);
                            narrowerTerms.add(el.getFirstChild().getNodeValue());
                        }
                    }
                    return narrowerTerms.iterator();
                default:
                    LOG.warn("Multiple entries were found in the thesaurus for \"" + term + "\"!");
                    return narrowerTerms.iterator();
            }
            
        } catch (ParserConfigurationException ex) {
            LOG.error(ex);
            return narrowerTerms.iterator();
        } catch (SAXException ex) {
            LOG.error(ex);
            return narrowerTerms.iterator();
        } catch (XPathExpressionException ex) {
            // shouldn't happen because of hard-coded xpath
            LOG.error(ex);
            return narrowerTerms.iterator();
        }
    }
    
    public Iterator<String> getNarrowerTermNamesRecursive(String term) throws IOException {
        List<String> ntr = new ArrayList<String>();
        Iterator<String> ntIt = this.getNarrowerTermNames(term);
        while (ntIt.hasNext()) {
            String next = ntIt.next();
            ntr.add(next);
            Iterator<String> nt2It = this.getNarrowerTermNames(next);
            while (nt2It.hasNext()) {
                ntr.add(nt2It.next());
            }
        }
        return ntr.iterator();
    }
    
    /**
     * Queries the vocabulary service and parses the response to extract
     * all authorized terms.  This method returns an empty list in the
     * event that the term is unrecognized.
     */
    public Iterator<String> getUseTermNames(String term) throws IOException {
        List<String> authorizedTerms = new ArrayList<String>();
        try {
            // 1.  Query the vocabulary service.
            Document sruDocument = this.requestTermInformation(UNAUTHORIZED_TERMS_URL_PARAMETERS, 1, term);
            int currentIndex = 1;
            do {
                int pageSize = getPageSize(sruDocument);
                int count = getRecordCount(sruDocument);
                if (count < 1) {
                    break;
                }
                // 2.  Extract all authorized terms
                NodeList nodelist = (NodeList) this.getXPathExpression(UNAUTHORIZED_TERMS_XPATH_EXPRESSION, term).evaluate(sruDocument, XPathConstants.NODESET);
                if (nodelist != null) {
                    for (int i = 0; i < nodelist.getLength(); i ++) {
                        Element el = (Element) nodelist.item(i);
                        authorizedTerms.add(el.getFirstChild().getNodeValue());
                    }
                }
                currentIndex += pageSize;
                if (count >= currentIndex) {
                    // fetch the next page
                    sruDocument = this.requestTermInformation(UNAUTHORIZED_TERMS_URL_PARAMETERS, currentIndex + 1, term);
                } else {
                    sruDocument = null;
                }
            } while (sruDocument != null);             
            
            return authorizedTerms.iterator();
        } catch (ParserConfigurationException ex) {
            LOG.error(ex);
            return authorizedTerms.iterator();
        } catch (SAXException ex) {
            LOG.error(ex);
            return authorizedTerms.iterator();
        } catch (XPathExpressionException ex) {
            // shouldn't happen because of hard-coded xpath
            LOG.error(ex);
            return authorizedTerms.iterator();
        }
    }
    
    public Iterator<String> getUseForTermNames(String term) throws IOException {
        List<String> unauthorizedTerms = new ArrayList<String>();
        try {
            // 1.  Query the vocabulary service.
            Document sruDocument = this.requestTermInformation(AUTHORIZED_TERMS_URL_PARAMETERS, 1, term);
            
            // 2.  Extract narrower terms 
            switch (getRecordCount(sruDocument)) {
                case 0:
                    LOG.debug("\"" + term + "\" was not found in the thesaurus!");
                    return unauthorizedTerms.iterator();
                case 1:
                    LOG.debug("\"" + term + "\" was found in the thesaurus!");
                    NodeList nodelist = (NodeList) this.getXPathExpression(AUTHORIZED_TERMS_XPATH_EXPRESSION, term).evaluate(sruDocument, XPathConstants.NODESET);
                    if (nodelist != null) {
                        for (int i = 0; i < nodelist.getLength(); i ++) {
                            Element el = (Element) nodelist.item(i);
                            unauthorizedTerms.add(el.getFirstChild().getNodeValue());
                        }
                    }
                    return unauthorizedTerms.iterator();
                default:
                    LOG.warn("Multiple entries were found in the thesaurus for \"" + term + "\"!");
                    return unauthorizedTerms.iterator();
            }
            
        } catch (ParserConfigurationException ex) {
            LOG.error(ex);
            return unauthorizedTerms.iterator();
        } catch (SAXException ex) {
            LOG.error(ex);
            return unauthorizedTerms.iterator();
        } catch (XPathExpressionException ex) {
            // shouldn't happen because of hard-coded xpath
            LOG.error(ex);
            return unauthorizedTerms.iterator();
        }

    }
    
    public Iterator<String> getBroaderTermNames(String term) throws IOException {
        List<String> terms = new ArrayList<String>();
        try {
            // 1.  Query the vocabulary service.
            Document sruDocument = this.requestTermInformation(BROADER_TERMS_URL_PARAMETERS, 1, term);
            
            // 2.  Extract broader terms 
            switch (getRecordCount(sruDocument)) {
                case 0:
                    LOG.debug("\"" + term + "\" was not found in the thesaurus!");
                    return terms.iterator();
                case 1:
                    LOG.debug("\"" + term + "\" was found in the thesaurus!");
                    NodeList nodelist = (NodeList) this.getXPathExpression(BROADER_TERMS_XPATH_EXPRESSION, term).evaluate(sruDocument, XPathConstants.NODESET);
                    if (nodelist != null) {
                        for (int i = 0; i < nodelist.getLength(); i ++) {
                            Element el = (Element) nodelist.item(i);
                            terms.add(el.getFirstChild().getNodeValue());
                        }
                    }
                    return terms.iterator();
                default:
                    LOG.warn("Multiple entries were found in the thesaurus for \"" + term + "\"!");
                    return terms.iterator();
            }
        } catch (ParserConfigurationException ex) {
            LOG.error(ex);
            return terms.iterator();
        } catch (SAXException ex) {
            LOG.error(ex);
            return terms.iterator();
        } catch (XPathExpressionException ex) {
            // shouldn't happen because of hard-coded xpath
            LOG.error(ex);
            return terms.iterator();
        }
    }
    
    public Iterator<String> getBroaderTermNamesRecursive(String term) throws IOException {
        List<String> btr = new ArrayList<String>();
        Iterator<String> btIt = this.getBroaderTermNames(term);
        while (btIt.hasNext()) {
            String next = btIt.next();
            btr.add(next);
            Iterator<String> bt2It = this.getBroaderTermNames(next);
            while (bt2It.hasNext()) {
                btr.add(bt2It.next());
            }
        }
        return btr.iterator();
    }
    
    public Iterator<String> getRelatedTermNames(String term) throws IOException {
        List<String> terms = new ArrayList<String>();
        try {
            // 1.  Query the vocabulary service.
            Document sruDocument = this.requestTermInformation(RELATED_TERMS_URL_PARAMETERS, 1, term);
            
            // 2.  Extract broader terms 
            switch (getRecordCount(sruDocument)) {
                case 0:
                    LOG.debug("\"" + term + "\" was not found in the thesaurus!");
                    return terms.iterator();
                case 1:
                    LOG.debug("\"" + term + "\" was found in the thesaurus!");
                    NodeList nodelist = (NodeList) this.getXPathExpression(RELATED_TERMS_XPATH_EXPRESSION, term).evaluate(sruDocument, XPathConstants.NODESET);
                    if (nodelist != null) {
                        for (int i = 0; i < nodelist.getLength(); i ++) {
                            Element el = (Element) nodelist.item(i);
                            terms.add(el.getFirstChild().getNodeValue());
                        }
                    }
                    return terms.iterator();
                default:
                    LOG.warn("Multiple entries were found in the thesaurus for \"" + term + "\"!");
                    return terms.iterator();
            }
        } catch (ParserConfigurationException ex) {
            LOG.error(ex);
            return terms.iterator();
        } catch (SAXException ex) {
            LOG.error(ex);
            return terms.iterator();
        } catch (XPathExpressionException ex) {
            // shouldn't happen because of hard-coded xpath
            LOG.error(ex);
            return terms.iterator();
        }
    }

    public boolean exists(String term) throws IOException {
        try {
            Document sruDocument = this.requestTermInformation(AUTHORIZED_TERMS_URL_PARAMETERS, 1, term);
            return (getRecordCount(sruDocument) >= 1);
        } catch (ParserConfigurationException ex) {
            LOG.error(ex);
            return false;
        } catch (SAXException ex) {
            LOG.error(ex);
            return false;
        }
    }
    
    public List<ScanTerm> getNearbyTerms(String term, int termsBefore, int termsAfter) {
        try {
            return SRUScanResponseParser.getScanResponse(this.baseUrl, "oclcts.rootHeading exact \"" + term + "\"", termsBefore, termsBefore + termsAfter);
        } catch (IOException ex) {
            LOG.error(ex);
            return new ArrayList<ScanTerm>();
        }
    }
    
}
