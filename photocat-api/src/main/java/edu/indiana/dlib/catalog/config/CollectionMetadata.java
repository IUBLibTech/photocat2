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
import java.util.List;
import java.util.Properties;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

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
 * Metadata about the collection as a whole.  The current implementation
 * as well as the schema file on which it's based is tentative.
 */
public class CollectionMetadata {
    
    String id;
    
    String type;
    
    String fullName;

    String shortName;
    
    String description;
    
    String iconUrl;

    String homepageTitle;
    
    String homepageUrl;

    String collectionManagerAddress;
    
    String termsOfUse;
    
    List<BrowseSet> facets;
    
    SerializableSearchConstraint featuredItemConditions;
    
    SerializableSearchConstraint itemPublicationConditions;
    
    /**
     * May be null.
     */
    Properties recordCreationProperties;
    
    /**
     * May be null.
     */
    Properties imageSubmissionProperties;

    BlockViewsConfig blockViewsConfig;
    
    /**
     * Copy constructor.
     */
    public CollectionMetadata(CollectionMetadata cm) {
        id = cm.id;
        type = cm.type;
        fullName = cm.fullName;
        shortName = cm.shortName;
        description = cm.description;
        iconUrl = cm.iconUrl;
        homepageTitle = cm.homepageTitle;
        homepageUrl = cm.homepageUrl;
        collectionManagerAddress = cm.collectionManagerAddress;
        termsOfUse = cm.termsOfUse;
        facets = new ArrayList<BrowseSet>(cm.facets);
        featuredItemConditions = cm.featuredItemConditions;
        itemPublicationConditions = cm.itemPublicationConditions;
        recordCreationProperties = cm.recordCreationProperties;
        imageSubmissionProperties = cm.imageSubmissionProperties;
        blockViewsConfig = cm.blockViewsConfig;
    }
    
    /**
     * Constructs a CollectionMetadata object that represents
     * the metadata parsed from a collection element that
     * has been validated against the configuration schema.
     */
    public CollectionMetadata(Element el) throws DataFormatException {
        XPath xpath = XPathHelper.getInstance().getXPath();
        
        try {
            id = getRequiredElement("id", el, xpath);
            type = getRequiredElement("type", el, xpath);
            fullName = getRequiredElement("fullName", el, xpath);
            shortName = getRequiredElement("shortName", el, xpath);
            description = getRequiredElement("description", el, xpath);
            iconUrl = getRequiredElement("iconUrl", el, xpath);
            
            Element homepageEl = (Element) xpath.evaluate("u:homepage", el, XPathConstants.NODE);
            if (homepageEl != null) {
                homepageTitle = getRequiredElement("title", homepageEl, xpath);
                homepageUrl = getRequiredElement("url", homepageEl, xpath);
            }
            
            collectionManagerAddress = getOptionalElement("collectionManagerAddress", el, xpath);
            termsOfUse = getOptionalElement("termsOfUse", el, xpath);
            
            facets = new ArrayList<BrowseSet>();
            NodeList facetNl = (NodeList) xpath.evaluate("u:facets/u:collectionFacet", el, XPathConstants.NODESET);
            for (int i = 0; i < facetNl.getLength(); i ++) {
                String name = (String) xpath.evaluate("u:name", facetNl.item(i), XPathConstants.STRING);
                facets.add(new CollectionBrowseSet(name));
            }
            
            facetNl = (NodeList) xpath.evaluate("u:facets/*", el, XPathConstants.NODESET);
            for (int i = 0; i < facetNl.getLength(); i ++) {
                Node facetNode = facetNl.item(i);
                if (facetNode.getLocalName().equals("dateFacet")) {
                    String name = (String) xpath.evaluate("u:name", facetNl.item(i), XPathConstants.STRING);
                    String fieldType = (String) xpath.evaluate("u:fieldType", facetNl.item(i), XPathConstants.STRING);
                    String partName = (String) xpath.evaluate("u:fieldPart", facetNl.item(i), XPathConstants.STRING);
                    String format = (String) xpath.evaluate("u:dateFormat", facetNl.item(i), XPathConstants.STRING);
                    facets.add(new DateBrowseSet(name, fieldType, partName, format));
                } else if (facetNode.getLocalName().equals("facet")) {
                    String name = (String) xpath.evaluate("u:name", facetNl.item(i), XPathConstants.STRING);
                    String fieldType = (String) xpath.evaluate("u:fieldType", facetNl.item(i), XPathConstants.STRING);
                    String partName = (String) xpath.evaluate("u:fieldPart", facetNl.item(i), XPathConstants.STRING);
                    facets.add(new FieldPartBrowseSet(name, fieldType, partName));
                } else if (facetNode.getLocalName().equals("enumeratedFacet")) {
                    String name = (String) xpath.evaluate("u:name", facetNl.item(i), XPathConstants.STRING);
                    List<EnumeratedBrowseSet.Entry> entries = new ArrayList<EnumeratedBrowseSet.Entry>();
                    NodeList facetValueNl = (NodeList) xpath.evaluate("u:facetValue", facetNl.item(i), XPathConstants.NODESET);
                    for (int j = 0; j < facetValueNl.getLength(); j ++) {
                        entries.add(parseEnumeratedBrowseSetEntry(xpath, (Element) facetValueNl.item(j)));
                    }
                    facets.add(new EnumeratedBrowseSet(name, entries));
                }
            }
            
            featuredItemConditions = parseConditionalAsSearchConstraint((Element) xpath.evaluate("u:featured", el, XPathConstants.NODE), xpath);
            
            itemPublicationConditions = parseConditionalAsSearchConstraint((Element) xpath.evaluate("u:publish", el, XPathConstants.NODE), xpath);

            Element blockViewsEl = (Element) xpath.evaluate("u:blockViews", el, XPathConstants.NODE);
            if (blockViewsEl != null) {
                blockViewsConfig = new BlockViewsConfig(blockViewsEl.getAttribute("scope"), parseConditionalAsSearchConstraint(blockViewsEl, xpath));
            }
            
            recordCreationProperties = parseProperties((Element) xpath.evaluate("u:enableRecordCreation", el, XPathConstants.NODE), xpath);
            imageSubmissionProperties = parseProperties((Element) xpath.evaluate("u:enableImageSubmission", el, XPathConstants.NODE), xpath);

        } catch (XPathException ex) {
            // Shouldn't happen because the xpath isn't dynamicaly generated
            throw new RuntimeException(ex);
        }
    }

