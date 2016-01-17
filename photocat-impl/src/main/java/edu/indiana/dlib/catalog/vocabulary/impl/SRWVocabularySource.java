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
package edu.indiana.dlib.catalog.vocabulary.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import edu.indiana.dlib.catalog.config.SourceDefinition;
import edu.indiana.dlib.catalog.config.VocabularySourceConfiguration;
import edu.indiana.dlib.catalog.vocabulary.VocabularySource;
import edu.indiana.dlib.catalog.vocabulary.VocabularySourceInitializationException;
import edu.indiana.dlib.catalog.vocabulary.VocabularySourceManager;
import edu.indiana.dlib.catalog.vocabulary.VocabularyTerm;
import edu.indiana.dlib.catalog.vocabulary.impl.srw.SearchResultsPseudoList;

/**
 * A TermSource implementation that wraps an SRW server as the 
 * source of terms.  Because this implementation fetches term
 * information from a remote service, it may suffer from 
 * inconsistent performance.
 */
public class SRWVocabularySource implements VocabularySource {

    private Logger LOGGER = Logger.getLogger(SRWVocabularySource.class);
    
    public static final String SCOPE_NOTES_PROPERTY_NAME = "Scope Notes";
    
    /**
     * The identifier for this TermSource.
     */
    private String identifier;
    
    /**
     * The SRW base URL.
     */
    private String srwBaseUrl;
    
    /**
     * The title parsed from the explain response.
     */
    private String title;
    
    /**
     * The description parsed from the explain response.
     */
    private String description;
    
    /**
     * Set to true if it's determined at construction time 
     * that the underlying ZThes server supports relationship
     * operators.
     */
    private boolean supportsZthesRelationshipOperators;
    
    private XPath xpath;
    
    private String identifierFieldName;
    
    private String identifierFieldSetName;
    
    private String titleFieldName;
    
    private String titleFieldSetName;
    
    private String prefixSearchRelation;
    
    private String prefixSearchField;
    
    private String cqlFieldSetName;
    
    private boolean supportsFullListing;
    
