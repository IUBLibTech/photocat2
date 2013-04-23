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
package edu.indiana.dlib.catalog.fields.click.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.click.control.AbstractContainer;
import org.apache.click.control.Checkbox;
import org.apache.click.control.Field;
import org.apache.click.util.HtmlStringBuffer;

import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.NameValuePair;

/**
 * TODO: preserve values by including a catch-all field that preserves them
 * across HTTP round-trips (and exposes them to the user)
 */
public class FieldExposingAttributesContainer extends AbstractContainer implements FieldAttributesContainer {

    private List<String> attributeList;
    
    private Set<String> defaultAttributes;
    
    /**
     * A Map from field attribute name to the Field that exposes the part.
     * This is only for explicitly bound parts.  Other parts (those
     * not explicitly bound by calls to setPartField()) but found in
     * the data will not be represented by this control.
     */
    private Map<String, Field> attributeToFieldBindingMap;
    
    private FieldConfiguration config;
    
    public FieldExposingAttributesContainer(FieldConfiguration config) {
        super(config.getFieldType() + "_attributes");
        this.config = config;
        this.attributeList = new ArrayList<String>();
        this.attributeToFieldBindingMap = new HashMap<String, Field>();
        this.defaultAttributes = new HashSet<String>();
    }
    
    public void setAttributeField(String attributeName, Field field) {
        field.setName(this.config.getFieldType() + "_" + attributeName);
        this.add(field);
        if (this.attributeList.contains(attributeName)) {
            this.attributeList.remove(attributeName);
        }
        this.attributeList.add(attributeName);
        this.attributeToFieldBindingMap.put(attributeName, field);
    }
    
    public List<NameValuePair> getFieldAttributes() {
        List<NameValuePair> attributes = new ArrayList<NameValuePair>();
        for (String name : this.attributeList) {
            String value = this.attributeToFieldBindingMap.get(name).getValue();
            if (value != null && value.trim().length() > 0) {
                attributes.add(new NameValuePair(name, value));
            }
        }
        return attributes;
    }
    
    public void setDefaultAttributes(List<NameValuePair> attributes) {
        if (attributes != null) {
            for (NameValuePair attribute : attributes) {
                Field field = this.attributeToFieldBindingMap.get(attribute.getName());
                if (field != null && field.getValue() == null) {
                    field.setValue(attribute.getValue());
                    this.defaultAttributes.add(attribute.getName());
                }
            }
        }
    }
    
    public void setFieldAttributes(List<NameValuePair> attributes) {
        if (attributes != null) {
            for (NameValuePair attribute : attributes) {
                Field field = this.attributeToFieldBindingMap.get(attribute.getName());
                if (field != null) {
                    field.setValue(attribute.getValue());
                    this.defaultAttributes.remove(attribute.getName());
                } else {
                    //System.out.println("Ignoring (and possibly losing) the unrecognized attribute, \"" + name + "\" on field " + config.getFieldType() + ".");
                }
            }
        }
    }
    
    public void onRender() {
        super.onRender();
        for (String attributeName : this.defaultAttributes) {
            Field field = this.attributeToFieldBindingMap.get(attributeName);
            field.setStyle("color", "#A0A0A0");
            field.setAttribute("onfocus", "this.style.color='#000000';");
        }
    }
    
    public void render(HtmlStringBuffer buffer) {
        buffer.elementStart("table");
        buffer.closeTag();
        buffer.append("\n");
        for (String attributeName : this.attributeList) {
            if (this.attributeToFieldBindingMap.get(attributeName) instanceof Checkbox) {
                buffer.elementEnd("tr");
                buffer.append("\n");
                
                buffer.elementStart("td");
                buffer.closeTag();
                this.attributeToFieldBindingMap.get(attributeName).render(buffer);
                buffer.elementEnd("td");
                buffer.append("\n");
                
                buffer.elementStart("th");
                buffer.closeTag();
                buffer.append(this.config.getAttributeDisplayLabel(attributeName));
                buffer.elementEnd("th");
                buffer.append("\n");
                
                buffer.elementEnd("tr");
                buffer.append("\n");
            } else {
                buffer.elementEnd("tr");
                buffer.append("\n");
                
                buffer.elementStart("th");
                buffer.closeTag();
                buffer.append(this.config.getAttributeDisplayLabel(attributeName));
                buffer.elementEnd("th");
                buffer.append("\n");
                
                buffer.elementEnd("tr");
                buffer.append("\n");
                
                buffer.elementStart("tr");
                buffer.closeTag();
                buffer.append("\n");
                
                buffer.elementStart("td");
                buffer.closeTag();
                this.attributeToFieldBindingMap.get(attributeName).render(buffer);
                buffer.elementEnd("td");
                buffer.append("\n");
                
                buffer.elementEnd("tr");
                buffer.append("\n");
            }
        }
        buffer.elementEnd("table");
        buffer.append("\n");
    }
    
}
