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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A data structure (class) that contains the structured data
 * from a field.
 * 
 * For every field, there are any number of 'attributes' which 
 * are associated with the field as a whole (attributes aren't
 * repeated with repeated values).
 * 
 * There are also any number of "values".  Each "value" is really
 * a series of "parts" that make up the value.
 * 
 * For example, a "Field" that represents the concept of "person
 *  name" may be stored with an "attribute" describing the position
 *  of everyone in the picture, followed by one "value" for every
 *  person, each consisting of "parts" for "family name", "given name"
 *  and "nick name".
 */
public class FieldData {

    /**
     * The field type for this field.
     */
    private String fieldType;
    
    /**
     * This is the part of the entered value or values that apply
     * to all (if repeatable) of the "field".
     */
    private List<NameValuePair> attributes;
    
    /**
     * This is the entered values.
     */
    private List<List<NameValuePair>> values;

    public FieldData(String fieldType) {
        this(fieldType, null, null);
    }
    
    public FieldData(String fieldType, List<NameValuePair> attributes, List<List<NameValuePair>> values) {
        if (fieldType == null) {
            throw new IllegalArgumentException("FieldType must not be null!");
        }

        if (values == null) {
            values = new ArrayList<List<NameValuePair>>();
        }
        if (attributes == null) {
            attributes = new ArrayList<NameValuePair>();
        }
        this.fieldType = fieldType;
        this.attributes = attributes;
        this.values = values;
    }
    
