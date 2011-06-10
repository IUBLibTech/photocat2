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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.fields.click.control.AbstractUIField;
import edu.indiana.dlib.catalog.fields.click.control.ValuePreservingFieldAttributesContainer;

/**
 * A generic UIField implementation that supports any field that 
 * can be represented as a single control (TextField, TextArea, Select)
 * with optional attached sources.
 * 
 * Requirements for this field to be a workable implementation for 
 * a particular field are:
 * <ul>
 *   <li>
 *     That field stores its data in a single field, or a single 
 *     field with an optional "id" and "authority" part in 
 *     the case where the value came from a vocabulary source
 *   </li>
 * </ul>
 */
public abstract class GenericSinglePartUIField extends AbstractUIField {

    private String mainPartName;
    
    public GenericSinglePartUIField(FieldDefinition def, FieldConfiguration conf) throws ConfigurationException {
        super(def, conf);
        attributesContainer = new ValuePreservingFieldAttributesContainer();
        add(attributesContainer);
        
        Collection<String> requiredPartNames = getRequiredPartNames();
        if (!def.getDataSpecification().getValidAttributeNames().isEmpty()) {
            throw new ConfigurationException(this.getClass().getName() + " (implementing " + def.getType() + ") does not support attributes!");
        } else if (requiredPartNames.size() > 1) {
            StringBuffer partNames = new StringBuffer();
            for (String partName : requiredPartNames) {
                if (partNames.length() > 0) {
                    partNames.append(", ");
                }
                partNames.append(partName);
            }
            throw new ConfigurationException(this.getClass().getName() + " (implementing " + def.getType() + ") only supports a single field part when no source is specified! (" + partNames + ")");
        } else {
            this.mainPartName = requiredPartNames.iterator().next();
            RepeatableValueGroupContainer vc = new RepeatableValueGroupContainer(conf, def);
            try {
                addFields(vc, this.mainPartName);
            } catch (Exception ex) {
                throw new ConfigurationException(ex);
            }
            valuesContainer = vc;
            add(valuesContainer);
        }
    }
    
    /**
     * Returns the required part names by making assumptions about 
     * the data specification and the use of this field.  These 
     * assumptions include:
     * <ul>
     *   <li>
     *     The exposed text field will represent the part whose
     *     name is included in the specification and is not
     *     "id" or "authority" which are by contention reserved
     *     for the "id" and "authority" of the vocabulary source. 
     *   </li>
     * </ul>
     */
    public Collection<String> getRequiredPartNames() {
        List<String> validPartNames = this.getFieldDefinition().getDataSpecification().getValidPartNames();
        List<String> requiredPartNames = new ArrayList<String>();
        for (String specifiedPartName : this.getFieldDefinition().getDataSpecification().getValidPartNames()) {
            if (!specifiedPartName.equals("id") && !specifiedPartName.equals("authority")) {
                requiredPartNames.add(specifiedPartName);
            }
        }
        return requiredPartNames;
    }
    
    /**
     * Returns "id" and "authority".
     */
    public Collection<String> getOptionalPartNames() {
        return Arrays.asList(new String[] {"id", "authority"});
    }
    
    /**
     * Returns the name of the part name that may be variable
     * between instances of this UIField.  This is the part of
     * the value that is not the "id" or "attribute".
     */
    public String getMainPartName() {
        return this.mainPartName;
    }
    
    /**
     * Gets the Field that will be used for the input
     * of the main part.
     */
    protected abstract void addFields(RepeatableValueGroupContainer container, String partName) throws Exception;

}
