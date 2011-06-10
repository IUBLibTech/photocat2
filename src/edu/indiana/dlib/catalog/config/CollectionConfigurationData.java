/**
 * Copyright 2011, Trustees of Indiana University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   
 *   Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *   
 *   Neither the name of Indiana University nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */
package edu.indiana.dlib.catalog.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Encapsulates the parsed data from a field configuration XML
 * file that conforms to the schema at "info:photocat/configuration".
 * 
 * The current implementation is immutable and read-only.  Future
 * versions could allow modifictions and output and thus allow
 * alterations to the configuration.
 */
public abstract class CollectionConfigurationData {

    private CollectionMetadata collectionMetadata;
    
    private String definitionId;
    
    private List<FieldConfiguration> fieldConfigurations;
    
    private Map<String, FieldData> defaultValueMap;
    
    private List<TransformationConfiguration> transformations;
    
    /**
     * A constructor that should be invoked before parseInputStream().
     */
    public CollectionConfigurationData() {
    }
    
    /**
     * A copy constructor to create a CollectionConfigurationData that
     * includes all values from the provided 'config'.  This is a shallow
     * copy because CollectionConfigurationData objects are meant to be
     * immutable.
     */
    public CollectionConfigurationData(CollectionConfigurationData config) {
        this.collectionMetadata = config.collectionMetadata;
        this.definitionId = config.definitionId;
        this.defaultValueMap = config.defaultValueMap;
        this.fieldConfigurations = config.fieldConfigurations;
        this.transformations = config.transformations;
    }
    