    private Properties parseProperties(Element parentEl, XPath xpath) throws XPathExpressionException {
        if (parentEl != null) {
            Properties p = new Properties();
            NodeList pNl = (NodeList) xpath.evaluate("u:property", parentEl, XPathConstants.NODESET);
            for (int i = 0; i < pNl.getLength(); i ++) {
                Element pEl = (Element) pNl.item(i);
                p.setProperty(pEl.getAttribute("name"), (String) xpath.evaluate("text()", pEl, XPathConstants.STRING));
            }
            return p;
        } else {
            return null;
        }
    }
    
    /**
     * This method parses a subset of elements of the "conditionalType"
     * into a SerializableSearchConstraint that reflects the logic
     * encoded into the XML.  Currently this method only supports the
     * OR operator.
     * This is the counterpart of: CollectionConfigurationData.appendSerializableSearchConstraint()
     * and need only be compatible with it.
     */
    private SerializableSearchConstraint parseConditionalAsSearchConstraint(Element el, XPath xpath) throws XPathExpressionException {
        if (el == null) {
            return null;
        }
        if (el.getLocalName().equals("or")) {
            List<SerializableSearchConstraint> constraints = new ArrayList<SerializableSearchConstraint>();
            NodeList children = el.getChildNodes();
            for (int i = 0; i < children.getLength(); i ++) {
                Node child = children.item(i);
                if (child instanceof Element) {
                    constraints.add(parseConditionalAsSearchConstraint((Element) child, xpath));
                }
            }
            return new OrSearchConstraintGroup(constraints);
        } else if (el.getLocalName().equals("condition")) {
            if ("FIELD_COMPARISON".equals(el.getAttribute("type"))) {
                String field = (String) xpath.evaluate("u:property[@name='field']", el, XPathConstants.STRING);
                String part = (String) xpath.evaluate("u:property[@name='part']", el, XPathConstants.STRING);
                String value = (String) xpath.evaluate("u:property[@name='value']", el, XPathConstants.STRING);
                return new FieldPartValueSearchConstraint(field, part, value);
            } else {
                throw new RuntimeException("Unsupported condition type: \"" + el.getAttribute("type"));
            }
        } else if (el.getLocalName().equals("featured") || el.getLocalName().equals("publish") || el.getLocalName().equals("blockViews")){
            List<SerializableSearchConstraint> constraints = new ArrayList<SerializableSearchConstraint>();
            NodeList children = el.getChildNodes();
            for (int i = 0; i < children.getLength(); i ++) {
                Node child = children.item(i);
                if (child instanceof Element) {
                    constraints.add(parseConditionalAsSearchConstraint((Element) child, xpath));
                }
            }
            if (constraints.size() == 1) {
                return constraints.get(0);
            } else {
                return new AndSearchConstraintGroup(constraints);
            }
        } else {
            throw new RuntimeException("Unexpected or unsupported element: " + el.getNodeName());
        }
    }
    
