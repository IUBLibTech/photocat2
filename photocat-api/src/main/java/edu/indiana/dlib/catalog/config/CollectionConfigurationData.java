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
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.indiana.dlib.catalog.search.BrowseSet;
import edu.indiana.dlib.catalog.search.impl.CollectionBrowseSet;
import edu.indiana.dlib.catalog.search.impl.DateBrowseSet;
import edu.indiana.dlib.catalog.search.impl.EnumeratedBrowseSet;
import edu.indiana.dlib.catalog.search.impl.FieldPartBrowseSet;
import edu.indiana.dlib.catalog.search.structured.constraints.AndSearchConstraintGroup;
import edu.indiana.dlib.catalog.search.structured.constraints.FieldPartValueSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.OrSearchConstraintGroup;
import edu.indiana.dlib.catalog.search.structured.constraints.SerializableSearchConstraint;

/**
 * Encapsulates the parsed data from a field configuration XML
 * file that conforms to the schema at "info:ico/collection".
 * 
 * The current implementation is immutable and read-only.  Future
 * versions could allow modifications and output and thus allow
 * alterations to the configuration.
 * 
 * There are some unusual constraints for the creation of a configuration
 * these are mirrored in the XML schema when possible, but are explained
 * more fully here.
 * <ul>
 *   <li>
 *     Any number of groups of fields exist for which each group only
 *     contains fields from a single definition file.  The original model
 *     only had the possibility of fields from a single definition, and
 *     this model made the XML files backwards compatible when support
 *     for multiple definitions in the same field.
 *   </li>
 *   <li>
 *     No two fields, even if from different definitions may have the
 *     same type id.  This constraint was also applied when multiple
 *     field definitions were supported for a single collection (and
 *     therefore its items) so that the ItemMetdata record schema 
 *     would not have to be changed.
 *   </li>
 * </ul>
 */
public abstract class CollectionConfigurationData {

    private boolean isPublic;
    
    /**
     * The basic metadata about a collection.  This member variable may 
     * be updated, so is copied (rather than linked) by the copy constructor.
     */
    protected CollectionMetadata collectionMetadata;
    
    /**
     * The list of fields configured for this collection.  The list is mutable
     * but the individual items shouldn't be modified.
     */
    private List<FieldConfiguration> fieldConfigurations;
    
    /**
     * The default values for fields in this collection.
     */
    private Map<FieldConfiguration, FieldData> defaultValueMap;
    
    /**
     * Configured transformation scenarios.
     */
    private List<TransformationConfiguration> transformations;
    
    /**
     * A constructor that should be invoked before parseInputStream().
     */
    public CollectionConfigurationData() {
    }
    
