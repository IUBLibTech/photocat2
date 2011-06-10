/*
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
package edu.indiana.dlib.catalog.fields.click.control.uifield;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.click.control.AbstractContainer;
import org.apache.click.control.AbstractLink;
import org.apache.click.control.Field;
import org.apache.click.control.HiddenField;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;
import org.apache.click.element.JsImport;
import org.apache.click.util.HtmlStringBuffer;
import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.fields.click.control.FieldValuesContainer;

/**
 * A container that represents any number of instances
 * of a repeatable group of controls.
 * 
 * Nesting controls in this will work only under the following
 * conditions:
 * <ol>
 *   <li>The entire form is submitted and processed (note: this excludes most ajax calls)</li>
 * </ol>
 */
public class RepeatableValueGroupContainer extends AbstractContainer implements FieldValuesContainer {

    private Logger LOGGER = Logger.getLogger(RepeatableValueGroupContainer.class);
    
    private static final long serialVersionUID = 1L;

    /**
     * A List of lists of parts, each list representing a single "value".
     */
    private List<List<NameValuePair>> multipartValueList;

    private boolean isDefault;
    
    private List<String> partList;
    
    /**
     * A Map from field part name to the Field that exposes the part.
     * This is only for explicitly bound parts.  Other parts (those
     * not explicitly bound by calls to setPartField()) but found in
     * the data will not be represented by this control.
     */
    private Map<String, Field> partToFieldBindingMap;
    
    private Set<String> suppressedPartNames;
    
    private HiddenField repetitionCountField;
    
    private AddAnotherLink addAnotherFieldLink;
    
    private FieldConfiguration config;
    
    private FieldDefinition def;
    
    private boolean displayLabels;
    
    /**
     * A list of lists of fields that represent groups.  These 
     * groups should not preserve their values unless all the
     * parts have valid values.
     */
    private List<String[]> partGroups;

    public RepeatableValueGroupContainer(FieldConfiguration config, FieldDefinition def) {
        super(config.getFieldType() + "_values");
        this.config = config;
        this.def = def;
        this.multipartValueList = new ArrayList<List<NameValuePair>>();
        this.partList = new ArrayList<String>();
        this.partToFieldBindingMap = new HashMap<String, Field>();
        this.suppressedPartNames = new HashSet<String>();
        this.isDefault = false;
        
        this.repetitionCountField = new HiddenField(this.getName()+ "_repetitionCount", Integer.class);
        if (this.config.isRepeatable()) {
            if (this.config.getStartingBoxes() != null) {
                this.repetitionCountField.setValueObject(new Integer(this.config.getStartingBoxes()));
            } else {
                this.repetitionCountField.setValueObject(new Integer(1));
            }
            this.add(this.repetitionCountField);
        } else {
            this.repetitionCountField.setValueObject(new Integer(1));
        }

        if (this.config.isRepeatable()) {
            this.addAnotherFieldLink = new AddAnotherLink();
            this.addAnotherFieldLink.setImageSrc("/images/plus.gif");
            this.add(this.addAnotherFieldLink);
        }
    }
    
    public void setPartField(String partName, Field field) {
        setPartField(partName, field, false);
    }
    
    public void setPartField(String partName, Field field, boolean suppressValue) {
        field.setName(this.config.getFieldType() + "_" + this.partList.size() + "-" + partName);
        this.add(field);
        if (this.partList.contains(partName)) {
            LOGGER.warn("Part \"" + partName + "\" was added to field " + this.config.getFieldType() + " more than once, only the last one will be displayed!");
            this.partList.remove(partName);
        }
        this.partList.add(partName);
        this.partToFieldBindingMap.put(partName, field);
        if (suppressValue) {
            this.suppressedPartNames.add(partName);
        }
    }
    
    /**
     * If nested controls wish to perform simplified ajax calls, this
     * method is useful because those calls can include the parameter
     * necessary for all controls to be processed.
     */
    public HiddenField getRepetitionCountHiddenField() {
        return this.repetitionCountField;
    }
    