    /**
     * Constructs a FieldData object representing the value of
     * the supplied Element that is expected to be a valid
     * fieldType object according the the metadata schema at
     * info:photocat/metadata. 
     */
    public FieldData(Element fieldElement) throws DataFormatException {
        XPath xpath = XPathHelper.getInstance().getXPath();
        try {
            this.fieldType = (String) xpath.evaluate("@fieldType", fieldElement, XPathConstants.STRING);
            
            NodeList attributesNl = (NodeList) xpath.evaluate("m:attribute", fieldElement, XPathConstants.NODESET);
            this.attributes = new ArrayList<NameValuePair>();
            for (int i = 0; i < attributesNl.getLength(); i ++) {
                Element attributeEl = (Element) attributesNl.item(i);
                this.attributes.add(new NameValuePair(attributeEl.getAttribute("name"), (String) xpath.evaluate(".", attributeEl, XPathConstants.STRING)));
            }
            
            NodeList valueNl = (NodeList) xpath.evaluate("m:values/m:value", fieldElement, XPathConstants.NODESET);
            this.values = new ArrayList<List<NameValuePair>>();
            for (int inputIndex = 0; inputIndex < valueNl.getLength(); inputIndex ++) {
                List<NameValuePair> parts = new ArrayList<NameValuePair>();
                NodeList partsNl = (NodeList) xpath.evaluate("m:part", valueNl.item(inputIndex), XPathConstants.NODESET);
                for (int i = 0; i < partsNl.getLength(); i ++) {
                    Element partEl = (Element) partsNl.item(i);
                    parts.add(new NameValuePair(partEl.getAttribute("property"), (String) xpath.evaluate(".", partEl, XPathConstants.STRING)));
                }
                this.values.add(parts);
            }
        } catch (XPathExpressionException ex) {
            // This won't happen except in the even of
            // a programming error since the xpath 
            // expression isn't built from run-time data.
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Returns a field element to be nested in the itemMetadata element
     * of a Item Metadata document.  This method doesn't actually add
     * the element, though it does create an element that is "owned by"
     * the provided document.
     * @param doc the itemMetadata document
     */
    public Element toFieldEl(Document doc) {
        Element fieldEl = doc.createElementNS(XPathHelper.M_URI, "m:field");
        fieldEl.setAttribute("fieldType", this.fieldType);
        
        for (NameValuePair attribute : this.attributes) {
            Element attributeEl = doc.createElementNS(XPathHelper.M_URI, "m:attribute");
            attributeEl.setAttribute("name", attribute.getName());
            attributeEl.appendChild(doc.createTextNode(attribute.getValue()));
            fieldEl.appendChild(attributeEl);
        }
        
        Element valuesEl = doc.createElementNS(XPathHelper.M_URI, "m:values");
        for (List<NameValuePair> parts : this.values) {
            Element valueEl = doc.createElementNS(XPathHelper.M_URI, "m:value");
            for (NameValuePair part : parts) {
                Element partEl = doc.createElementNS(XPathHelper.M_URI, "m:part");
                partEl.setAttribute("property", part.getName());
                partEl.appendChild(doc.createTextNode(part.getValue()));
                valueEl.appendChild(partEl);
            }
            if (valueEl.hasChildNodes()) {
                valuesEl.appendChild(valueEl);
            }
        }
        if (valuesEl.hasChildNodes()) {
            fieldEl.appendChild(valuesEl);
        }
        
        return fieldEl;
    }
    
    /**
     * Gets the field type.
     */
    public String getFieldType() {
        return this.fieldType;
    }
    
    /**
     * Gets the number of entered values.  For a non-repeatable
     * field, this should be 0 or 1, but may be higher if the
     * configuration changed or if data was entered through 
     * alternate channels.  For complex fields, this may 
     */
    public int getEnteredValueCount() {
        return values.size();
    }
    
    public List<List<NameValuePair>> getParts() {
        return values;
    }
    
    /**
     * Gets the parts (various properties that make up the "value")
     * @param valueIndex the index (starting at zero) for the value
     * to retrieve.
     * @return an unmodifiable List of the parts
     */
    public List<NameValuePair> getParts(int valueIndex) {
        return values.get(valueIndex);
    }

    /**
     * A convenience method to get a list of all the values 
     * for a given part.
     */
    public List<String> getPartValues(String partName) {
        List<String> partValues = new ArrayList<String>();
        for (List<NameValuePair> value : this.values) {
            for (NameValuePair part : value) { 
                if (part.getName().equals(partName)) {
                    partValues.add(part.getValue());
                }
            }
        }
        return partValues;
    }
    
    /**
     * Gets the "attributes" for this field.  This may be any
     * values entered that are scoped the field (and all values)
     * rather than scoped to coincide with one particular value
     * entered for the field.
     * @return the attributes for this field
     */
    public List<NameValuePair> getAttributes() {
        return this.attributes;
    }
    
    /**
     * Removes the value (all parts) at the given index.
     */
    public List<NameValuePair> removeValue(int index) {
        return this.values.remove(index);
    }

    public void replaceValuesWithPart(String partName, String partValue, String replacement) {
        if (replacement == null || replacement.trim().length() == 0) {
            removeValuesWithPart(partName, partValue);
        } else {
            for (List<NameValuePair> value : values) {
                for (int i = 0; i < value.size(); i ++) {
                    NameValuePair part = value.get(i);
                    if (part.getName().equals(partName) && part.getValue().equals(partValue)) {
                        value.set(i, new NameValuePair(partName, replacement));
                    }
                }
            }
        }
    }
    
    public void removeValuesWithPart(String partName, String partValue) {
        for (int i = 0; i < values.size(); i ++) {
            for (NameValuePair part : values.get(i)) {
                if (part.getName().equals(partName) && part.getValue().equals(partValue)) {
                    values.remove(i);
                    i --;
                }
            }
        }
    }
    
    /** 
     * Adds the value to the beginning.
     */
    public void addValue(NameValuePair ...parts) {
        List<NameValuePair> newValue = new ArrayList<NameValuePair>();
        for (NameValuePair part : parts) {
            newValue.add(part);
        }
        this.values.add(0, newValue);
    }
    
    public void addValues(List<List<NameValuePair>> values) {
        this.values.addAll(values);
    }

    public void setAttributes(List<NameValuePair> attributes) {
        this.attributes = attributes;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (attributes != null && attributes.size() > 0) {
            for (NameValuePair attribute : attributes) {
                sb.append(attribute.getValue());
            }
        }
        for (List<NameValuePair> value : values) {
            for (NameValuePair part : value) {
                part.getValue();
            }
        }
        return sb.toString();
    }
    
    public boolean equals(FieldData o) {
        return (o.fieldType.equals(fieldType) && o.attributes.equals(attributes) && o.values.equals(values));
    }
    
    public int hashCode() {
        return fieldType.hashCode() + attributes.hashCode() + values.hashCode();
    }
    
}
