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
package ORG.oclc.os.SRW.shared;

import edu.indiana.dlib.search.facets.FacetRequestInfo;
import edu.indiana.dlib.search.facets.SearchFacet;
import edu.indiana.dlib.search.facets.SearchFacetValue;
import gov.loc.www.zing.srw.SearchRetrieveRequestType;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.axis.message.MessageElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <p>
 *   A class with convenience methods for parsing facet requests
 *   and generating facet responses.
 * </p>
 */
public class FacetExtensionHandler {
    
    public static final String SRU_FACETED_SEARCH_EXTENSION_SCHEMA_V1 = "http://www.dlib.indiana.edu/xml/sruFacetedSearch/version1.0/";

    static Log log = LogFactory.getLog(FacetExtensionHandler.class);
    
    public static List<FacetRequestInfo> parseRequest(SearchRetrieveRequestType request) {
        if (request.getExtraRequestData() == null || request.getExtraRequestData().get_any() == null) {
            return new ArrayList<FacetRequestInfo>(0);
        } else {
            String originalRequest = null;
            List<FacetRequestInfo> requestedFacets = new ArrayList<FacetRequestInfo>();
            for (MessageElement me : request.getExtraRequestData().get_any()) {
                try {
                    Document doc = me.getAsDocument();
                    if (doc.getDocumentElement().getNamespaceURI().equals(SRU_FACETED_SEARCH_EXTENSION_SCHEMA_V1) && doc.getDocumentElement().getLocalName().equals("requestFacetInformation")) {
                        originalRequest = doc.getDocumentElement().getFirstChild().getNodeValue();
                        for (String fieldString : originalRequest.split(" ")) {
                            String[] fieldAndCount = fieldString.split(",");
                            try {
                                if (fieldAndCount.length == 3) {
                                    requestedFacets.add(new FacetRequestInfo(fieldAndCount[0], Integer.parseInt(fieldAndCount[1]), Integer.parseInt(fieldAndCount[2]), originalRequest));
                                } else if (fieldAndCount.length == 2) {
                                    requestedFacets.add(new FacetRequestInfo(fieldAndCount[0], Integer.parseInt(fieldAndCount[1]), 0, originalRequest));
                                } else if (fieldAndCount.length == 1) {
                                    requestedFacets.add(new FacetRequestInfo(fieldAndCount[0], FacetRequestInfo.UNLIMITED, 0, originalRequest));
                                }
                            } catch (NumberFormatException ex) {
                                log.error(fieldAndCount[1] + ", the requested number of facet fields to return or the offset for \"" + fieldAndCount[0] + "\",  could not be parsed as an integer.", ex);
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.error("Error parsing \"requestFacetInformation\"!", ex);
                }
            }
            return requestedFacets;
        }
    }
    
    public static MessageElement createMessageElementForFacetRequest(List<FacetRequestInfo> fris) {
        QName name = new QName(SRU_FACETED_SEARCH_EXTENSION_SCHEMA_V1, "requestFacetInformation");
        MessageElement el = new MessageElement(name);
        el.setValue(FacetRequestInfo.getEncodedString(fris));
        return el;
    }
    
    public static Document getExtraResponseInfoForFacets(List<SearchFacet> facets, List<FacetRequestInfo> requestedFacets) throws DOMException, ParserConfigurationException {
        if (!facets.isEmpty()) {
            Document extraResponseDataDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation().createDocument(FacetExtensionHandler.SRU_FACETED_SEARCH_EXTENSION_SCHEMA_V1, "facetInformation", null);
            for (SearchFacet facet : facets) {
                Element categoryEl = extraResponseDataDoc.createElementNS(FacetExtensionHandler.SRU_FACETED_SEARCH_EXTENSION_SCHEMA_V1, "field");
                extraResponseDataDoc.getDocumentElement().appendChild(categoryEl);
                categoryEl.setAttributeNS(FacetExtensionHandler.SRU_FACETED_SEARCH_EXTENSION_SCHEMA_V1, "name", facet.getFacetDisplayName());
                for (SearchFacetValue value : facet.getRankedFacetValues()) {
                    Element nameEl = extraResponseDataDoc.createElementNS(FacetExtensionHandler.SRU_FACETED_SEARCH_EXTENSION_SCHEMA_V1, "value");
                    nameEl.appendChild(extraResponseDataDoc.createTextNode(value.getFacetValue()));
                    nameEl.setAttributeNS(FacetExtensionHandler.SRU_FACETED_SEARCH_EXTENSION_SCHEMA_V1, "hits", String.valueOf(value.getHitCount()));
                    categoryEl.appendChild(nameEl);
                }
            }
            Element requestInfoEl = extraResponseDataDoc.createElementNS(FacetExtensionHandler.SRU_FACETED_SEARCH_EXTENSION_SCHEMA_V1, "requestInfo");
            
            Element mirroredRequestEl = extraResponseDataDoc.createElementNS(FacetExtensionHandler.SRU_FACETED_SEARCH_EXTENSION_SCHEMA_V1, "originalRequest");
            mirroredRequestEl.appendChild(extraResponseDataDoc.createTextNode(requestedFacets.get(0).originalRequest));
            requestInfoEl.appendChild(mirroredRequestEl);
            
            Element processedRequestEl = extraResponseDataDoc.createElementNS(FacetExtensionHandler.SRU_FACETED_SEARCH_EXTENSION_SCHEMA_V1, "resolvedRequest");
            for (FacetRequestInfo fri : requestedFacets) {
                Element facetEl = extraResponseDataDoc.createElementNS(FacetExtensionHandler.SRU_FACETED_SEARCH_EXTENSION_SCHEMA_V1, "facet");
                facetEl.setAttribute("name", fri.facetFieldName);
                facetEl.setAttribute("maxValues", String.valueOf(fri.facetRequestCount));
                facetEl.setAttribute("offset", String.valueOf(fri.facetOffset));
                processedRequestEl.appendChild(facetEl);
            }
            requestInfoEl.appendChild(processedRequestEl);
            extraResponseDataDoc.getDocumentElement().appendChild(requestInfoEl);
            return extraResponseDataDoc;
        } else {
            log.debug("There are no facets.");
            return null;
        }
    }
    
    public static String getExtraResponseInfoForFacetsAsString(List<SearchFacet> facets, List<FacetRequestInfo> requestedFacets) throws TransformerConfigurationException, TransformerException, TransformerFactoryConfigurationError, DOMException, ParserConfigurationException {
        Document extraResponseInfoDoc = getExtraResponseInfoForFacets(facets, requestedFacets);
        if (extraResponseInfoDoc == null) {
            return null;
        } else {
            DOMSource source = new DOMSource(extraResponseInfoDoc);
            StreamResult sResult = new StreamResult(new StringWriter());
            TransformerFactory.newInstance().newTransformer().transform(source, sResult);
            return sResult.getWriter().toString();
        }
    }

}