    /**
     * Constructs an SRWTermSource to expose data from the
     * SRW/U server at the given URL.  This class requires
     * that the SRW/U server respond to certain zthes context 
     * set queries and support the zthes result format so 
     * this constructor makes an "explain" request against
     * the server and verifies that the requirements are
     * met and that that server is online and responsive. 
     * @param srwBaseUrl the base URL for rest and soap
     * calls to this SRW/U server
     * @throws IOException 
     * @throws VocabularySourceInitializationException 
     */
    public SRWVocabularySource(SourceDefinition def, VocabularySourceConfiguration config, VocabularySourceManager manager, String collectionId) throws IOException, VocabularySourceInitializationException {
        identifier = config.getId();
        if (config.getVocabularySourceConfig() == null) {
            throw new VocabularySourceInitializationException("Required property, \"srwBaseUrl\" was not provided for " + this.getClass().getName() + " " + def.getType() + "!");
        }
        srwBaseUrl = config.getVocabularySourceConfig().getProperty("srwBaseUrl");
        if (srwBaseUrl == null) {
            throw new VocabularySourceInitializationException("Required property, \"srwBaseUrl\" was not provided for " + this.getClass().getName() + " " + def.getType() + "!");
        }

        titleFieldName = getOptionalConfigValueDefault(config, "titleFieldName", "title");
        String titleFieldSetId = getOptionalConfigValueDefault(config, "titleFieldSetId", "info:srw/cql-context-set/1/dc-v1.1");
        
        identifierFieldName = getOptionalConfigValueDefault(config, "identifierFieldName", "identifier");
        String identifierFieldSetId = getOptionalConfigValueDefault(config, "identifierFieldSetId", "info:srw/cql-context-set/2/rec-1.1");
        
        InputStream inputStream = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            inputStream = new URL(this.srwBaseUrl + "?operation=explain&version=1.1").openStream();
            Document explainResponseDoc = factory.newDocumentBuilder().parse(inputStream);
            
            this.xpath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath();
            this.xpath.setNamespaceContext(new NamespaceContext() {

                public String getNamespaceURI(String prefix) {
                    if (prefix.equals("SRW")) {
                        return "http://www.loc.gov/zing/srw/";
                    } else if (prefix.equals("exp")) {
                        return "http://explain.z3950.org/dtd/2.0/";
                    } else {
                        return null;
                    }
                }

                public String getPrefix(String namespaceURI) {
                    if (namespaceURI.equals("http://www.loc.gov/zing/srw/")) {
                        return "SRW";
                    } else if (namespaceURI.equals("http://explain.z3950.org/dtd/2.0/")) {
                        return "exp";
                    } else {
                        return null;
                    }
                }

                public Iterator getPrefixes(String namespaceURI) {
                    if (namespaceURI.equals("http://www.loc.gov/zing/srw/")) {
                        return Arrays.asList(new String[] { "SRW" }).iterator();
                    } else if (namespaceURI.equals("http://explain.z3950.org/dtd/2.0/")) {
                        return Arrays.asList(new String[] { "exp" }).iterator();
                    } else {
                        return null;
                    }
                }});
            
            title = (String) xpath.evaluate("SRW:explainResponse/SRW:record/SRW:recordData/exp:explain/exp:databaseInfo/exp:title", explainResponseDoc, XPathConstants.STRING);
            description = (String) xpath.evaluate("SRW:explainResponse/SRW:record/SRW:recordData/exp:explain/exp:databaseInfo/exp:description", explainResponseDoc, XPathConstants.STRING);
            
            // verify the response format
            if (!(Boolean) xpath.evaluate("SRW:explainResponse/SRW:record/SRW:recordData/exp:explain/exp:schemaInfo/exp:schema[@identifier='http://zthes.z3950.org/xml/1.0/' and @retrieve='true']", explainResponseDoc, XPathConstants.BOOLEAN)) {
                throw new VocabularySourceInitializationException("The SRW service at " + this.srwBaseUrl + " does not support the zthes record format!");
            }
            
            // verify that the (optional) search for everything exists
            cqlFieldSetName = (String) xpath.evaluate("SRW:explainResponse/SRW:record/SRW:recordData/exp:explain/exp:indexInfo/exp:set[@identifier='info:srw/cql-context-set/1/cql-v1.1']/@name", explainResponseDoc, XPathConstants.STRING);
            if (!(Boolean) xpath.evaluate("SRW:explainResponse/SRW:record/SRW:recordData/exp:explain/exp:indexInfo/exp:index/exp:map/exp:name[@set='" + cqlFieldSetName + "'] = 'allRecords'", explainResponseDoc, XPathConstants.BOOLEAN)) {
                supportsFullListing = false;
            } else {
                supportsFullListing = true;
            }
            
            // verify that the title and identifier search fields exist
            titleFieldSetName = (String) xpath.evaluate("SRW:explainResponse/SRW:record/SRW:recordData/exp:explain/exp:indexInfo/exp:set[@identifier='" + titleFieldSetId + "']/@name", explainResponseDoc, XPathConstants.STRING);
            identifierFieldSetName = (String) xpath.evaluate("SRW:explainResponse/SRW:record/SRW:recordData/exp:explain/exp:indexInfo/exp:set[@identifier='" + identifierFieldSetId + "']/@name", explainResponseDoc, XPathConstants.STRING);
            LOGGER.debug("titleFieldSetName=" + titleFieldSetName);
            LOGGER.debug("identifierFieldSetName=" + identifierFieldSetName);
            if (!(Boolean) xpath.evaluate("SRW:explainResponse/SRW:record/SRW:recordData/exp:explain/exp:indexInfo/exp:index/exp:map/exp:name[@set='" + titleFieldSetName + "'] = '" + titleFieldName + "'", explainResponseDoc, XPathConstants.BOOLEAN)) {
                throw new VocabularySourceInitializationException("The SRW service at " + this.srwBaseUrl + " does not support searching on the title field!");
            }
            
            if (!(Boolean) xpath.evaluate("SRW:explainResponse/SRW:record/SRW:recordData/exp:explain/exp:indexInfo/exp:index/exp:map/exp:name[@set='" + identifierFieldSetName + "'] = '" + identifierFieldName + "'", explainResponseDoc, XPathConstants.BOOLEAN)) {
                throw new VocabularySourceInitializationException("The SRW service at " + this.srwBaseUrl + " does not support searching on the identifier field!");
            }

            prefixSearchRelation = getOptionalConfigValueDefault(config, "prefixSearchRelation", "exact");
            prefixSearchField = getOptionalConfigValueDefault(config, "prefixSearchField", titleFieldSetName + "." + titleFieldName);
            if (!(Boolean) xpath.evaluate("SRW:explainResponse/SRW:record/SRW:recordData/exp:explain/exp:indexInfo/exp:index[exp:title = '" + prefixSearchField + "' and exp:configInfo/exp:supports[@type='relation'] = '" + prefixSearchRelation + "']", explainResponseDoc, XPathConstants.BOOLEAN)) {
                throw new VocabularySourceInitializationException("The SRW service at " + this.srwBaseUrl + " does not support searching on the " + prefixSearchField + " field with the \"" + prefixSearchRelation + "\" relation!");
            }
            
            // check for zthes.nt field
            String zthesSetName = (String) xpath.evaluate("SRW:explainResponse/SRW:record/SRW:recordData/exp:explain/exp:indexInfo/exp:set[@identifier='http://zthes.z3950.org/cql/1.0.1']/@name", explainResponseDoc, XPathConstants.STRING);

            if ((Boolean) xpath.evaluate("SRW:explainResponse/SRW:record/SRW:recordData/exp:explain/exp:indexInfo/exp:index[@search='true']/exp:map/exp:name[@set='" + zthesSetName + "'] = 'nt'", explainResponseDoc, XPathConstants.BOOLEAN)) {
                this.supportsZthesRelationshipOperators = true;
            }
            
        } catch (SAXException ex) {
            throw new VocabularySourceInitializationException(ex);
        } catch (ParserConfigurationException ex) {
            throw new VocabularySourceInitializationException(ex);
        } catch (XPathExpressionException ex) {
            throw new VocabularySourceInitializationException(ex);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private String getOptionalConfigValueDefault(VocabularySourceConfiguration config, String propertyName, String defaultValue) {
        String value = config.getVocabularySourceConfig().getProperty(propertyName);
        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }
    
    /**
     * Gets the description of the term source that was parsed
     * out of the SRW explain response at construction time.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Gets the display name of the term source that was parsed
     * out of the SRW explain response at construction time. 
     */
    public String getDisplayName() {
        return this.title;
    }

    /**
     * Gets the identifier for this vocabulary, which was provided
     * at construction time.
     */
    public String getId() {
        return this.identifier;
    }

    /**
     * Returns true;
     */
    public boolean isAutocompleteEnabled() {
        return true;
    }

    /**
     * Returns true if relationship operators are implemented on the SRW
     * server.
     */
    public boolean isBrowsingEnabled() {
        return this.supportsZthesRelationshipOperators;
    }

    /**
     * Returns false, because the controlled vocabulary is accessed using
     * a READ-ONLY interface.
     */
    public boolean isManagementEnabled() {
        return false;
    }

    /**
     * Returns true.
     */
    public boolean isSearchEnabled() {
        return true;
    }

    /**
     * The current implementation does nothing.
     */
    public void notifyEnteredValue(String termId, String enteredValue) {
        return;
    }
    
    /**
     * The current implementation searches for any terms that *start* 
     * with the passed display name and returns true if the first 
     * match has the exact value.  If not, it returns false.
     * Relevance sorting is relied upon to guarantee that the exact
     * match would show up as the first result.
     */
    public boolean lookupTermByName(String displayName) {
        List<VocabularyTerm> terms = listTerms(prefixSearchField + " " + prefixSearchRelation + " \"" + displayName + "", 1, 0);
        if (terms.isEmpty()) {
        	return false;
        } else {
        	return terms.get(0).getDisplayName().equalsIgnoreCase(displayName);
        }
    }
    
    public List<VocabularyTerm> getTermsWithPrefix(String prefix, int limit, int offset) {
        return this.listTerms(prefixSearchField + " " + prefixSearchRelation + " \"" + prefix + "*\"", limit, offset);
    }
    
    public List<VocabularyTerm> listAllTerms(int limit, int offset) {
        return listTerms("cql.allRecords=\"1\"", limit, offset);
    }
    
    
    private List<VocabularyTerm> listTerms(String query, int limit, int offset) {
        List<VocabularyTerm> results = new ArrayList<VocabularyTerm>();
        try {
            SearchResultsPseudoList resultList = new SearchResultsPseudoList(query, this.srwBaseUrl, "http://zthes.z3950.org/xml/1.0/", titleFieldSetName + "." + titleFieldName + ",,1,,lowValue", 10);
            XPathExpression termNameXpathExpression = this.xpath.compile("term/termName");
            XPathExpression termIdXpathExpression = this.xpath.compile("term/termId");
            XPathExpression scopeNotesXpathExpression = this.xpath.compile("term/termNote[@label='scope note']");
            for (int i = offset; i < limit + offset && i < resultList.getSize(); i ++) {
                Map<String, List<String>> properties = null;
                Element el = resultList.get(i);
                String value = (String) termNameXpathExpression.evaluate(el, XPathConstants.STRING);
                //String id = (String) termIdXpathExpression.evaluate(el, XPathConstants.STRING);
                // Currently, we don't want to store ids, so we just use the display
                // name as the id.
                String scopeNotes = (String) scopeNotesXpathExpression.evaluate(el, XPathConstants.STRING);
                if (scopeNotes != null && scopeNotes.trim().length() > 0) {
                    properties = new HashMap<String, List<String>>();
                    properties.put(SCOPE_NOTES_PROPERTY_NAME, Collections.singletonList(scopeNotes));
                }
                if (value != null && !value.equals("")) {
                    if (properties == null) {
                        results.add(new DefaultVocabularyTerm(value, value, this.identifier, new ArrayList<String>(), new HashMap<String, List<String>>()));
                    } else {
                        results.add(new DefaultVocabularyTerm(value, value, this.identifier, new ArrayList<String>(properties.keySet()), properties));
                    }
                }
                
            }
        } catch (Throwable t) {
            LOGGER.error("Error fetching all terms!", t);
        }
        return results;
    }
    
    public int getTermCount() {
        try {
            SearchResultsPseudoList resultList = new SearchResultsPseudoList("cql.allRecords=\"1\"", this.srwBaseUrl, "http://zthes.z3950.org/xml/1.0/", titleFieldSetName + "." + titleFieldName + ",,1,,lowValue", 1);
            return resultList.getSize();
        } catch (Throwable t) {
            LOGGER.error("Error computing term count!", t);
            return 0;
        }
    }

    /**
     * Returns the "Scope Notes" property supported by the underlying thesaurus.
     */
    public List<String> getSupportedProperties() {
        return Collections.singletonList(SCOPE_NOTES_PROPERTY_NAME);
    }

    /**
     * The current implementation returns false.
     */
    public boolean allowUnlistedTerms() {
        return false;
    }

    public VocabularyTerm getTerm(String id) {
        List<VocabularyTerm> terms = listTerms(identifierFieldSetName + "." + identifierFieldName + " exact \"" + id + "\"", 1, 0);
        if (terms.size() == 1) {
            return terms.get(0);
        } else {
            return null;
        }
    }

    /**
     * Returns false.
     */
    public boolean supportsIds() {
        return false;
    }

}
