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
 * 
 * The member variables are public, but *should* not be edited
 * on copies unless it is known that instance isn't shared. 
 * Since originally this class was immutable, shared references
 * may be present in code.  To avoid this problem, when editing
 * these objects, make a copy first using the copy constructor.
 */
public class FieldConfiguration {
    
    /**
     * The id of the field definition file in which this field
     * is defined.
     */
    public String definitionId;
    
    /**
     * The "type" of field.  For those types known to this class
     * default values will be provided for the remaining fields
     * which may be overridden by the setter methods.
     */
    public String fieldType;
    
    /**
     * The label for this field as it should be displayed in
     * the cataloging and discovery applications.
     */
    public String displayLabel;
    
    /**
     * A map from part names to the display label for those parts.
     */
    public Map<String, String> partLabelMap;
    
    /**
     * A map from attribute names to the display label for those
     * attributes.
     */
    public Map<String, String> attributeLabelMap;

    /**
     * Notes on how this field should be used.  These should be
     * written for the cataloger and will be displayed as a tool-tip
     * or on the instruction page for a given field.
     */
    public String usageNotes;
    
    /**
     * True if this field is only to be displayed and not edited.
     */
    public Boolean readOnly;

    /**
     * True if this field may have multiple values.
     */
    public Boolean repeatable;
    
    /**
     * The initial number of input boxes to be displayed for
     * a repeatable field.  This value is ignored if repeatable
     * is set to false (and exactly one vlistalue will be allowed).
     */
    public Integer startingBoxes;

    /**
     * True if this field should be displayed in the
     * brief view of an item in the cataloging client
     * (ie, the search results listing).
     */
    public Boolean displayedInCatalogingBriefView;
    
    /**
     * False if this item is meant to be hidden from
     * view in the cataloging view of this item.  This
     * is suitable for boilerplate values that don't 
     * need to be seen or edited by catalogers.
     * 
     * This is no longer tracked.
     */
    public Boolean exposedInCatalogingFullView;
    
    /**
     * True if this field should be included in the 
     * brief view in the discovery interface.
     */
    public Boolean displayedInDiscoveryBriefView;
    
    /**
     * True if this field should be included in the 
     * full view in the discovery application.  When
     * false, this field can be considered internal
     * only as it will only be visible to catalogers
     * and those who view the records in their raw 
     * form.
     */
    public Boolean displayedInDiscoveryFullView;
    
    /**
     * True if the field should be strictly private.
     * If this is set to true, regardless of other
     * settings, this field would NOT be displayed
     * anywhere other than the cataloging view.
     */
    public Boolean isPrivate;

    /**
     * The list of VocabularySourceConfiguration objects for this field.
     * Note, some combinations of sources are nonsensical and though 
     * "valid" in the configuration may not be supported by the java
     * implementations.  Check the notes for the implementing class to
     * ensure compatibility.
     */
    public List<VocabularySourceConfiguration> vocabularySources;

    public List<String> disabledParts;
    
    public List<String> disabledAttributes;
    
    public int catalogingSortIndex;
    
    public int publicSortIndex;
    