    /**
     * Populates a new CollectionConfiguration from a InputStream
     * of the XML representation of the metadata.  This implementation
     * loads and validates the XML into a DOM before parsing
     * out the data fields.  This method is mean to be called exactly 
     * once during the constructor a a subclass.
     * @param xmlInputStream a stream to access the XML serialization
     * of CollectionConfiguration
     * @throws IOException if an error occurs while reading the
     * stream.
     * @throws DataFormatException if any error occurs while parsing
     * or validating the XML
     * @throws IllegalStateException if the identifier has already
     * been set for this item.  (This method may only be called
     * once and only during the constructor)
     */
    public void parseInputStream(InputStream xmlInputStream) throws IOException, DataFormatException {
        if (this.collectionMetadata != null) {
            throw new IllegalStateException("parseInputStream() may only be called once!");
        }
        Document configurationDoc = DocumentHelper.getInstance().parseAndValidateDocument(xmlInputStream);
    
        XPath xpath = XPathHelper.getInstance().getXPath();
        try {
            this.collectionMetadata = new CollectionMetadata((Element) xpath.evaluate("c:configuration/c:collection", configurationDoc, XPathConstants.NODE));
            this.fieldConfigurations = new ArrayList<FieldConfiguration>();
            this.definitionId = (String) xpath.evaluate("c:configuration/c:item/c:fields/@definitionId", configurationDoc, XPathConstants.STRING);
            NodeList fieldConfigurationsNl = (NodeList) xpath.evaluate("c:configuration/c:item/c:fields/c:field", configurationDoc, XPathConstants.NODESET);
            for (int i = 0; i < fieldConfigurationsNl.getLength(); i ++) {
                Element fieldConfigurationEl = (Element) fieldConfigurationsNl.item(i);
                this.fieldConfigurations.add(new FieldConfiguration(fieldConfigurationEl, (String) xpath.evaluate("@type", fieldConfigurationEl, XPathConstants.STRING)));
            }
            this.defaultValueMap = new HashMap<String, FieldData>();
            NodeList defaultValuesNl = (NodeList) xpath.evaluate("c:configuration/c:item/c:fields/c:defaultValues/m:field", configurationDoc, XPathConstants.NODESET);
            for (int i = 0; i < defaultValuesNl.getLength(); i ++) {
                Element fieldEl = (Element) defaultValuesNl.item(i);
                FieldData data = new FieldData(fieldEl);
                this.defaultValueMap.put(data.getFieldType(), data);
            }
            this.transformations = new ArrayList<TransformationConfiguration>();
            NodeList transformationNl = (NodeList) xpath.evaluate("c:configuration/c:item/c:transformation", configurationDoc, XPathConstants.NODESET);
            for (int i = 0; i < transformationNl.getLength(); i ++) {
                Element transformationEl = (Element) transformationNl.item(i);
                String formatName = (String) xpath.evaluate("c:formatName", transformationEl, XPathConstants.STRING);
                String xsltUrl = (String) xpath.evaluate("c:xsltUrl", transformationEl, XPathConstants.STRING);
                String fidelity = transformationEl.getAttribute("fidelity");
                this.transformations.add(new TransformationConfiguration(transformationEl.getAttribute("id"), formatName, xsltUrl, "lossless".equals(fidelity)));
            }
        } catch (XPathExpressionException ex) {
            // shouldn't happen because the xpath is not dynamically generated
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Generates a Document suitable for XML serialization representing
     *  the information encapsulated in this class.
     */
    public Document generateDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        
        Element rootEl = doc.createElementNS(XPathHelper.C_URI, "c:configuration");
        rootEl.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        Attr schemaLocation = doc.createAttributeNS(XPathHelper.XSI_URI, "xsi:schemaLocation");
        schemaLocation.setValue(XPathHelper.C_URI + " " + XPathHelper.C_XSD_LOC);
        rootEl.setAttributeNode(schemaLocation);
        doc.appendChild(rootEl);
        
        Element collectionEl = doc.createElementNS(XPathHelper.C_URI, "c:collection");
        rootEl.appendChild(collectionEl);
        Element nameEl = doc.createElementNS(XPathHelper.C_URI, "c:name");
        nameEl.appendChild(doc.createTextNode(collectionMetadata.getName()));
        collectionEl.appendChild(nameEl);
        Element idEl = doc.createElementNS(XPathHelper.C_URI, "c:id");
        idEl.appendChild(doc.createTextNode(collectionMetadata.getId()));
        collectionEl.appendChild(idEl);
        Element descriptionEl = doc.createElementNS(XPathHelper.C_URI, "c:description");
        descriptionEl.appendChild(doc.createTextNode(collectionMetadata.getDescription()));
        collectionEl.appendChild(descriptionEl);
        Element iconURLEl = doc.createElementNS(XPathHelper.C_URI, "c:iconURL");
        iconURLEl.appendChild(doc.createTextNode(collectionMetadata.getIconUrl()));
        collectionEl.appendChild(iconURLEl);
        Element bannerURLEl = doc.createElementNS(XPathHelper.C_URI, "c:bannerURL");
        bannerURLEl.appendChild(doc.createTextNode(collectionMetadata.getBannerUrl()));
        collectionEl.appendChild(bannerURLEl);
        Element createEl = doc.createElementNS(XPathHelper.C_URI, "c:allowRecordCreation");
        createEl.appendChild(doc.createTextNode(collectionMetadata.allowRecordCreation() ? "true" : "false"));
        collectionEl.appendChild(createEl);
        Element deleteEl = doc.createElementNS(XPathHelper.C_URI, "c:allowRecordDeletion");
        deleteEl.appendChild(doc.createTextNode(collectionMetadata.allowRecordDeletion() ? "true" : "false"));
        collectionEl.appendChild(deleteEl);
        
        Element itemEl = doc.createElementNS(XPathHelper.C_URI, "c:item");
        rootEl.appendChild(itemEl);
        Element fieldsEl = doc.createElementNS(XPathHelper.C_URI, "c:fields");
        fieldsEl.setAttribute("definitionId", this.getDefinitionId());
        itemEl.appendChild(fieldsEl);
        for (FieldConfiguration field : fieldConfigurations) {
            Element fieldEl = doc.createElementNS(XPathHelper.C_URI, "c:field");
            fieldEl.setAttribute("type", field.getFieldType());
            fieldsEl.appendChild(fieldEl);
            addElementNS(fieldEl, XPathHelper.C_URI, "c:displayLabel", field.getDisplayLabel());
            for (String attribute : field.listRelabeledAttributes()) {
                Element el = addElementNS(fieldEl, XPathHelper.C_URI, "c:attributeDisplayLabel", field.getAttributeDisplayLabel(attribute));
                el.setAttribute("attributeName", attribute);
            }
            for (String part : field.listRelabeledParts()) {
                Element el = addElementNS(fieldEl, XPathHelper.C_URI, "c:partDisplayLabel", field.getPartDisplayLabel(part));
                el.setAttribute("partName", part);
            }
            addElementNS(fieldEl, XPathHelper.C_URI, "c:usageNotes", field.getUsageNotes());
            addElementNS(fieldEl, XPathHelper.C_URI, "c:readOnly", field.isReadOnly() ? "true" : "false");
            Element repeatableEl = addElementNS(fieldEl, XPathHelper.C_URI, "c:repeatable", field.isRepeatable() ? "true" : "false");
            if (field.getStartingBoxes() != null) {
                repeatableEl.setAttribute("startingBoxes", String.valueOf(field.getStartingBoxes()));
            }
            addElementNS(fieldEl, XPathHelper.C_URI, "c:displayedInCatalogingBriefView", field.isDisplayedInCatalogingBriefView() ? "true" : "false");
            addElementNS(fieldEl, XPathHelper.C_URI, "c:exposedInCatalogingFullView", field.isExposedInCatalogingFullView() ? "true" : "false");
            addElementNS(fieldEl, XPathHelper.C_URI, "c:displayedInDiscoveryBriefView", field.isDisplayedInDiscoveryBriefView() ? "true" : "false");
            addElementNS(fieldEl, XPathHelper.C_URI, "c:displayedInDiscoveryFullView", field.isDisplayedInDiscoveryFullView() ? "true" : "false");
            
            for (String part : field.listDisabledAttributes()) {
                addElementNS(fieldEl, XPathHelper.C_URI, "c:disable", part).setAttribute("type", "part");
            }
            for (String attribute : field.listDisabledAttributes()) {
                addElementNS(fieldEl, XPathHelper.C_URI, "c:disable", attribute).setAttribute("type", "attribute");
            }
            
            for (VocabularySourceConfiguration sourceConfig : field.getVocabularySources()) {
                Element sourceEl = doc.createElementNS(XPathHelper.C_URI, "c:vocabularySource");
                fieldEl.appendChild(sourceEl);
                if (sourceConfig.getAuthorityBinding() != null) {
                   addElementNS(sourceEl, XPathHelper.C_URI, "c:authorityBinding", sourceConfig.getAuthorityBinding());
                }
                addElementNS(sourceEl, XPathHelper.C_URI, "c:valueBinding", sourceConfig.getValueBinding());
                if (sourceConfig.getVocabularySourceConfig() != null) {
                    Element sourceConfigEl = doc.createElementNS(XPathHelper.C_URI, "c:sourceConfig");
                    sourceEl.appendChild(sourceConfigEl);
                    for (String propertyName : sourceConfig.getVocabularySourceConfig().getPropertyNames()) {
                        addElementNS(sourceConfigEl, XPathHelper.C_URI, "c:property", sourceConfig.getVocabularySourceConfig().getProperty(propertyName)).setAttribute("property", propertyName);
                    }
                }
            }
        }
        if (!defaultValueMap.isEmpty()) {
            Element defaultValueEl = doc.createElementNS(XPathHelper.C_URI, "c:defaultValues");
            fieldsEl.appendChild(defaultValueEl);
            for (FieldData fieldData : defaultValueMap.values()) {
                defaultValueEl.appendChild(fieldData.toFieldEl(doc));
            }
        }
        
        if (getTransformationConfigurations() != null) {
            for (TransformationConfiguration tconf : getTransformationConfigurations()) {
                Element transformationEl = doc.createElementNS(XPathHelper.C_URI, "c:transformation");
                transformationEl.setAttribute("id", tconf.getId());
                transformationEl.setAttribute("fidelity", tconf.isLossy() ? "lossy" : "lossless");
                addElementNS(transformationEl, XPathHelper.C_URI, "c:formatName", tconf.getFormatName());
                addElementNS(transformationEl, XPathHelper.C_URI, "c:xsltUrl", tconf.getXsltUrl());
                itemEl.appendChild(transformationEl);
            }
        }
        
        return doc;
    }
    
    /**
     * A helper method to create and add an element with a text value
     * if and only if that text value is not null.  For long lists of
     * optional simple elements, this method is ideal.
     */
    private Element addElementNS(Element parent, String nsUrl, String qname, String textValue) {
        if (textValue != null) {
            Element el = parent.getOwnerDocument().createElementNS(nsUrl, qname);
            el.appendChild(parent.getOwnerDocument().createTextNode(textValue));
            parent.appendChild(el);
            return el;
        } else {
            return null;
        }
    }
    
    public String getId() {
        return collectionMetadata.getId();
    }
    
    public String getDefinitionId() {
        return definitionId;
    }
    
    public void setDefinitionId(String id) {
        definitionId = id;
    }
    
    public CollectionMetadata getCollectionMetadata() {
        return collectionMetadata;
    }
    
    public void setCollectionMetadata(CollectionMetadata cm) {
        collectionMetadata = cm;
    }
    
    public List<FieldConfiguration> listFieldConfigurations() {
        return Collections.unmodifiableList(this.fieldConfigurations);
    }
    
    public FieldData getDefaultValue(String fieldType) {
        return this.defaultValueMap.get(fieldType);
    }
    
    public List<TransformationConfiguration> getTransformationConfigurations() {
        return this.transformations;
    }
}