    public int getRepetitionCount() {
        return (Integer) this.repetitionCountField.getValueObject();
    }
        
    /**
     * Overrides the superclass implementation without delegating
     * back.  This implementation first processes the embedded 
     * "repetition count field" and determines the number of repetitions
     * (which was updated by javascript).  Then for each repetition
     * this control creates updates the names of the nested controls and
     * invokes their onProcess() method and uses their processed value
     * to update the multipartValueList.
     */
    public boolean onProcess() {
        boolean continueProcessing = true;
        if (!this.repetitionCountField.onProcess()) {
            continueProcessing = false;
        }
        if (this.addAnotherFieldLink != null && !this.addAnotherFieldLink.onProcess()) {
            continueProcessing = false;
        }

        for (int i = 0; i < this.getRepetitionCount(); i ++) {
            List<NameValuePair> parts = new ArrayList<NameValuePair>();
            for (String partName : this.partList) {
                Field partField = this.partToFieldBindingMap.get(partName);
                String originalName = partField.getName();
                partField.setName(originalName + "_" + i);
                partField.setValue(getPartValue(i, partName));
                if (!partField.onProcess()) {
                    continueProcessing = false;
                }
                String value = partField.getValue();
                if (!this.suppressedPartNames.contains(partName) && value != null && value.trim().length() > 0) {
                    parts.add(new NameValuePair(partName, partField.getValue()));
                }
                partField.setName(originalName);
            }
            while (this.multipartValueList.size() < i + 1) {
                this.multipartValueList.add(null);
            }
            this.multipartValueList.set(i, parts);
        }
        return continueProcessing;
    }
    
    public List<List<NameValuePair>> getValues() {
        boolean isEmpty = true;
        for (List<NameValuePair> value : multipartValueList) {
            if (!value.isEmpty()) {
                isEmpty = false;
            }
        }
        List<List<NameValuePair>> result = (List<List<NameValuePair>>) (isEmpty ? Collections.emptyList() : multipartValueList);
        return result;
    }
    
    private String getPartValue(int valueIndex, String partName) {
        try {
            for (NameValuePair nvp : this.multipartValueList.get(valueIndex)) {
                if (nvp.getName().equals(partName)) {
                    return nvp.getValue();
                }
            }
        } catch (RuntimeException ex) {
            return null;
        }
        return null;
    }
    

    public void setDefaultValues(List<List<NameValuePair>> values) {
        if (values != null && (this.multipartValueList == null || this.multipartValueList.isEmpty())) {
            // create a deep copy
            this.multipartValueList = new ArrayList<List<NameValuePair>>();
            for (List<NameValuePair> value : values) {
                this.multipartValueList.add(new ArrayList<NameValuePair>(value));
            }
            this.isDefault = true;
        }
    }
    
    public void setValues(List<List<NameValuePair>> values) {
        if (values == null || values.isEmpty()) {
            if (this.isDefault) {
                // do nothing, there's no real value to overwrite the default
            } else {
                this.multipartValueList = new ArrayList<List<NameValuePair>>();
            }
        } else {
            this.multipartValueList = values;
            this.isDefault = false;
        }
        if (this.multipartValueList.size() > this.getRepetitionCount()) {
            if (config.isRepeatable()) {
                this.repetitionCountField.setValueObject(new Integer(this.multipartValueList.size() + 1));
            } else {
                this.repetitionCountField.setValueObject(new Integer(this.multipartValueList.size()));
            }
        }
    }
    
    public void onRender() {
        this.repetitionCountField.onRender();
        if (this.addAnotherFieldLink != null) {
            this.addAnotherFieldLink.onRender();
        }
        
        if (this.isDefault) {
            for (String partName : this.partList) {
                Field field = this.partToFieldBindingMap.get(partName);
                field.setStyle("color", "#A0A0A0");
                field.setAttribute("onfocus", "this.style.color='#000000';");
            }
        }
        
        for (int i = 0; i < this.getRepetitionCount(); i ++) {
            List<NameValuePair> parts = new ArrayList<NameValuePair>();
            this.updatePartNamesForIteration(i);
            for (String partName : this.partList) {
                this.partToFieldBindingMap.get(partName).setValue(getPartValue(i, partName));
            }
            for (String partName : this.partList) {
                Field partField = this.partToFieldBindingMap.get(partName);
                partField.onRender();
            }
            this.restorePartNamesAfterIteratation(i);
        }
    }
    