    /**
     * Constructs a FieldConfiguration by parsing the given element
     * which is expected to either from from the xpath 
     * u:configuration/u:item/u:fields/u:field or 
     * d:definitions/d:fieldDefinition/d:defaultConfiguration
     * where c is shorthand for the namespace "info:photocat/configuration"
     * and c is the shorthand for the namespace "info:photocat/definitions"
     * @param fieldConfigurationEl the DOM Element for a field configuration
     * @throws XPathExpressionException 
     * @throws DataFormatException 
     */
    public FieldConfiguration(Element fieldConfigurationEl, String fieldType, String definitionId, int catalogingSortIndex, int publicSortIndex) throws XPathExpressionException, DataFormatException {
        this.definitionId = definitionId;
        this.fieldType = fieldType;
        this.catalogingSortIndex = catalogingSortIndex;
        this.publicSortIndex = publicSortIndex;
        XPath xpath = XPathHelper.getInstance().getXPath();
        if ((Boolean) xpath.evaluate("u:displayLabel", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            displayLabel = (String) xpath.evaluate("u:displayLabel", fieldConfigurationEl);
        }
        
        attributeLabelMap = new HashMap<String, String>();
        NodeList attributeNameNl = (NodeList) xpath.evaluate("u:attributeDisplayLabel", fieldConfigurationEl, XPathConstants.NODESET);
        for (int i = 0; i < attributeNameNl.getLength(); i ++) {
            Element attributeDisplayLabelEl = (Element) attributeNameNl.item(i);
            attributeLabelMap.put(attributeDisplayLabelEl.getAttribute("attributeName"), (String) xpath.evaluate("text()", attributeDisplayLabelEl, XPathConstants.STRING));
        }
        
        partLabelMap = new HashMap<String, String>();
        NodeList partNameNl = (NodeList) xpath.evaluate("u:partDisplayLabel", fieldConfigurationEl, XPathConstants.NODESET);
        for (int i = 0; i < partNameNl.getLength(); i ++) {
            Element partDisplayLabelEl = (Element) partNameNl.item(i);
            partLabelMap.put(partDisplayLabelEl.getAttribute("partName"), (String) xpath.evaluate("text()", partDisplayLabelEl, XPathConstants.STRING));
        }
        
        if ((Boolean) xpath.evaluate("u:usageNotes", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            usageNotes = (String) xpath.evaluate("u:usageNotes", fieldConfigurationEl);
        }
        if ((Boolean) xpath.evaluate("u:readOnly", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            readOnly = Boolean.parseBoolean((String) xpath.evaluate("u:readOnly", fieldConfigurationEl, XPathConstants.STRING));
        }
        if ((Boolean) xpath.evaluate("u:repeatable", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            repeatable = Boolean.parseBoolean((String) xpath.evaluate("u:repeatable", fieldConfigurationEl, XPathConstants.STRING));
            if ((Boolean) xpath.evaluate("u:repeatable/@startingBoxes", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
                startingBoxes = new Integer((String) xpath.evaluate("u:repeatable/@startingBoxes", fieldConfigurationEl, XPathConstants.STRING));
            }
        }
        if ((Boolean) xpath.evaluate("u:displayedInCatalogingBriefView", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            displayedInCatalogingBriefView = Boolean.parseBoolean((String) xpath.evaluate("u:displayedInCatalogingBriefView", fieldConfigurationEl, XPathConstants.STRING));
        }
        if ((Boolean) xpath.evaluate("u:exposedInCatalogingFullView", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            exposedInCatalogingFullView = Boolean.parseBoolean((String) xpath.evaluate("u:exposedInCatalogingFullView", fieldConfigurationEl, XPathConstants.STRING));
        }
        if ((Boolean) xpath.evaluate("u:displayedInDiscoveryBriefView", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            displayedInDiscoveryBriefView = Boolean.parseBoolean((String) xpath.evaluate("u:displayedInDiscoveryBriefView", fieldConfigurationEl, XPathConstants.STRING));
        }
        if ((Boolean) xpath.evaluate("u:displayedInDiscoveryFullView", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            displayedInDiscoveryFullView = Boolean.parseBoolean((String) xpath.evaluate("u:displayedInDiscoveryFullView", fieldConfigurationEl, XPathConstants.STRING));
        }
        if ((Boolean) xpath.evaluate("u:private", fieldConfigurationEl, XPathConstants.BOOLEAN)) {
            isPrivate = Boolean.parseBoolean((String) xpath.evaluate("u:private", fieldConfigurationEl, XPathConstants.STRING));
        }
        

        disabledParts = new ArrayList<String>();
        NodeList disabledPartsNl = (NodeList) xpath.evaluate("u:disable[@type='part']/@name", fieldConfigurationEl, XPathConstants.NODESET);
        if (disabledPartsNl.getLength() == 0) {
            disabledParts = Collections.emptyList();
        } else {
            disabledParts = new ArrayList<String>(disabledPartsNl.getLength());
            for (int i = 0; i < disabledPartsNl.getLength(); i ++) {
                disabledParts.add(disabledPartsNl.item(i).getNodeValue());
            }
        }
        
        NodeList disabledAttributesNl = (NodeList) xpath.evaluate("u:disable[@type='attribute']/@name", fieldConfigurationEl, XPathConstants.NODESET);
        if (disabledAttributesNl.getLength() == 0) {
            disabledAttributes = Collections.emptyList();
        } else {
            disabledAttributes = new ArrayList<String>(disabledAttributesNl.getLength());
            for (int i = 0; i < disabledAttributesNl.getLength(); i ++) {
                disabledAttributes.add(disabledAttributesNl.item(i).getNodeValue());
            }
        }

        NodeList sourcesNl = (NodeList) xpath.evaluate("u:vocabularySource", fieldConfigurationEl, XPathConstants.NODESET);
        vocabularySources = new ArrayList<VocabularySourceConfiguration>(sourcesNl.getLength());
        for (int i = 0; i < sourcesNl.getLength(); i ++) {
            vocabularySources.add(new VocabularySourceConfiguration(sourcesNl.item(i)));
        }
    }

    /**
     * A basic copy constructor.
     */
    public FieldConfiguration(FieldConfiguration source) {
        this(source, source);
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
        publicSortIndex = values.publicSortIndex;
        catalogingSortIndex = values.catalogingSortIndex;
        definitionId = (values.definitionId == null ? defaultValues.definitionId : values.definitionId);
        fieldType = (values.fieldType == null ? defaultValues.fieldType : values.fieldType);
        displayLabel = (values.displayLabel == null ? defaultValues.displayLabel : values.displayLabel);
        attributeLabelMap = new HashMap<String, String>();
        if (defaultValues.attributeLabelMap != null) {
            attributeLabelMap.putAll(defaultValues.attributeLabelMap);
        }
        if (values.attributeLabelMap != null) {
            attributeLabelMap.putAll(values.attributeLabelMap);
        }
        partLabelMap = new HashMap<String, String>();
        if (defaultValues.partLabelMap != null) {
            partLabelMap.putAll(defaultValues.partLabelMap);
        }
        if (values.partLabelMap != null) {
            partLabelMap.putAll(values.partLabelMap);
        }
        usageNotes = (values.usageNotes == null ? defaultValues.usageNotes : values.usageNotes);
        readOnly = (values.readOnly == null ? defaultValues.readOnly : values.readOnly);
        repeatable = (values.repeatable == null ? defaultValues.repeatable : values.repeatable);
        startingBoxes = (values.startingBoxes == null ? defaultValues.startingBoxes : values.startingBoxes);
        displayedInCatalogingBriefView = (values.displayedInCatalogingBriefView == null ? defaultValues.displayedInCatalogingBriefView : values.displayedInCatalogingBriefView);
        exposedInCatalogingFullView = (values.exposedInCatalogingFullView == null ? defaultValues.exposedInCatalogingFullView : values.exposedInCatalogingFullView);
        displayedInDiscoveryBriefView = (values.displayedInDiscoveryBriefView == null ? defaultValues.displayedInDiscoveryBriefView : values.displayedInDiscoveryBriefView);
        displayedInDiscoveryFullView = (values.displayedInDiscoveryFullView == null ? defaultValues.displayedInDiscoveryFullView : values.displayedInDiscoveryFullView);
        isPrivate = (values.isPrivate == null ? defaultValues.isPrivate : values.isPrivate);
        vocabularySources = new ArrayList<VocabularySourceConfiguration>(values.vocabularySources);
        disabledParts = new ArrayList<String>(values.disabledParts);
        disabledAttributes = new ArrayList<String>(values.disabledAttributes);
    }
    
    public String getDefinitionId() {
        return definitionId;
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
        String partLabel = partLabelMap.get(partName);
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
        String attributeLabel = attributeLabelMap.get(attributeName);
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
        return displayedInDiscoveryBriefView != null && displayedInDiscoveryBriefView.booleanValue() && !isPrivate();
    }

    public Boolean isDisplayedInDiscoveryFullView() {
        return displayedInDiscoveryFullView != null && displayedInDiscoveryFullView.booleanValue() && !isPrivate();
    }
    
    public Boolean isPrivate() {
        if (isPrivate == null) {
            return Boolean.FALSE;
        } else {
            return isPrivate;
        }
    }

    public List<VocabularySourceConfiguration> getVocabularySources() {
        return vocabularySources;
    }
    
    public boolean isPartDisabled(String partName) {
        return disabledParts.contains(partName);
    }
    
    public List<String> listDisabledParts() {
        return disabledParts;
    }
    
    public boolean isAttributeDisabled(String attributeName) {
        return disabledAttributes.contains(attributeName);
    }
    
    public List<String> listDisabledAttributes() {
        return disabledAttributes;
    }
    
    public int getCatalogingSortIndex() {
        return catalogingSortIndex;
    }
    
    public int getPublicSortIndex() {
        return publicSortIndex;
    }
    
    public boolean equals(Object o) {
        return o != null && o instanceof FieldConfiguration && this.fieldType.equals(((FieldConfiguration) o).fieldType) && this.definitionId.equals(((FieldConfiguration) o).definitionId);
    }
    
    public int hashCode() {
        return (fieldType + definitionId).hashCode();
    }
    
}
