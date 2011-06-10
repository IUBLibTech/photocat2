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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The configuration for a field.  This configuration typically
 * just affects how/when the field is presented, except when
 * data sources are supplied which potentially narrow its scope.
 * 
 * Any member variable may be null indicating that no value was
 * specified in the configuration file fragment represented by
 * this instance.
 */
public class FieldConfiguration {
    
    /**
     * The "type" of field.  For those types known to this class
     * default values will be provided for the remaining fields
     * which may be overridden by the setter methods.
     */
    private String fieldType;
    
    /**
     * The label for this field as it should be displayed in
     * the cataloging and discovery applications.
     */
    private String displayLabel;
    
    /**
     * A map from part names to the display label for those parts.
     */
    private Map<String, String> partLabelMap;
    
    /**
     * A map from attribute names to the display label for those
     * attributes.
     */
    private Map<String, String> attributeLabelMap;

    /**
     * Notes on how this field should be used.  These should be
     * written for the cataloger and will be displayed as a tool-tip
     * or on the instruction page for a given field.
     */
    private String usageNotes;
    
    /**
     * True if this field is only to be displayed and not edited.
     */
    private Boolean readOnly;

    /**
     * True if this field may have multiple values.
     */
    private Boolean repeatable;
    
    /**
     * The initial number of input boxes to be displayed for
     * a repeatable field.  This value is ignored if repeatable
     * is set to false (and exactly one vlistalue will be allowed).
     */
    private Integer startingBoxes;

    /**
     * True if this field should be displayed in the
     * brief view of an item in the cataloging client
     * (ie, the search results listing).
     */
    private Boolean displayedInCatalogingBriefView;
    
    /**
     * False if this item is meant to be hidden from
     * view in the cataloging view of this item.  This
     * is suitable for boilerplate values that don't 
     * need to be seen or edited by catalogers.
     */
    private Boolean exposedInCatalogingFullView;
    
    /**
     * True if this field should be included in the 
     * brief view in the discovery interface.
     */
    private Boolean displayedInDiscoveryBriefView;
    
    /**
     * True if this field should be included in the 
     * full view in the discovery application.  When
     * false, this field can be considered internal
     * only as it will only be visible to catalogers
     * and those who view the records in their raw 
     * form.
     */
    private Boolean displayedInDiscoveryFullView;

    /**
     * The list of VocabularySourceConfiguration objects for this field.
     * Note, some combinations of sources are nonsensical and though 
     * "valid" in the configuration may not be supported by the java
     * implementations.  Check the notes for the implementing class to
     * ensure compatibility.
     */
    private List<VocabularySourceConfiguration> vocabularySources;

    private List<String> disabledParts;
    
    private List<String> disabledAttributes;
    
