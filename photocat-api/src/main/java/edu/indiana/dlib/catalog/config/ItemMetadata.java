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
package edu.indiana.dlib.catalog.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An object representing all of the information that can be stored 
 * in an XML file conforming to the item metadata schema 
 * (info:photocat/metadata).  Other information (meta-metadata) like
 * record modification date may be tracked in an extension of this
 * class.
 */
public class ItemMetadata {

    /**
     * The global identifier for this item, typically a URI.
     */
    private String id;
    
    /**
     * The collection to which this item belongs.
     */
    private String collectionId;
    
    /**
     * A map from the field names to the data for that field.
     */
    private Map<String, FieldData> fieldDataMap;

    protected ItemMetadata() {
        // does nothing... subclass must call parseInputStream 
        // to create a valid object.
    }
    
    public ItemMetadata(String globalId, String collectionId) {
        this.id = globalId;
        this.collectionId = collectionId;
        this.fieldDataMap = new HashMap<String, FieldData>();
    }
    
    /**
     * Constructs an ItemMetadata from a InputStream of the
     * XML representation of the metadata.  This implementation
     * loads and validates the XML into a DOM before parsing
     * out the data fields.
     * @param xmlInputStream a stream to access the XML serialization
     * of ItemMetadata.
     * @throws IOException if an error occurs while reading the
     * stream.
     * @throws DataFormatException if any error occurs while parsing
     * or validating the XML
     * @throws IllegalStateException if the identifier has already
     * been set for this item.  (This method may only be called
     * once and only during the constructor)
     */
    public void parseInputStream(InputStream xmlInputStream) throws IOException, DataFormatException {
        Document metadataDoc = DocumentHelper.getInstance().parseAndValidateDocument(xmlInputStream);
        parseDOMNode(metadataDoc.getDocumentElement());
    }
    
    /**
     * A method that parses a (valid) DOM node representing the root
     * element of the DOM version of an XML serialized ItemMetadata 
     * object. 
     */
    protected void parseDOMNode(Node node) throws DataFormatException {
        if (this.id != null) {
            throw new IllegalStateException("parseInputStream() may only be called once!");
        }
        
        try {
            // Parse out the fieldIdentifier
            XPath xpath = XPathHelper.getInstance().getXPath();
            XPathExpression idExpression = xpath.compile("m:id");
            if ((Boolean) idExpression.evaluate(node, XPathConstants.BOOLEAN)) {
                this.id = (String) idExpression.evaluate(node, XPathConstants.STRING);
            } else {
                throw new DataFormatException("Required field \"id\" was not found!");
            }
            
            // Parse out all collection identifiers
            XPathExpression collectionExpression = xpath.compile("m:collectionId");
            if ((Boolean) collectionExpression.evaluate(node, XPathConstants.BOOLEAN)) {
                this.collectionId = (String) collectionExpression.evaluate(node, XPathConstants.STRING);
            } else {
                throw new DataFormatException("Required field \"collectionId\" was not found!");
            }
            
            // Parse each Field
            NodeList fieldNl = (NodeList) xpath.evaluate("m:field", node, XPathConstants.NODESET);
            this.fieldDataMap = new HashMap<String, FieldData>();
            for (int i = 0; i < fieldNl.getLength(); i ++) {
                FieldData data = new FieldData((Element) fieldNl.item(i));
                this.fieldDataMap.put(data.getFieldType(), data);
            }
            
        } catch (XPathExpressionException ex) {
            // This won't happen except in the even of
            // a programming error since the xpath 
            // expression isn't built from run-time data.
            throw new AssertionError(ex);
        }
    }
    
    public void writeOutXML(OutputStream os) throws ParserConfigurationException, IOException, TransformerException {
        DocumentHelper.writeOutDocument(os, generateDocument());
    }
    
    /**
     * Generates a Document suitable for XML serialization representing
     *  the metadata encapsulated in this class.
     */
    public Document generateDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        
        Element rootEl = doc.createElementNS(XPathHelper.M_URI, "m:itemMetadata");
        rootEl.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        Attr schemaLocation = doc.createAttributeNS(XPathHelper.XSI_URI, "xsi:schemaLocation");
        schemaLocation.setValue(XPathHelper.M_URI + " " + XPathHelper.M_XSD_LOC);
        rootEl.setAttributeNode(schemaLocation);
        doc.appendChild(rootEl);
        
        Element fieldIdentifierEl = doc.createElementNS(XPathHelper.M_URI, "m:id");
        fieldIdentifierEl.appendChild(doc.createTextNode(this.id));
        rootEl.appendChild(fieldIdentifierEl);
        
        Element collectionIdEl = doc.createElementNS(XPathHelper.M_URI, "m:collectionId");
        collectionIdEl.appendChild(doc.createTextNode(this.collectionId));
        rootEl.appendChild(collectionIdEl);
        
        List<String> fieldTypes = new ArrayList<String>(this.fieldDataMap.keySet());
        Collections.sort(fieldTypes);
        for (String fieldType : fieldTypes) {
            Element fieldEl = this.fieldDataMap.get(fieldType).toFieldEl(doc);
            if (fieldEl.hasChildNodes()) {
                rootEl.appendChild(fieldEl);
            }
        }
        
        return doc;
    }
    
    /**
     * Gets the globally unique identifier for this item.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Gets the identifier for the collection to which this
     * item belongs.
     */
    public String getCollectionId() {
        return this.collectionId;
    }
    
    /**
     * Gets the value(s) (or null if none exists) for the given field type.
     * @param fieldType a field identifying string
     * @return the FieldData value or null
     */
    public FieldData getFieldData(String fieldType) {
        return this.fieldDataMap.get(fieldType);
    }
    
    /**
     * Deletes all values for the given field.
     */
    public void deleteFieldValue(String fieldType) {
        setFieldValue(fieldType, null);
    }
    
    /**
     * Sets, modifies or removes a field's values.
     */
    public void setFieldValue(String fieldType, FieldData fieldData) {
        if (fieldData == null) {
            this.fieldDataMap.remove(fieldType);
        } else if (!fieldType.equals(fieldData.getFieldType())) {
            throw new IllegalArgumentException("The provided fieldType does not match that of the field! (\"" + fieldType + "\" vs. \"" + fieldData.getFieldType() + "\"!)");
        } else {
            this.fieldDataMap.put(fieldType, fieldData);
        }
    }
    
    /**
     * Returns all the represented field types.
     */
    public Collection<String> getRepresentedFieldTypes() {
        return this.fieldDataMap.keySet();
    }
    
}
