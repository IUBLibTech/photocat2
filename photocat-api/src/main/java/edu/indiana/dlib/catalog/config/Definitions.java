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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * A class that encapsulates the data stored in the 
 * definition XML file.  The current implementation is
 * immutable, so the general use pattern would be to fetch
 * an instance of this class from another class that is
 * responsible for ensuring that a current version is 
 * returned.
 * 
 * The structure of this class and its embedded classes is
 * meant to mirror the structure of the schema file.
 */
public abstract class Definitions {
    
    /**
     * The identifier for this set of field definitions.
     */
    private String id;
    
    /**
     * The field types in the order they appear in the configuration
     * file represented by this object.
     */
    private List<String> fieldTypes;
    
    /**
     * A Map from the field type identifiers to the objects representing 
     * the field definition.
     */
    private Map<String, FieldDefinition> fieldTypeToDefMap;
 
    /**
     * The source types in the order they appear in the configuration
     * file represented by this object.
     */
    private List<String> sourceTypes;
    
    /**
     * A Map from the source type identifiers to the objects representing
     * the source definitions.
     */
    private Map<String, SourceDefinition> sourceTypeToDefMap;
    
    public String getId() {
        return this.id;
    }
    
    /**
     * Gets an unmodifiable list of the field types specified in the
     * underlying configuration in the order they appear in that
     * file.
     */
    public List<String> listFieldTypes() {
        return Collections.unmodifiableList(this.fieldTypes);
    }
    
    /**
     * Gets the FieldDefinition for the given field type or null
     * if the field is undefined in the underlying configuration.
     */
    public FieldDefinition getFieldDefinition(String type) {
        return this.fieldTypeToDefMap.get(type);
    }
    
    /**
     * Gets the SourceDefinition for the given source type or null
     * if the source is undefined in the underlying configuration.
     */
    public SourceDefinition getSourceDefinition(String type) {
        return this.sourceTypeToDefMap.get(type);
    }
    
    /**
     * Constructs a FieldDefinition object from a InputStream of the
     * XML representation of the definition.  This implementation
     * loads and validates the XML into a DOM before parsing
     * out the data fields.
     * @param xmlInputStream a stream to access the XML serialization
     * of FieldDefinition
     * @throws IOException if an error occurs while reading the
     * stream.
     * @throws DataFormatException if any error occurs while parsing
     * or validating the XML
     * @throws IllegalStateException if the identifier has already
     * been set for this item.  (This method may only be called
     * once and only during the constructor)
     */
    protected void parseInputStream(InputStream xmlInputStream) throws IOException, DataFormatException {
        if (this.fieldTypes != null) {
            throw new IllegalStateException("parseInputStream() may only be called once!");
        }
        Document definitionDoc = DocumentHelper.getInstance().parseAndValidateDocument(xmlInputStream);
    }
    
    protected void loadDocument(Document definitionDoc) throws DataFormatException {
        XPath xpath = XPathHelper.getInstance().getXPath();
        try {
            if (!(Boolean) xpath.evaluate("d:definitions/@id", definitionDoc, XPathConstants.BOOLEAN)) {
                throw new DataFormatException("Required id attribute missing from field definition file!");
            }
            this.id = (String) xpath.evaluate("d:definitions/@id", definitionDoc, XPathConstants.STRING);
            this.fieldTypes = new ArrayList<String>();
            this.fieldTypeToDefMap = new HashMap<String, FieldDefinition>();

            NodeList definitionNl = (NodeList) xpath.evaluate("d:definitions/d:fieldDefinition", definitionDoc, XPathConstants.NODESET);
            for (int i = 0; i < definitionNl.getLength(); i ++) {
                FieldDefinition def = new FieldDefinition(this, definitionNl.item(i));
                this.fieldTypes.add(def.getType());
                this.fieldTypeToDefMap.put(def.getType(), def);
            }
            
            this.sourceTypes = new ArrayList<String>();
            this.sourceTypeToDefMap = new HashMap<String, SourceDefinition>();
            NodeList sourceDefinitionNl = (NodeList) xpath.evaluate("d:definitions/d:sourceDefinition", definitionDoc, XPathConstants.NODESET);
            for (int i = 0; i < sourceDefinitionNl.getLength(); i ++) {
                SourceDefinition def = new SourceDefinition(sourceDefinitionNl.item(i));
                this.sourceTypes.add(def.getType());
                this.sourceTypeToDefMap.put(def.getType(), def);
            }
            
        } catch (XPathExpressionException ex) {
            // This won't happen except in the even of
            // a programming error since the xpath 
            // expression isn't built from run-time data.
            throw new AssertionError(ex);
        }
    }
    
}