    /**
     * A copy constructor to create a CollectionConfigurationData that
     * includes all values from the provided 'config'.  This is a deep
     * copy.
     */
    public CollectionConfigurationData(CollectionConfigurationData config) {
        collectionMetadata = new CollectionMetadata(config.collectionMetadata);
        defaultValueMap = config.defaultValueMap != null ? new HashMap<FieldConfiguration, FieldData>(config.defaultValueMap) : new HashMap<FieldConfiguration, FieldData>();
        fieldConfigurations = config.fieldConfigurations != null ? new ArrayList<FieldConfiguration>(config.fieldConfigurations) : new ArrayList<FieldConfiguration>();
        transformations = config.transformations != null ? new ArrayList<TransformationConfiguration>(config.transformations) : new ArrayList<TransformationConfiguration>();
        isPublic = config.isPublic;
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
    protected void parseInputStream(InputStream xmlInputStream) throws IOException, DataFormatException {
        if (this.collectionMetadata != null) {
            throw new IllegalStateException("parseInputStream() may only be called once!");
        }
        Document configurationDoc = DocumentHelper.getInstance().parseAndValidateDocument(xmlInputStream);
        loadDocument(configurationDoc);
    }
    
    protected void loadDocument(String newId, String newType, Document configurationDoc) throws DataFormatException {
        if (!(newType.equals("collection") || newType.equals("unit"))) {
            throw new IllegalArgumentException("Invalid type, \"" + newType + "\"!");
        }
        loadDocument(configurationDoc);
        collectionMetadata.id = newId;
        collectionMetadata.type = newType;
    }
    
    protected void loadDocument(Document configurationDoc) throws DataFormatException {
        XPath xpath = XPathHelper.getInstance().getXPath();
        try {
            Element collectionEl = (Element) xpath.evaluate("u:configuration/u:collection", configurationDoc, XPathConstants.NODE);
            isPublic = "true".equalsIgnoreCase(String.valueOf(collectionEl.getAttribute("isPublic")));
            collectionMetadata = new CollectionMetadata((Element) xpath.evaluate("u:configuration/u:collection", configurationDoc, XPathConstants.NODE));
            fieldConfigurations = new ArrayList<FieldConfiguration>();
            NodeList fieldsNL = (NodeList) xpath.evaluate("u:configuration/u:item/u:fields", configurationDoc, XPathConstants.NODESET);
            for (int fsi = 0; fsi < fieldsNL.getLength(); fsi ++) {
                Node fieldsNode = fieldsNL.item(fsi);
                String definitionId = (String) xpath.evaluate("@definitionId", fieldsNode, XPathConstants.STRING);
                NodeList fieldConfigurationsNl = (NodeList) xpath.evaluate("u:field", fieldsNode, XPathConstants.NODESET);
                for (int i = 0; i < fieldConfigurationsNl.getLength(); i ++) {
                    Element fieldConfigurationEl = (Element) fieldConfigurationsNl.item(i);
                    int csi = 0;
                    String cSortIndexStr = (String) xpath.evaluate("@catalogingSortIndex", fieldConfigurationEl, XPathConstants.STRING);
                    if (cSortIndexStr != null && cSortIndexStr.length() > 0) {
                        csi = Integer.parseInt(cSortIndexStr);
                    }
                    int psi = 0;
                    String pSortIndexStr = (String) xpath.evaluate("@publicationSortIndex", fieldConfigurationEl, XPathConstants.STRING);
                    if (pSortIndexStr != null && pSortIndexStr.length() > 0) {
                        psi = Integer.parseInt(pSortIndexStr);
                    }
                    fieldConfigurations.add(new FieldConfiguration(fieldConfigurationEl, (String) xpath.evaluate("@type", fieldConfigurationEl, XPathConstants.STRING), definitionId, csi, psi));
                }
                defaultValueMap = new HashMap<FieldConfiguration, FieldData>();
                NodeList defaultValuesNl = (NodeList) xpath.evaluate("u:defaultValues/m:field", fieldsNode, XPathConstants.NODESET);
                for (int i = 0; i < defaultValuesNl.getLength(); i ++) {
                    Element fieldEl = (Element) defaultValuesNl.item(i);
                    FieldData data = new FieldData(fieldEl);
                    FieldConfiguration conf = null;
                    for (FieldConfiguration c : fieldConfigurations) {
                        if (c.getDefinitionId().equals(definitionId) && c.getFieldType().equals(data.getFieldType())) {
                            conf = c;
                            break;
                        }
                    }
                    if (conf != null) {
                        defaultValueMap.put(conf, data);
                    } else {
                        throw new RuntimeException("Default value is specified for an undefined field type! (" + data.getFieldType() + ")");
                    }
                }
            }
            transformations = new ArrayList<TransformationConfiguration>();
            NodeList transformationNl = (NodeList) xpath.evaluate("u:configuration/u:item/u:transformation", configurationDoc, XPathConstants.NODESET);
            for (int i = 0; i < transformationNl.getLength(); i ++) {
                Element transformationEl = (Element) transformationNl.item(i);
                String formatName = (String) xpath.evaluate("u:formatName", transformationEl, XPathConstants.STRING);
                String xsltUrl = (String) xpath.evaluate("u:xsltUrl", transformationEl, XPathConstants.STRING);
                String fidelity = transformationEl.getAttribute("fidelity");
                this.transformations.add(new TransformationConfiguration(transformationEl.getAttribute("id"), formatName, xsltUrl, "lossy".equals(fidelity)));
            }
        } catch (XPathExpressionException ex) {
            // shouldn't happen because the xpath is not dynamically generated
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Generates a Document suitable for XML serialization representing
     *  the information encapsulated in this class.
     * @throws XPathExpressionException 
     */
    public Document generateDocument() throws ParserConfigurationException, XPathExpressionException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        
        Element rootEl = doc.createElementNS(XPathHelper.U_URI, "u:configuration");
        rootEl.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        Attr schemaLocation = doc.createAttributeNS(XPathHelper.XSI_URI, "xsi:schemaLocation");
        schemaLocation.setValue(XPathHelper.U_URI + " " + XPathHelper.U_XSD_LOC);
        rootEl.setAttributeNode(schemaLocation);
        doc.appendChild(rootEl);
        
        Element collectionEl = doc.createElementNS(XPathHelper.U_URI, "u:collection");
        collectionEl.setAttribute("isPublic", String.valueOf(isPublic()));
        rootEl.appendChild(collectionEl);
        
        addElementNS(collectionEl, XPathHelper.U_URI, "u:id", collectionMetadata.getId());
        addElementNS(collectionEl, XPathHelper.U_URI, "u:type", collectionMetadata.isUnit() ? "unit" : "collection");
        addElementNS(collectionEl, XPathHelper.U_URI, "u:fullName", collectionMetadata.getFullName());
        addElementNS(collectionEl, XPathHelper.U_URI, "u:shortName", collectionMetadata.getShortName());
        addElementNS(collectionEl, XPathHelper.U_URI, "u:description", collectionMetadata.getDescription());
        addElementNS(collectionEl, XPathHelper.U_URI, "u:iconUrl", collectionMetadata.getIconUrl());
        
        if (collectionMetadata.getHomepageTitle() != null) {
            Element homepageEl = doc.createElementNS(XPathHelper.U_URI, "u:homepage");
            addElementNS(homepageEl, XPathHelper.U_URI, "u:title", collectionMetadata.getHomepageTitle());
            addElementNS(homepageEl, XPathHelper.U_URI, "u:url", collectionMetadata.getHomepageUrl());
            collectionEl.appendChild(homepageEl);
        }

        addElementNS(collectionEl, XPathHelper.U_URI, "u:collectionManagerAddress", collectionMetadata.getCollectionManagerAddress());
        addElementNS(collectionEl, XPathHelper.U_URI, "u:termsOfUse", collectionMetadata.getTermsOfUse());
        
        if (collectionMetadata.getFacets() != null && !collectionMetadata.getFacets().isEmpty()) {
            Element facetsEl = doc.createElementNS(XPathHelper.U_URI, "u:facets");
            for (BrowseSet facet : collectionMetadata.getFacets()) {
                if (facet instanceof FieldPartBrowseSet) {
                    FieldPartBrowseSet set = (FieldPartBrowseSet) facet;
                    Element facetEl = doc.createElementNS(XPathHelper.U_URI, "u:facet");
                    
                    addElementNS(facetEl, XPathHelper.U_URI, "u:name", set.getDisplayName());
                    addElementNS(facetEl, XPathHelper.U_URI, "u:fieldType", set.getFieldType());
                    addElementNS(facetEl, XPathHelper.U_URI, "u:fieldPart", set.getPartName());
                    facetsEl.appendChild(facetEl);
                } else if (facet instanceof CollectionBrowseSet) {
                    CollectionBrowseSet set = (CollectionBrowseSet) facet;
                    Element collectionFacetEl = doc.createElementNS(XPathHelper.U_URI, "u:collectionFacet");
                    addElementNS(collectionFacetEl, XPathHelper.U_URI, "u:name", set.getDisplayName());
                    facetsEl.appendChild(collectionFacetEl);
                } else if (facet instanceof DateBrowseSet) {
                    DateBrowseSet set = (DateBrowseSet) facet;
                    Element facetEl = doc.createElementNS(XPathHelper.U_URI, "u:dateFacet");
                    addElementNS(facetEl, XPathHelper.U_URI, "u:name", set.getDisplayName());
                    addElementNS(facetEl, XPathHelper.U_URI, "u:fieldType", set.getFieldType());
                    addElementNS(facetEl, XPathHelper.U_URI, "u:fieldPart", set.getPartName());
                    addElementNS(facetEl, XPathHelper.U_URI, "u:dateFormat", set.getDateFormat());
                    facetsEl.appendChild(facetEl);
                } else if (facet instanceof EnumeratedBrowseSet) {
                    EnumeratedBrowseSet set = (EnumeratedBrowseSet) facet;
                    Element facetEl = doc.createElementNS(XPathHelper.U_URI, "u:enumeratedFacet");
                    addElementNS(facetEl, XPathHelper.U_URI, "u:name", set.getDisplayName());
                    for (EnumeratedBrowseSet.Entry entry : set.getBrowseValues()) {
                        appendEnumeratedFacetValue(facetEl, entry);
                    }
                    facetsEl.appendChild(facetEl);
                } else {
                    throw new RuntimeException("Unsupported BrowseSet type: " + facet.getClass().getName());
                }
            }
            collectionEl.appendChild(facetsEl);
        }
        
        if (collectionMetadata.getConditionForFeaturedItems() != null) {
            Element featuredEl = doc.createElementNS(XPathHelper.U_URI, "u:featured");
            appendSerializableSearchConstraint(featuredEl, collectionMetadata.getConditionForFeaturedItems());
            collectionEl.appendChild(featuredEl);
        }
        
        Element publishEl = doc.createElementNS(XPathHelper.U_URI, "u:publish");
        appendSerializableSearchConstraint(publishEl, collectionMetadata.getConditionsForPublication());
        collectionEl.appendChild(publishEl);
        
        if (collectionMetadata.blockViewsConfig != null) {
            Element blockViewsEl = doc.createElementNS(XPathHelper.U_URI, "u:blockViews");
            blockViewsEl.setAttribute("scope", collectionMetadata.blockViewsConfig.getScope());
            appendSerializableSearchConstraint(blockViewsEl, collectionMetadata.blockViewsConfig.getConstraint());
            collectionEl.appendChild(blockViewsEl);
        }
        
        if (collectionMetadata.getRecordCreationProperties() != null && collectionMetadata.getRecordCreationProperties().size() > 0) {
            Element enableRecordCreationEl = doc.createElementNS(XPathHelper.U_URI, "u:enableRecordCreation");
            Properties p = collectionMetadata.getRecordCreationProperties();
            Enumeration<Object> keys = (Enumeration<Object>) p.propertyNames();
            while (keys.hasMoreElements()) {
                String name = (String) keys.nextElement();
                addElementNS(enableRecordCreationEl, XPathHelper.U_URI, "u:property", p.getProperty(name)).setAttribute("name", name);
            }
            collectionEl.appendChild(enableRecordCreationEl);
        } 
        
        if (collectionMetadata.getImageSubmissionProperties() != null && collectionMetadata.getImageSubmissionProperties().size() > 0) {
            Element imageSubmissionEl = doc.createElementNS(XPathHelper.U_URI, "u:enableImageSubmission");
            Properties p = collectionMetadata.getImageSubmissionProperties();
            Enumeration<Object> keys = (Enumeration<Object>) p.propertyNames();
            while (keys.hasMoreElements()) {
                String name = (String) keys.nextElement();
                addElementNS(imageSubmissionEl, XPathHelper.U_URI, "u:property", p.getProperty(name)).setAttribute("name", name);
            }
            collectionEl.appendChild(imageSubmissionEl);

        }
        
        Element itemEl = doc.createElementNS(XPathHelper.U_URI, "u:item");
        rootEl.appendChild(itemEl);
        
        Element currentFieldsEl = null;

        for (FieldConfiguration field : fieldConfigurations) {
            if (currentFieldsEl == null || !field.getDefinitionId().equals(currentFieldsEl.getAttribute("definitionId"))) {
                currentFieldsEl = doc.createElementNS(XPathHelper.U_URI, "u:fields");
                currentFieldsEl.setAttribute("definitionId", field.getDefinitionId());
                itemEl.appendChild(currentFieldsEl);
            }
            Element fieldEl = doc.createElementNS(XPathHelper.U_URI, "u:field");
            fieldEl.setAttribute("type", field.getFieldType());
            if (field.getCatalogingSortIndex() != 0) {
                fieldEl.setAttribute("catalogingSortIndex", String.valueOf(field.getCatalogingSortIndex()));
            }
            if (field.getPublicSortIndex() != 0) {
                fieldEl.setAttribute("publicationSortIndex", String.valueOf(field.getPublicSortIndex()));
            }
            currentFieldsEl.appendChild(fieldEl);
            addElementNS(fieldEl, XPathHelper.U_URI, "u:displayLabel", field.getDisplayLabel());
            for (String attribute : field.listRelabeledAttributes()) {
                Element el = addElementNS(fieldEl, XPathHelper.U_URI, "u:attributeDisplayLabel", field.getAttributeDisplayLabel(attribute));
                el.setAttribute("attributeName", attribute);
            }
            for (String part : field.listRelabeledParts()) {
                Element el = addElementNS(fieldEl, XPathHelper.U_URI, "u:partDisplayLabel", field.getPartDisplayLabel(part));
                el.setAttribute("partName", part);
            }
            addElementNS(fieldEl, XPathHelper.U_URI, "u:usageNotes", field.getUsageNotes());
            if (field.isReadOnly() != null) {
                addElementNS(fieldEl, XPathHelper.U_URI, "u:readOnly", field.isReadOnly() ? "true" : "false");
            }
            if (field.isRepeatable() != null) {
                Element repeatableEl = addElementNS(fieldEl, XPathHelper.U_URI, "u:repeatable", field.isRepeatable() ? "true" : "false");
                if (field.getStartingBoxes() != null) {
                    repeatableEl.setAttribute("startingBoxes", String.valueOf(field.getStartingBoxes()));
                }
            }
            if (field.displayedInCatalogingBriefView != null) {
                addElementNS(fieldEl, XPathHelper.U_URI, "u:displayedInCatalogingBriefView", field.displayedInCatalogingBriefView ? "true" : "false");
            }
            //if (field.isExposedInCatalogingFullView()) {
            //    addElementNS(fieldEl, XPathHelper.U_URI, "u:exposedInCatalogingFullView", field.isExposedInCatalogingFullView() ? "true" : "false");
            //}
            if (field.displayedInDiscoveryBriefView != null) {
                addElementNS(fieldEl, XPathHelper.U_URI, "u:displayedInDiscoveryBriefView", field.displayedInDiscoveryBriefView ? "true" : "false");
            }
            if (field.displayedInDiscoveryFullView != null) {
                addElementNS(fieldEl, XPathHelper.U_URI, "u:displayedInDiscoveryFullView", field.displayedInDiscoveryFullView ? "true" : "false");
            }
            if (field.isPrivate != null) {
                addElementNS(fieldEl, XPathHelper.U_URI, "u:private", field.isPrivate ? "true" : "false");
            }
            
            for (String part : field.listDisabledParts()) {
                Element disableEl = doc.createElementNS(XPathHelper.U_URI, "u:disable");
                disableEl.setAttribute("name", part);
                disableEl.setAttribute("type", "part");
                fieldEl.appendChild(disableEl);
            }
            for (String attribute : field.listDisabledAttributes()) {
                Element disableEl = doc.createElementNS(XPathHelper.U_URI, "u:disable");
                disableEl.setAttribute("name", attribute);
                disableEl.setAttribute("type", "attribute");
                fieldEl.appendChild(disableEl);
            }
            
            for (VocabularySourceConfiguration sourceConfig : field.getVocabularySources()) {
                Element sourceEl = doc.createElementNS(XPathHelper.U_URI, "u:vocabularySource");
                sourceEl.setAttribute("id", sourceConfig.getId());
                sourceEl.setAttribute("type", sourceConfig.getType());
                fieldEl.appendChild(sourceEl);
                if (sourceConfig.getAuthorityBinding() != null) {
                   addElementNS(sourceEl, XPathHelper.U_URI, "u:authorityBinding", sourceConfig.getAuthorityBinding());
                }
                addElementNS(sourceEl, XPathHelper.U_URI, "u:valueBinding", sourceConfig.getValueBinding());
                if (sourceConfig.getVocabularySourceConfig() != null) {
                    Element sourceConfigEl = doc.createElementNS(XPathHelper.U_URI, "u:sourceConfig");
                    sourceEl.appendChild(sourceConfigEl);
                    for (String propertyName : sourceConfig.getVocabularySourceConfig().getPropertyNames()) {
                        addElementNS(sourceConfigEl, XPathHelper.U_URI, "u:property", sourceConfig.getVocabularySourceConfig().getProperty(propertyName)).setAttribute("name", propertyName);
                    }
                }
            }
        }
        if (!defaultValueMap.isEmpty()) {
            XPath xpath = XPathHelper.getInstance().getXPath();
            Map<String, Element> defaultValuesElMap = new HashMap<String, Element>();
            for (FieldConfiguration field : defaultValueMap.keySet()) {
                Element defaultValuesEl = defaultValuesElMap.get(field.getDefinitionId());
                if (defaultValuesEl == null) {
                    defaultValuesEl = doc.createElementNS(XPathHelper.U_URI, "u:defaultValues");
                    defaultValuesElMap.put(field.getDefinitionId(), defaultValuesEl);
                    Element fieldEl = (Element) xpath.evaluate("/u:configuration/u:item/u:fields[@definitionId='" + field.getDefinitionId() + "']/u:field[@type='" + field.getFieldType() + "']", doc, XPathConstants.NODE);
                    if (fieldEl == null) {
                        throw new IllegalStateException("A default value was specified for field " + field.getFieldType() + ", that wasn't configured for this collection!");
                    }
                    fieldEl.getParentNode().appendChild(defaultValuesEl);
                }
                defaultValuesEl.appendChild(defaultValueMap.get(field).toFieldEl(doc));
            }
        }

        
        if (getTransformationConfigurations() != null) {
            for (TransformationConfiguration tconf : getTransformationConfigurations()) {
                Element transformationEl = doc.createElementNS(XPathHelper.U_URI, "u:transformation");
                transformationEl.setAttribute("id", tconf.getId());
                transformationEl.setAttribute("fidelity", tconf.isLossy() ? "lossy" : "lossless");
                addElementNS(transformationEl, XPathHelper.U_URI, "u:formatName", tconf.getFormatName());
                addElementNS(transformationEl, XPathHelper.U_URI, "u:xsltUrl", tconf.getXsltUrl());
                itemEl.appendChild(transformationEl);
            }
        }
        return doc;
    }
    
    /**
     * Recursive method to append the conditional elements that were parsed into a 
     * SerializableSearchConstraint.
     * This is the opposite of: CollectionMetadata.parseConditionalAsSearchConstraint()
     * and need only be compatible with it.
     */
    private void appendSerializableSearchConstraint(Element parent, SerializableSearchConstraint c) {
        if (c instanceof OrSearchConstraintGroup) {
            OrSearchConstraintGroup or = (OrSearchConstraintGroup) c;
            Element orEl = parent.getOwnerDocument().createElementNS(XPathHelper.U_URI, "u:or");
            for (SerializableSearchConstraint child : or.getOredConstraints()) {
                appendSerializableSearchConstraint(orEl, child);
            }
            parent.appendChild(orEl);
        } else if (c instanceof AndSearchConstraintGroup) {
            AndSearchConstraintGroup and = (AndSearchConstraintGroup) c;
            for (SerializableSearchConstraint child : and.getAndedConstraints()) {
                appendSerializableSearchConstraint(parent, child);
            }
        } else if (c instanceof FieldPartValueSearchConstraint) {
            FieldPartValueSearchConstraint fpvsc = (FieldPartValueSearchConstraint) c;
            Element conditionEl = parent.getOwnerDocument().createElementNS(XPathHelper.U_URI, "u:condition");
            conditionEl.setAttribute("type", "FIELD_COMPARISON");
            addElementNS(conditionEl, XPathHelper.U_URI, "u:property", fpvsc.getFieldType()).setAttribute("name", "field");
            addElementNS(conditionEl, XPathHelper.U_URI, "u:property", fpvsc.getPartName()).setAttribute("name", "part");
            addElementNS(conditionEl, XPathHelper.U_URI, "u:property", fpvsc.getValue()).setAttribute("name", "value");
            parent.appendChild(conditionEl);
        } else {
            throw new RuntimeException("The search constraint \"" + c.getClass().getName() + " can't be serialized into the conditionals appropriate for the metadata schema.");
        }
    }
    
    /**
     * Recursive method to append DOM elements representing the given EnumeratedBrowseSet.Entry. 
     */
    private void appendEnumeratedFacetValue(Element parent, EnumeratedBrowseSet.Entry entry) {
        Document doc = parent.getOwnerDocument();
        Element el = doc.createElementNS(XPathHelper.U_URI, "u:facetValue");
        addElementNS(el, XPathHelper.U_URI, "u:name", entry.getDisplayName());
        addElementNS(el, XPathHelper.U_URI, "u:fieldType", entry.getFieldType());
        addElementNS(el, XPathHelper.U_URI, "u:fieldPart", entry.getPartName());
        if (entry.getValue() != null) {
            addElementNS(el, XPathHelper.U_URI, "u:fieldValue", entry.getValue());
        } else {
            for (EnumeratedBrowseSet.Entry childEntry : entry.getEntries()) {
                appendEnumeratedFacetValue(el, childEntry);
            }
        }
        parent.appendChild(el);
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
    
    public String getFullName() {
        return collectionMetadata.fullName;
    }
    
    public String getShortName() {
        return collectionMetadata.shortName;
    }
    
    public String getDescription() {
        return collectionMetadata.description;
    }
    
    public String getIconUrl() {
        return collectionMetadata.iconUrl;
    }
    
    public String getHomepageTitle() {
        return collectionMetadata.homepageTitle;
    }
    
    public String getHomepageUrl() {
        return collectionMetadata.homepageUrl;
    }
    
    public String getCollectionManagerAddress() {
        return collectionMetadata.collectionManagerAddress;
    }
    
    public String getTermsOfUse() {
        return collectionMetadata.termsOfUse;
    }
    
    public CollectionMetadata getCollectionMetadata() {
        return collectionMetadata;
    }
    
    public void setCollectionMetadata(CollectionMetadata cm) {
        collectionMetadata = cm;
    }
    
    public void setFieldConfigurations(List<FieldConfiguration> newFields) {
        fieldConfigurations = newFields;
    }
    
    /**
     * Users may update the returned list, but should only do so 
     * when they know their copy is not shared (ie, they just 
     * used the copy constructor).
     */
    public List<FieldConfiguration> getFieldConfigurations() {
        return fieldConfigurations;
    }
    
    public Collection<String> listRepresentedDefinitionsIds() {
        HashSet<String> dIds = new HashSet<String>();
        for (FieldConfiguration conf : fieldConfigurations) {
            dIds.add(conf.getDefinitionId());
        }
        return dIds;
    }
    
    public FieldData getDefaultValue(String fieldType) {
        for (FieldData defaultValue : defaultValueMap.values()) {
            if (defaultValue.getFieldType().equals(fieldType)) {
                return defaultValue;
            }
        }
        return null;
    }
    
    public void removeDefaultValue(FieldConfiguration field) {
        defaultValueMap.remove(field);
    }
    
    public List<TransformationConfiguration> getTransformationConfigurations() {
        return transformations;
    }
    
    public void setTransformationConfigurations(List<TransformationConfiguration> t) {
        transformations = t;
    }
    
    /**
     * The current implementation returns the value from the 
     * metadata record, but subclasses could override this
     * and only return a collection as public if it meets that
     * *and* some other requirements.
     * @return the value of the /u:configuration/u:collection/@isPublic 
     */
    public boolean isPublic() {
        return isPublic;
    }
    
    public void setIsPublic(boolean value) {
        isPublic = value;
    }
    
    public int hashCode() {
        return getId().hashCode();
    }
    
    public boolean equals(Object o) {
        return (o instanceof CollectionConfigurationData && ((CollectionConfigurationData) o).getId().equals(getId()));
    }
}