    public void render(HtmlStringBuffer buffer) {
        this.repetitionCountField.render(buffer);
        buffer.elementStart("table");
        buffer.closeTag();
        buffer.append("\n");
        
        if (this.displayLabels || this.config.isRepeatable()) {
            buffer.elementStart("tr");
            buffer.closeTag();
            buffer.append("\n");
            for (String partName : this.partList) {
                if (this.displayLabels && !this.suppressedPartNames.contains(partName)) {
                    buffer.elementStart("th");
                    buffer.closeTag();
                    buffer.append(this.config.getPartDisplayLabel(partName));
                    buffer.elementEnd("th");
                    buffer.append("\n");
                } else {
                    buffer.elementStart("th");
                    buffer.elementEnd();
                }
            }
            
            // This column is the unexpected parts
            buffer.elementStart("th");
            buffer.elementEnd();
            
            if (this.config.isRepeatable()) {
                buffer.elementStart("td");
                String id = this.getId() + "_add_another_td";
                buffer.appendAttribute("id", id);
                this.addAnotherFieldLink.setAddAnotherTdId(id);
                buffer.appendAttribute("rowspan", String.valueOf(this.getRepetitionCount() + 1));
                buffer.appendAttribute("valign", "bottom");
                buffer.closeTag();
                buffer.append("\n");
                this.addAnotherFieldLink.render(buffer);
                buffer.append("\n");
                buffer.elementEnd("td");
                buffer.append("\n");
            }
            buffer.elementEnd("tr");
            buffer.append("\n");
        }
        for (int i = 0; i < this.getRepetitionCount(); i ++) {
            // This table row represents the repeatable content
            buffer.elementStart("tr");
            if (i == 0) {
                buffer.appendAttribute("id", this.getId());
            }
            buffer.closeTag();
            buffer.append("\n");
            
            this.updatePartNamesForIteration(i);
            for (String partName : this.partList) {
                this.partToFieldBindingMap.get(partName).setValue(getPartValue(i, partName));
            }
            for (String partName : this.partList) {
                buffer.elementStart("td");
                buffer.closeTag();
                Field partField = this.partToFieldBindingMap.get(partName);
                partField.render(buffer);
                buffer.elementEnd("td");
                buffer.append("\n");
            }
            this.restorePartNamesAfterIteratation(i);
            
            // unexpected parts
            buffer.elementStart("td");
            buffer.closeTag();
            if (this.multipartValueList.size() > i) {
                this.appendUnexpectedParts(this.def.getDataSpecification().getValidPartNames(), this.multipartValueList.get(i), this.getId() + "_" + i + "_unexpected_parts", buffer);
            }
            buffer.elementEnd("td");
            buffer.append("\n");
            
            buffer.elementEnd("tr");
            buffer.append("\n");
        }
        buffer.elementEnd("table");
        buffer.append("\n");
    }