    /**
     * Constructs a FieldConfiguration by parsing the given element
     * which is expected to either from from the xpath 
     * c:configuration/c:item/c:fields/c:field or 
     * d:definitions/d:fieldDefinition/d:defaultConfiguration
     * where c is shorthand for the namespace "info:photocat/configuration"
     * and c is the shorthand for the namespace "info:photocat/definitions"
     * @param fieldConfigurationEl the DOM Element for a field configuration
     * @throws XPathExpressionException 
     * @throws DataFormatException 
     */
    public FieldConfiguration(Element fieldConfigurationEl, String fieldType) throws XPathExpressionException, DataFormatException {
        XPath xpath = XPathHelper.getInstance().getXPath();
        
        this.fieldType = fieldType;
        if ((Boolean) xpath.evaluate("c:displayLabel", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            this.displayLabel = (String) xpath.evaluate("c:displayLabel", fieldConfigurationEl);
        }
        
        this.attributeLabelMap = new HashMap<String, String>();
        NodeList attributeNameNl = (NodeList) xpath.evaluate("c:attributeDisplayLabel", fieldConfigurationEl, XPathConstants.NODESET);
        for (int i = 0; i < attributeNameNl.getLength(); i ++) {
            Element attributeDisplayLabelEl = (Element) attributeNameNl.item(i);
            this.attributeLabelMap.put(attributeDisplayLabelEl.getAttribute("attributeName"), (String) xpath.evaluate("text()", attributeDisplayLabelEl, XPathConstants.STRING));
        }
        
        this.partLabelMap = new HashMap<String, String>();
        NodeList partNameNl = (NodeList) xpath.evaluate("c:partDisplayLabel", fieldConfigurationEl, XPathConstants.NODESET);
        for (int i = 0; i < partNameNl.getLength(); i ++) {
            Element partDisplayLabelEl = (Element) partNameNl.item(i);
            this.partLabelMap.put(partDisplayLabelEl.getAttribute("partName"), (String) xpath.evaluate("text()", partDisplayLabelEl, XPathConstants.STRING));
        }
        
        if ((Boolean) xpath.evaluate("c:usageNotes", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            this.usageNotes = (String) xpath.evaluate("c:usageNotes", fieldConfigurationEl);
        }
        if ((Boolean) xpath.evaluate("c:readOnly", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            this.readOnly = Boolean.parseBoolean((String) xpath.evaluate("c:readOnly", fieldConfigurationEl, XPathConstants.STRING));
        }
        if ((Boolean) xpath.evaluate("c:repeatable", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            this.repeatable = Boolean.parseBoolean((String) xpath.evaluate("c:repeatable", fieldConfigurationEl, XPathConstants.STRING));
            if ((Boolean) xpath.evaluate("c:repeatable/@startingBoxes", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
                this.startingBoxes = new Integer((String) xpath.evaluate("c:repeatable/@startingBoxes", fieldConfigurationEl, XPathConstants.STRING));
            }
        }
        if ((Boolean) xpath.evaluate("c:displayedInCatalogingBriefView", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            this.displayedInCatalogingBriefView = Boolean.parseBoolean((String) xpath.evaluate("c:displayedInCatalogingBriefView", fieldConfigurationEl, XPathConstants.STRING));
        }
        if ((Boolean) xpath.evaluate("c:exposedInCatalogingFullView", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            this.exposedInCatalogingFullView = Boolean.parseBoolean((String) xpath.evaluate("c:exposedInCatalogingFullView", fieldConfigurationEl, XPathConstants.STRING));
        }
        if ((Boolean) xpath.evaluate("c:displayedInDiscoveryBriefView", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            this.displayedInDiscoveryBriefView = Boolean.parseBoolean((String) xpath.evaluate("c:displayedInDiscoveryBriefView", fieldConfigurationEl, XPathConstants.STRING));
        }
        if ((Boolean) xpath.evaluate("c:displayedInDiscoveryFullView", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            this.displayedInDiscoveryFullView = Boolean.parseBoolean((String) xpath.evaluate("c:displayedInDiscoveryFullView", fieldConfigurationEl, XPathConstants.STRING));
        }

        this.disabledParts = new ArrayList<String>();
        NodeList disabledPartsNl = (NodeList) xpath.evaluate("c:disable[@type='part']/@name", fieldConfigurationEl, XPathConstants.NODESET);
        if (disabledPartsNl.getLength() == 0) {
            this.disabledParts = Collections.emptyList();
        } else {
            this.disabledParts = new ArrayList<String>(disabledPartsNl.getLength());
            for (int i = 0; i < disabledPartsNl.getLength(); i ++) {
                this.disabledParts.add(disabledPartsNl.item(i).getNodeValue());
            }
        }
        
        NodeList disabledAttributesNl = (NodeList) xpath.evaluate("c:disable[@type='attribute']/@name", fieldConfigurationEl, XPathConstants.NODESET);
        if (disabledAttributesNl.getLength() == 0) {
            this.disabledAttributes = Collections.emptyList();
        } else {
            this.disabledAttributes = new ArrayList<String>(disabledAttributesNl.getLength());
            for (int i = 0; i < disabledAttributesNl.getLength(); i ++) {
                this.disabledAttributes.add(disabledAttributesNl.item(i).getNodeValue());
            }
        }

        NodeList sourcesNl = (NodeList) xpath.evaluate("c:vocabularySource", fieldConfigurationEl, XPathConstants.NODESET);
        this.vocabularySources = new ArrayList<VocabularySourceConfiguration>(sourcesNl.getLength());
        for (int i = 0; i < sourcesNl.getLength(); i ++) {
            this.vocabularySources.add(new VocabularySourceConfiguration(sourcesNl.item(i)));
        }
    }
    
    /**
     * A Simple program to test the parsing of field definitions from a 
     * sample configuration file.
     */
    public static void main(String args[]) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document configDoc = factory.newDocumentBuilder().parse("xsd/redesign/example-configuration.xml");
        XPath xpath = XPathHelper.getInstance().getXPath();
        
        NodeList nl = (NodeList) xpath.evaluate("c:configuration/c:item/c:fields/c:field", configDoc, XPathConstants.NODESET);
        for (int i = 0; i < nl.getLength(); i ++) {
            FieldConfiguration fc = new FieldConfiguration((Element) nl.item(i), (String) xpath.evaluate("@type", nl.item(i), XPathConstants.STRING));
            System.out.println(fc.fieldType + " - " + fc.getDisplayLabel() + " (" + fc.isDisplayedInCatalogingBriefView() + ")");
        }
    }
    
    /**
     * Constructs a FieldConfiguration that is a copy of the "values"
     * FieldConfiguration except that any null member variables are
     * replaced by those in the provided "defaultValues". 
     * @param values the specified values
     * @param defaultValues the values that should be used when no
     * specified value exists
     */
    public FieldConfiguration(FieldConfiguration values, FieldConfiguration defaultValues) {
        this.fieldType = (values.fieldType == null ? defaultValues.fieldType : values.fieldType);
        this.displayLabel = (values.displayLabel == null ? defaultValues.displayLabel : values.displayLabel);
        this.attributeLabelMap = new HashMap<String, String>();
        if (defaultValues.attributeLabelMap != null) {
            this.attributeLabelMap.putAll(defaultValues.attributeLabelMap);
        }
        if (values.attributeLabelMap != null) {
            this.attributeLabelMap.putAll(values.attributeLabelMap);
        }
        this.partLabelMap = new HashMap<String, String>();
        if (defaultValues.partLabelMap != null) {
            this.partLabelMap.putAll(defaultValues.partLabelMap);
        }
        if (values.partLabelMap != null) {
            this.partLabelMap.putAll(values.partLabelMap);
        }
        this.usageNotes = (values.usageNotes == null ? defaultValues.usageNotes : values.usageNotes);
        this.readOnly = (values.readOnly == null ? defaultValues.readOnly : values.readOnly);
        this.repeatable = (values.repeatable == null ? defaultValues.repeatable : values.repeatable);
        this.startingBoxes = (values.startingBoxes == null ? defaultValues.startingBoxes : values.startingBoxes);
        this.displayedInCatalogingBriefView = (values.displayedInCatalogingBriefView == null ? defaultValues.displayedInCatalogingBriefView : values.displayedInCatalogingBriefView);
        this.exposedInCatalogingFullView = (values.exposedInCatalogingFullView == null ? defaultValues.exposedInCatalogingFullView : values.exposedInCatalogingFullView);
        this.displayedInDiscoveryBriefView = (values.displayedInDiscoveryBriefView == null ? defaultValues.displayedInDiscoveryBriefView : values.displayedInDiscoveryBriefView);
        this.displayedInDiscoveryFullView = (values.displayedInDiscoveryFullView == null ? defaultValues.displayedInDiscoveryFullView : values.displayedInDiscoveryFullView);
        this.vocabularySources = values.vocabularySources;
        this.disabledParts = values.disabledParts;
        this.disabledAttributes = values.disabledAttributes;
        //System.out.println("Display Label: " + values.displayLabel + " | " + defaultValues.displayLabel + " --> " + this.displayLabel);
    }
    
    public String getFieldType() {
        return fieldType;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }
    
    /**
     * Returns the specified display label for the given part name,
     * or if none is specified, returns the part name.
     * @param partName the name of a part of this field (this must
     * be one of those defined in the field definition).
     * @return the display label of the part if defined, or the part
     * name itself.
     */
    public String getPartDisplayLabel(String partName) {
        String partLabel = this.partLabelMap.get(partName);
        if (partLabel == null) {
            return partName;
        } else {
            return partLabel;
        }
    }
    
    public List<String> listRelabeledParts() {
        List<String> list = new ArrayList<String>(partLabelMap.keySet());
        Collections.sort(list);
        return list;
    }
    
    /**
     * Returns the specified display label for the given attribute
     * name, or if none is specified, returns the attribute name.
     * @param attributeName the name of a attribute of this field 
     * (this must be one of those defined in the field definition).
     * @return the display label of the attribute if defined, or the
     * attribute name itself.
     */
    public String getAttributeDisplayLabel(String attributeName) {
        String attributeLabel = this.attributeLabelMap.get(attributeName);
        if (attributeLabel == null) {
            return attributeName;
        } else {
            return attributeLabel;
        }
    }
    
    public List<String> listRelabeledAttributes() {
        List<String> list = new ArrayList<String>(attributeLabelMap.keySet());
        Collections.sort(list);
        return list;
    }

    public String getUsageNotes() {
        return usageNotes;
    }

    public Boolean isReadOnly() {
        return readOnly;
    }

    public Boolean isRepeatable() {
        return repeatable;
    }

    public Integer getStartingBoxes() {
        return startingBoxes;
    }

    public Boolean isDisplayedInCatalogingBriefView() {
        return displayedInCatalogingBriefView;
    }

    public Boolean isExposedInCatalogingFullView() {
        return exposedInCatalogingFullView;
    }

    public Boolean isDisplayedInDiscoveryBriefView() {
        return displayedInDiscoveryBriefView;
    }

    public Boolean isDisplayedInDiscoveryFullView() {
        return displayedInDiscoveryFullView;
    }

    public List<VocabularySourceConfiguration> getVocabularySources() {
        return this.vocabularySources;
    }
    
    public boolean isPartDisabled(String partName) {
        return this.disabledParts.contains(partName);
    }
    
    public List<String> listDisabledParts() {
        return this.disabledParts;
    }
    
    public boolean isAttributeDisabled(String attributeName) {
        return this.disabledAttributes.contains(attributeName);
    }
    
    public List<String> listDisabledAttributes() {
        return this.disabledAttributes;
    }
    
}
