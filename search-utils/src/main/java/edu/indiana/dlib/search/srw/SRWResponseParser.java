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
package edu.indiana.dlib.search.srw;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.indiana.dlib.common.utils.DomUtils;
import edu.indiana.dlib.search.facets.DefaultSearchFacet;
import edu.indiana.dlib.search.facets.DefaultSearchFacetValue;
import edu.indiana.dlib.search.facets.SearchFacet;
import edu.indiana.dlib.search.facets.SearchFacetValue;
import edu.indiana.dlib.search.highlighter.DefaultHighlighterMatch;
import edu.indiana.dlib.search.highlighter.HighlighterMatch;

public class SRWResponseParser {

    /**
     * A very crude method that simply returns the value of 
     * the first "mods:url" node that has an attribute
     * "access" that equals "preview".  There is no guarantee
     * that the response returned is from the first record.
     */
    public static String getFirstResultPreviewURLString(Document response) {
        NodeList modsUrlNL = response.getElementsByTagName("mods:url");
        if (modsUrlNL.getLength() != 0) {
            for (int i = 0; i < modsUrlNL.getLength(); i ++) {
                Element previewEl = (Element) modsUrlNL.item(0);
                if ("preview".equals(previewEl.getAttribute("access"))) {
                    return previewEl.getFirstChild().getNodeValue();
                }
            }
        } 
        return null;
        
    }
    
    /**
     * Parses and returns all facet information from the document
     * which is expected to be the result of an SRU search.
     */
    public static List<SearchFacet> extractFacetInformation(Document response) {
        List<SearchFacet> facets = new ArrayList<SearchFacet>();
        NodeList extraResponseDataNL = response.getElementsByTagName("extraResponseData");
        if (extraResponseDataNL.getLength() != 0) {
            Element extraResponseDataEl = (Element) extraResponseDataNL.item(0);
            NodeList childrenOfExtraResponseNL = extraResponseDataEl.getChildNodes();
            for (int i = 0; i < childrenOfExtraResponseNL.getLength(); i ++) {
                Node node = childrenOfExtraResponseNL.item(i);
                if (node instanceof Element && ((Element) node).getNodeName().endsWith("facetInformation")) {
                    return extractFacetInformationFromElement((Element) node);
                }
            }
        }
        return facets;
    }
    
    public static List<SearchFacet> extractFacetInformationFromElement(Element facetInfoEl) {
        List<SearchFacet> facets = new ArrayList<SearchFacet>();
        NodeList facetChildrenNL = facetInfoEl.getChildNodes();
        for (int j = 0; j < facetChildrenNL.getLength(); j ++) {
            Node facetInfoChildNode = facetChildrenNL.item(j);
            if (facetInfoChildNode instanceof Element && ((Element) facetInfoChildNode).getNodeName().endsWith("field")) {
                Element fieldEl = (Element) facetInfoChildNode;
                List<SearchFacetValue> values = new ArrayList<SearchFacetValue>();
                NodeList fieldChildrenNL = fieldEl.getChildNodes();
                for (int k = 0; k < fieldChildrenNL.getLength(); k ++) {
                    Node fieldChildNode = fieldChildrenNL.item(k);
                    if (fieldChildNode instanceof Element && ((Element) fieldChildNode).getNodeName().endsWith("value")) {
                        Element valueEl = (Element) fieldChildNode;
                        if (valueEl != null && valueEl.getFirstChild() != null) {
                            values.add(new DefaultSearchFacetValue(fieldEl.getAttribute("name"), valueEl.getFirstChild().getNodeValue(), Integer.parseInt(valueEl.getAttribute("hits"))));
                        } else {
                            values.add(new DefaultSearchFacetValue(fieldEl.getAttribute("name"), "", Integer.parseInt(valueEl.getAttribute("hits"))));
                        }
                    }
                }
                DefaultSearchFacet facet = new DefaultSearchFacet(fieldEl.getAttribute("name"), fieldEl.getAttribute("name"), values);
                facets.add(facet);
            }
        }
        return facets;
    }
    
    public static List<HighlighterMatch> extractHighlighterInformationFromElement(Element highlighterInfoEl) {
        List<HighlighterMatch> matches = new ArrayList<HighlighterMatch>();
        NodeList highlighterChildrenNL = highlighterInfoEl.getChildNodes();
        for (int j = 0; j < highlighterChildrenNL.getLength(); j ++) {
            Node matchNode = highlighterChildrenNL.item(j);
            if (matchNode instanceof Element && ((Element) matchNode).getNodeName().endsWith("match")) {
                Element matchEl = (Element) matchNode;
                matches.add(new DefaultHighlighterMatch(matchEl.getAttribute("field"), DomUtils.getElementValue(matchEl)));
            }
        }
        return matches;
    }
    
    public static int getRecordCount(Document doc) {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        
        XPathExpression expr;
        try {
            expr = xpath.compile("//searchRetrieveResponse[1]/numberOfRecords[1]");
            Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
            return Integer.parseInt((String) expr.evaluate(doc, XPathConstants.STRING));
        } catch (XPathExpressionException ex) {
            ex.printStackTrace();
            // can't happen
            return -1;
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }
    
    public static String getXmlFromNode(Node root) throws TransformerException, IOException {
        DOMSource source = new DOMSource(root);
        StringWriter sWriter = new StringWriter();
        StreamResult sResult = new StreamResult(sWriter);
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer t = tFactory.newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.transform(source, sResult);
        sResult.getWriter().flush();
        String string = sWriter.toString();
        sWriter.close();
        return string;
    }
    
}
