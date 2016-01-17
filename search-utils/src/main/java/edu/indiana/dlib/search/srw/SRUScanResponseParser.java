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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * <p>
 *   A helper class to issue and parse SRU scan requests.
 * </p>
 */
public class SRUScanResponseParser {

    private static Log LOG = LogFactory.getLog(SRUScanResponseParser.class);
    
    public static List<ScanTerm> getScanResponse(String baseUrl, String scanClause, int offset, int range) throws IOException {
        List<ScanTerm> terms = new ArrayList<ScanTerm>();
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(new SRWNamespaceContext());
            String termXpathStr = "//srw:scanResponse/srw:terms/srw:term";
            XPathExpression termXPath = null;
            try {
                termXPath = xpath.compile(termXpathStr);
            } catch (XPathExpressionException ex) {
                // shouldn't happen with hard-coded xpath
                LOG.error("Error building xpath expression: " + termXpathStr, ex);
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            URL url = new URL(baseUrl + "?operation=scan&scanClause=" + URLEncoder.encode(scanClause, "UTF-8") + "&responsePosition=" + offset + "&version=1.1&maximumTerms=" + range);
            LOG.info("Fetching Scan: " + url);
            Document doc = docBuilder.parse(new InputSource(url.openStream()));
            NodeList nodelist = (NodeList) termXPath.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodelist.getLength(); i ++) {
                Element termEl = (Element) nodelist.item(i);
                NodeList children = termEl.getChildNodes();
                ScanTerm term = new ScanTerm();
                for (int j = 0; j < children.getLength(); j ++) {
                    if (children.item(j) instanceof Element) {
                        Element child = (Element) children.item(j);
                        if (child.getNodeName().endsWith("value")) {
                            term.setValue(getElementValue(child));
                        } else if (child.getNodeName().endsWith("numberOfRecords")) {
                            term.setNumberOfRecords(Integer.parseInt(getElementValue(child)));
                        } else if (child.getNodeName().endsWith("whereInList")) {
                            term.setWhereInList(getElementValue(child));
                        }
                    }
                }
                terms.add(term);
            }
            
        } catch (ParserConfigurationException ex) {
            LOG.error(ex);
        } catch (UnsupportedEncodingException ex) {
            LOG.error(ex);
        } catch (SAXException ex) {
            LOG.error(ex);
        } catch (XPathExpressionException ex) {
            LOG.error(ex);
        }
        return terms;
    }
    
    private static String getElementValue(Element el) {
        if (el == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i ++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                sb.append(child.getNodeValue());
            }
        }
        return sb.toString();
    }
    
    public static class ScanTerm {
        
        private String value;
        
        private int numberOfRecords;
        
        private String whereInList;

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setNumberOfRecords(int numberOfRecords) {
            this.numberOfRecords = numberOfRecords;
        }

        public int getNumberOfRecords() {
            return numberOfRecords;
        }

        public void setWhereInList(String whereInList) {
            this.whereInList = whereInList;
        }

        public String getWhereInList() {
            return whereInList;
        }
    }
    
}
