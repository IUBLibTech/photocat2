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
package edu.indiana.dlib.catalog.config.impl.fedora;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.indiana.dlib.catalog.config.Aspect;
import edu.indiana.dlib.catalog.config.DataFormatException;
import edu.indiana.dlib.catalog.config.DataView;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.config.XPathHelper;
import edu.indiana.dlib.catalog.config.impl.DefaultAspect;
import edu.indiana.dlib.catalog.config.impl.DefaultDataView;
import edu.indiana.dlib.catalog.config.impl.DefaultItem;
import edu.indiana.dlib.catalog.config.impl.DefaultItemMetadata;

/**
 * A basic extension of the DefaultItem class that reads
 * DefaultItem from (or writes it to) a quickly parsible
 * XML format.  This format doesn't have a schema and 
 * is never stored anywhere permanent (perhaps just in a
 * lucene index).
 */
public class SerializableItem extends DefaultItem {
    
    /**
     * Performs a shallow copy of the Item.
     */
    public SerializableItem(Item item) {
        metadata = item.getMetadata();
        aspects = new ArrayList<Aspect>(item.getAspects());
        controlFields = new ArrayList<NameValuePair>(item.getControlFields());
    }
    
    public SerializableItem(InputStream xmlInputStream) throws ParserConfigurationException, DataFormatException, SAXException, IOException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder builder = builderFactory.newDocumentBuilder();
        initializeFromNode(builder.parse(xmlInputStream));
    }
    
    public SerializableItem(Node node) throws DataFormatException {
        initializeFromNode(node);
    }
    
    private void initializeFromNode(Node node) throws DataFormatException {
        try {
            XPath xpath = XPathHelper.getInstance().getXPath();
            // parse out the item-metadata
            NodeList imNl = (NodeList) xpath.evaluate("m:itemMetadata", node, XPathConstants.NODESET);
            for (int i = 0; i < imNl.getLength(); i ++) {
                Node imNode = (Node) imNl.item(i);
                if (imNode.getLocalName().equals("itemMetadata")) {
                    metadata = new DefaultItemMetadata(imNode);
                    break;
                }
            }
            if (metadata == null) {
                throw new DataFormatException("Unable to find itemMetadata element!");
            }
            
            // parse out the aspects
            NodeList aspectNl = (NodeList) xpath.evaluate("i:aspects", node, XPathConstants.NODESET);
            aspects = new ArrayList<Aspect>();
            for (int i = 0; i < aspectNl.getLength(); i ++) {
                Node aspectNode = (Node) aspectNl.item(i);
                DataView thumbnail = null;
                DataView screen = null;
                DataView large = null;
                List<DataView> dataviews = new ArrayList<DataView>();
                NodeList dvNl = (NodeList) xpath.evaluate("i:aspect/i:dataview", aspectNode, XPathConstants.NODESET);
                for (int j = 0; j < dvNl.getLength(); j ++) {
                    Node dataviewNode = (Node) dvNl.item(j);
                    String url = (String) xpath.evaluate("text()", dataviewNode, XPathConstants.STRING);
                    String mimeType = (String) xpath.evaluate("@mimetype", dataviewNode, XPathConstants.STRING);
                    String viewName = (String) xpath.evaluate("@name", dataviewNode, XPathConstants.STRING);
                    Boolean isMaster = (Boolean) xpath.evaluate("@master = 'true'", dataviewNode, XPathConstants.BOOLEAN);
                    String type = (String) xpath.evaluate("@type", dataviewNode, XPathConstants.STRING);
                    DataView view = new DefaultDataView(new URL(url), mimeType, viewName, Boolean.TRUE.equals(isMaster));
                    dataviews.add(view);
                    if (type != null && "screen".equals(type)) {
                        screen = view;
                    } else if (type != null && "thumbnail".equals(type)) {
                        thumbnail = view;
                    } else if (type != null && "large".equals(type)) {
                        large = view;
                    }
                }
                aspects.add(new DefaultAspect(((Element) xpath.evaluate("i:aspect", aspectNode, XPathConstants.NODE)).getAttribute("id"), dataviews, thumbnail, screen, large, null));
            }
            
            // parse out the control fields
            controlFields = new ArrayList<NameValuePair>();
            NodeList controlFieldNl = (NodeList) xpath.evaluate("i:controlfields/i:controlfield", node, XPathConstants.NODESET);
            for (int i = 0; i < aspectNl.getLength(); i ++) {
                Node controlFieldNode = (Node) controlFieldNl.item(i);
                controlFields.add(new NameValuePair((String) xpath.evaluate("@name", controlFieldNode, XPathConstants.STRING), (String) xpath.evaluate("text()", controlFieldNode, XPathConstants.STRING)));
            }
        } catch (XPathExpressionException ex) {
            // This won't happen except in the even of
            // a programming error since the xpath 
            // expression isn't built from run-time data.
            throw new AssertionError(ex);
        } catch (MalformedURLException ex) {
            throw new DataFormatException(ex);
        }
    }

    public Document toDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        
        Element rootEl = doc.createElementNS(XPathHelper.I_URI, "i:item");
        doc.appendChild(rootEl);
        
        // add the item metadata
        Node imNode = metadata.generateDocument().getDocumentElement();
        doc.adoptNode(imNode);
        rootEl.appendChild(imNode);
        
        // add the aspects
        if (!aspects.isEmpty()) {
            Element aspectsEl = doc.createElementNS(XPathHelper.I_URI, "i:aspects");
            for (Aspect aspect : aspects) {
                Element aspectEl = doc.createElementNS(XPathHelper.I_URI, "i:aspect");
                aspectEl.setAttribute("id", aspect.getId());
                for (DataView view : aspect.listDataViews()) {
                    Element dataviewEl = doc.createElementNS(XPathHelper.I_URI, "i:dataview");
                    dataviewEl.appendChild(doc.createTextNode(view.getURL().toString()));
                    if (view.getMimeType() != null) {
                        dataviewEl.setAttribute("mimetype", view.getMimeType());
                    }
                    if (view.getViewName() != null) {
                        dataviewEl.setAttribute("name", view.getViewName());
                    }
                    dataviewEl.setAttribute("master", String.valueOf(view.isMaster()));
                    if (view == aspect.getThumbnailView()) {
                        dataviewEl.setAttribute("type", "thumbnail");
                    } else if (view == aspect.getScreenView()) {
                        dataviewEl.setAttribute("type", "screen");
                    } else if (view == aspect.getLargeView()) {
                        dataviewEl.setAttribute("type", "large");
                    }
                    aspectEl.appendChild(dataviewEl);
                }
                aspectsEl.appendChild(aspectEl);
            }
            rootEl.appendChild(aspectsEl);
        }

        
        // add the control fields
        if (!controlFields.isEmpty()) {
            Element cfsEl = doc.createElementNS(XPathHelper.I_URI, "i:controlfields");
            for (NameValuePair controlField : controlFields) {
                Element cfEl = doc.createElementNS(XPathHelper.I_URI, "i:controlfield");
                cfEl.setAttribute("name", controlField.getName());
                cfEl.appendChild(doc.createTextNode(controlField.getValue()));
                cfsEl.appendChild(cfEl);
            }
            rootEl.appendChild(cfsEl);
        }
        return doc;
    }
    
}