    public void appendUnexpectedParts(Collection<String> expectedNames, List<NameValuePair> value, String id, HtmlStringBuffer buffer) {
        List<NameValuePair> unexpectedParts = new ArrayList<NameValuePair>();
        for (NameValuePair part : value) {
            if (!expectedNames.contains(part.getName())) {
                unexpectedParts.add(part);
            }
        }
        
        if (!unexpectedParts.isEmpty()) {
            // create an image link to open the div
            //buffer.elementStart("img");
            
            // create a div with a list of unexpected values
            buffer.elementStart("a");
            buffer.appendAttribute("href", "javascript:displayElement('" + id + "');");
            buffer.closeTag();
            
            buffer.elementStart("img");
            buffer.appendAttribute("src", getContext().getRequest().getContextPath() + "/images/warn.png");
            buffer.appendAttribute("alt", "warning: unexpected values");
            buffer.elementEnd();
            
            buffer.elementEnd("a");
            
            buffer.elementStart("div");
            buffer.appendAttribute("class", "unexpected_values_div_hidden");
            buffer.appendAttribute("id", id);
            buffer.closeTag();
            buffer.append("\n");
            
            buffer.elementStart("dl");
            buffer.closeTag();
            buffer.append("\n");
            
            for (NameValuePair part : unexpectedParts) {
                buffer.elementStart("dt");
                buffer.closeTag();
                buffer.append(part.getName());
                buffer.elementEnd("dt");
                buffer.append("\n");
                buffer.elementStart("dd");
                buffer.closeTag();
                buffer.append(part.getValue());
                buffer.elementEnd("dd");
                buffer.append("\n");
            }
            
            buffer.elementEnd("dl");
            buffer.append("\n");
            
            buffer.elementStart("a");
            buffer.appendAttribute("href", "javascript:hideElement('" + id + "');");
            buffer.closeTag();
            buffer.append("close");
            buffer.elementEnd("a");
            
            buffer.elementEnd("div");
            buffer.append("\n");
        }
    }
    
    
    /**
     * Invokes onDestroy on all nested components in an
     * undefined order. 
     */
    public void onDestroy() {
        this.repetitionCountField.onDestroy();
        for (Field field : this.partToFieldBindingMap.values()) {
            field.onDestroy();
        }
        if (this.addAnotherFieldLink != null) {
            this.addAnotherFieldLink.onDestroy();
        }
    }
    
    public boolean addAnother() {
        this.repetitionCountField.setValueObject(new Integer(this.getRepetitionCount() + 1));
        return true;
    }
    
    
    /**
     * A simple extension of AddAnotherLink that only overrides
     * getHref() to return a javascript URL that invokes the
     * copyElement() javascript method with the appropriate 
     * parameters.
     */
    public class AddAnotherLink extends AbstractLink {

        private String addAnotherTdId;
        
        public String getHref() {
            return "javascript:CopyElement.copyElement('" + RepeatableValueGroupContainer.this.getId() + "', '" + repetitionCountField.getId() + "', '" + addAnotherTdId + "');";
        }
        
        public void setAddAnotherTdId(String id) {
            this.addAnotherTdId = id;
        }
        
        /**
         * Adds the Javascript import required to drive the
         * link that adds another copy of the repeatable elements.
         */
        public List<Element> getHeadElements() { 
            // Use lazy loading to ensure the JS is only added the 
            // first time this method is called. 
            if (headElements == null) { 
                // Get the head elements from the super implementation 
                headElements = super.getHeadElements(); 
    
                // Include the control's external JavaScript resource 
                JsImport jsImport = new JsImport("/js/copy-elements.js"); 
                headElements.add(jsImport);
                
                JsImport valuesImport = new JsImport("/js/unexpected-values.js");
                if (!headElements.contains(valuesImport)) {
                    headElements.add(valuesImport);
                }
                CssImport cssImport = new CssImport("/style/unexpected-values.css");
                if (!headElements.contains(cssImport)) {
                    headElements.add(cssImport);
                }
            } 
            return headElements;
        }

    }
    
    private void updatePartNamesForIteration(int i) {
        for (String partName : this.partList) {
            Field partField = this.partToFieldBindingMap.get(partName);
            String originalName = partField.getName();
            partField.setName(originalName + "_" + i);
        }
    }
    
    private void restorePartNamesAfterIteratation(int i) {
        for (String partName : this.partList) {
            Field partField = this.partToFieldBindingMap.get(partName);
            partField.setName(partField.getName().substring(0, partField.getName().lastIndexOf("_" + i)));
        }
    }

    public void setShowingLabels(boolean displayLabels) {
        this.displayLabels = displayLabels;
    }

    public boolean isShowingLabels() {
        return displayLabels;
    }

}