    private EnumeratedBrowseSet.Entry parseEnumeratedBrowseSetEntry(XPath xpath, Element entryEl) throws XPathExpressionException {
        String name = (String) xpath.evaluate("u:name", entryEl, XPathConstants.STRING);
        String fieldType = (String) xpath.evaluate("u:fieldType", entryEl, XPathConstants.STRING);
        String partName = (String) xpath.evaluate("u:fieldPart", entryEl, XPathConstants.STRING);
        XPathExpression valueExpr = xpath.compile("u:fieldValue");
        if (Boolean.TRUE.equals((Boolean) valueExpr.evaluate(entryEl, XPathConstants.BOOLEAN))) {
            // terminal entry
            return new EnumeratedBrowseSet.Entry(name, fieldType, partName, (String) valueExpr.evaluate(entryEl, XPathConstants.STRING));
        } else {
            // entry requiring recursive parsing
            List<EnumeratedBrowseSet.Entry> entries = new ArrayList<EnumeratedBrowseSet.Entry>();
            NodeList facetValueNl = (NodeList) xpath.evaluate("u:facetValue", entryEl, XPathConstants.NODESET);
            for (int j = 0; j < facetValueNl.getLength(); j ++) {
                entries.add(parseEnumeratedBrowseSetEntry(xpath, (Element) facetValueNl.item(j)));
            }
            return new EnumeratedBrowseSet.Entry(name, fieldType, partName, entries);
        }
    }
    
    private String getRequiredElement(String paramName, Element el, XPath xpath) throws DataFormatException, XPathExpressionException {
        XPathExpression elXpath = xpath.compile("u:" + paramName);
        if ((Boolean) elXpath.evaluate(el, XPathConstants.BOOLEAN)) {
            return (String) elXpath.evaluate(el, XPathConstants.STRING);
        } else {
            throw new DataFormatException("Required field \"" + paramName + "\" was not found!");
        }
    }
    
    private String getOptionalElement(String paramName, Element el, XPath xpath) throws DataFormatException, XPathExpressionException {
        XPathExpression elXpath = xpath.compile("u:" + paramName);
        if ((Boolean) elXpath.evaluate(el, XPathConstants.BOOLEAN)) {
            return (String) elXpath.evaluate(el, XPathConstants.STRING);
        } else {
            return null;
        }
    }
    
    public String getId() {
        return id;
    }
    
    public boolean isCollection() {
        return type.equals("collection");
    }
    
    public boolean isUnit() {
        return type.equals("unit");
    }

    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String value) {
        fullName = value;
    }
    
    public String getShortName() {
        return shortName;
    }
    
    public void setShortName(String value) {
        shortName = value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String value) {
        description = value;
    }
    
    public String getIconUrl() {
        return iconUrl;
    }
    
    public void setIcondUrl(String value) {
        iconUrl = value;
    }
    
    public String getHomepageTitle() {
        return homepageTitle;
    }
    
    public void setHomepageTitle(String value) {
        homepageTitle = value;
    }
    
    public String getHomepageUrl() {
        return homepageUrl;
    }
    
    public void setHomepageUrl(String value) {
        homepageUrl = value;
    }
    
    public String getCollectionManagerAddress() {
        return collectionManagerAddress;
    }
    
    public void setCollectionManagerAddress(String value) {
        collectionManagerAddress = value;
    }
    
    public String getTermsOfUse() {
        return termsOfUse;
    }
    
    public void setTermsOfUse(String value) {
        termsOfUse = value;
    }
    
    public List<BrowseSet> getFacets() {
        return facets;
    }
    
    public void setFacets(List<BrowseSet> value) {
        facets = value;
    }
    
    public SerializableSearchConstraint getConditionForFeaturedItems() {
        return featuredItemConditions;
    }
    
    public void setConditionForFeaturedItems(SerializableSearchConstraint value) {
        featuredItemConditions = value;
    }
    
    public SerializableSearchConstraint getConditionsForPublication() {
        return itemPublicationConditions;
    }
    
    public void setConditionsForPublication(SerializableSearchConstraint value) {
        itemPublicationConditions = value;
    }

    public BlockViewsConfig getBlockViewsConfig() {
        return blockViewsConfig;
    }
    
    public void setBlockViewsConfig(BlockViewsConfig value) {
        blockViewsConfig = value;
    }
    
    public Properties getRecordCreationProperties() {
        return recordCreationProperties;
    }
    
    public void setRecordCreationProperties(Properties value) {
        recordCreationProperties = value;
    }
    
    public Properties getImageSubmissionProperties() {
        return imageSubmissionProperties;
    }
    
    public void setImageSubmissionProperties(Properties value) {
        imageSubmissionProperties = value;
    }

    public Object getType() {
        return type;
    }
    
    public void setType(String value) {
        type = value;
    }
    
}
