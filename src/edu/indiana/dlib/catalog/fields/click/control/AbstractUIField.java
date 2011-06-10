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
package edu.indiana.dlib.catalog.fields.click.control;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.click.Control;
import org.apache.click.control.AbstractContainer;

import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.DataSpecification;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.fields.UIField;

/**
 * A convenient UIField implementation that handles basic 
 * stuff like data specification validation, summary value
 * generation, and field to value conversion (with the help
 * of FieldAttributesContainer and FieldValuesContainer 
 * implementations).
 */
public abstract class AbstractUIField extends AbstractContainer implements UIField {

    private FieldDefinition def;
    
    private FieldConfiguration conf;

    protected FieldAttributesContainer attributesContainer;

    protected FieldValuesContainer valuesContainer;
    
    private List<NameValuePair> preservedAttributes;
    
    private List<List<NameValuePair>> preservedValues;
    
    public AbstractUIField(FieldDefinition def, FieldConfiguration conf) throws ConfigurationException {
        this.def = def;
        this.conf = conf;
        if (!def.getType().equals(conf.getFieldType())) {
            throw new IllegalArgumentException("Field type mismatch!");
        }
        this.validateDataSpecification(this.def.getDataSpecification());
        
        if (!this.supportsAttachedVocabularySources() && conf.getVocabularySources() != null && !conf.getVocabularySources().isEmpty()) {
            throw new ConfigurationException(this.getClass().getName() + " (the implementation of " + def.getType() + ") does not suppport attached vocabulary sources!");
        }
    }
    
    /**
     * This method is called at construction time and compares the
     * specified fields against the results of getRequiredPartNames(),
     * getOptionalPartNames() and getRequiredAttributeNames()
     */
    protected void validateDataSpecification(DataSpecification spec) throws ConfigurationException {
        // These lists are small, so this method is relatively inefficient
        // with regards to list traversal, but more efficient with regards
        // to object instantiation

        List<String> specifiedParts = spec.getValidPartNames();
        Collection<String> requiredPartNames = getRequiredPartNames();
        Collection<String> optionalPartNames = getOptionalPartNames();
        // make sure all required parts are specified
        for (String requiredPartName : requiredPartNames) {
            if (!specifiedParts.contains(requiredPartName)) {
                throw new ConfigurationException("Part \"" + requiredPartName + "\" is expected by " + this.getClass().getName() + " to implement " + this.getFieldType() + ", but isn't listed in the specification!");
            }
        }
        
        // make sure no specified part is unrecognized
        for (String specifiedPartName : specifiedParts) {
            if (!requiredPartNames.contains(specifiedPartName) && !optionalPartNames.contains(specifiedPartName)) {
                throw new ConfigurationException("Part \"" + specifiedPartName + "\" was specified in the field definition but is not recognized by the java class (" + this.getClass().getName() + ") that implements that field!");
            }
        } 
        
        List<String> specifiedAttributes = spec.getValidAttributeNames();
        Collection<String> requiredAttributeNames = getRequiredAttributeNames();
        Collection<String> optionalAttributeNames = getOptionalAttributeNames();
        // make sure all required attributes are specified
        for (String requiredAttributeName : requiredAttributeNames) {
            if (!specifiedAttributes.contains(requiredAttributeName)) {
                throw new ConfigurationException("Attribute \"" + requiredAttributeName + "\" is expected by " + this.getClass().getName() + " to implement " + this.getFieldType() + ", but isn't listed in the specification!");
            }
        }
        
        // make sure no specified attribute is unrecognized
        for (String specifiedAttributeName : specifiedAttributes) {
            if (!requiredAttributeNames.contains(specifiedAttributeName) && !optionalAttributeNames.contains(specifiedAttributeName)) {
                throw new ConfigurationException("Attribute \"" + specifiedAttributeName + "\" was specified in the field definition but is not recognized by the java class (" + this.getClass().getName() + ") that implements that field!");
            }
        }
    }
    
    /**
     * Gets a collection of part names that must be included
     * in the specification.
     * 
     * This default implementation returns an empty list.
     * Unless subclasses override this method (or one of
     * the others) validateDataSpecification() will throw
     * an exception if any parts or attributes are specified.
     */
    public Collection<String> getRequiredPartNames() {
        return Collections.emptyList();
    }
    
    /**
     * Gets a collection of part names that may be included
     * in the specification.
     * 
     * This default implementation returns an empty list.
     * Unless subclasses override this method (or one of
     * the others) validateDataSpecification() will throw
     * an exception if any parts or attributes are specified.
     */
    public Collection<String> getOptionalPartNames() {
        return Collections.emptyList();
    }

    /**
     * Gets a collection of attribute names that must be included
     * in the specification.
     * 
     * This default implementation returns an empty list.
     * Unless subclasses override this method (or one of
     * the others) validateDataSpecification() will throw
     * an exception if any parts or attributes are specified.
     */
    public Collection<String> getRequiredAttributeNames() {
        return Collections.emptyList();
    }

    /**
     * Gets a collection of attribute names that may be included
     * in the specification.
     * 
     * This default implementation returns an empty list.
     * Unless subclasses override this method (or one of
     * the others) validateDataSpecification() will throw
     * an exception if any parts or attributes are specified.
     */
    public Collection<String> getOptionalAttributeNames() {
        return Collections.emptyList();
    }
    
    /**
     * Gets the FieldAttributesContainer that exposes the 
     * attributes for this field.  Subclasses must implement
     * this method such that it doesn't return a null value
     * by the time "getFieldValue()" or "setFieldValue()" are
     * called.  If this method returns a null value, this
     * UIField implementation will not expose field attributes
     * for editing, but may preserve existing values.
     */
    protected FieldAttributesContainer getFieldAttributesContainer() {
        return this.attributesContainer;
    }
    
    /**
     * Gets the FieldValuesContainer that exposes the 
     * values for this field.  Subclasses must implement
     * this method such that it doesn't return a null value
     * by the time "getFieldValue()" or "setFieldValue()" are
     * called.  If this method returns a null value, this
     * UIField implementation will not expose field values 
     * for editing, but may preserve existing values.
     */
    protected FieldValuesContainer getFieldValuesContainer() {
        return this.valuesContainer;
    }

    /**
     * Implements UIField, this method returns the FieldDefinition
     * that was supplied to the constructor.
     */
    public FieldDefinition getFieldDefinition() {
        return this.def;
    }

    /**
     * Implements UIField, this method returns the FieldConfiguration
     * that was supplied to the constructor.
     */
    public FieldConfiguration getFieldConfiguration() {
        return this.conf;
    }

    /**
     * Implements UIField, this method passes through to the 
     * FieldDefinition to return the type.
     */
    public String getFieldType() {
        return this.def.getType();
    }
    
    /**
     * A method that should be overridden by subclasses that
     * support the attachment of vocabulary sources.  The
     * current implementation returns false causing a 
     * ConfigurationException to be thrown by the constructor
     * if the configuration includes any vocabulary sources.
     */
    public boolean supportsAttachedVocabularySources() {
        return false;
    }

    /**
     * Implements UIField, this method invokes setDefaultFieldAttributes()
     * on the FieldAttributesContainer for this class and SetDefaultFieldValues()
     * on the FieldValuesContainer for this class.
     */
    public void setDefaultValue(FieldData values) {
        if (values != null) {
            if (values.getAttributes() != null) {
                if (this.attributesContainer != null) {
                    this.attributesContainer.setDefaultAttributes(values.getAttributes());
                } else {
                    this.preservedAttributes = values.getAttributes();
                }
            }
            if (values.getParts() != null) {
                if (this.valuesContainer != null) {
                    this.valuesContainer.setDefaultValues(values.getParts());
                } else {
                    this.preservedValues = values.getParts();
                }
            }
        }
    }
    
    /**
     * Implements UIField, this method invokes setFieldAttributes() on
     * the FieldAttributesContainer for this class, and setFieldValues()
     * on the FieldValuesContainer for this class.  Those implementations
     * in turn update the underlying Click Controls.
     */
    public void setFieldData(FieldData values) {
        if (values != null) {
            if (getFieldAttributesContainer() != null) {
                getFieldAttributesContainer().setFieldAttributes(values.getAttributes());
            } else {
                this.preservedAttributes = values.getAttributes();
            }
            if (getFieldValuesContainer() != null) {
                getFieldValuesContainer().setValues(values.getParts());
            } else {
                this.preservedValues = values.getParts();
            }
        } else {
            if (getFieldAttributesContainer() != null) {
                getFieldAttributesContainer().setFieldAttributes(null);
            }
            if (getFieldValuesContainer() != null) {
                getFieldValuesContainer().setValues(null);
            }
        }
    }
    
    /**
     * Implements UIField, this method invokes getFieldAttributes() on
     * the FieldAttributesContainer for this class, and getFieldValues()
     * on the FieldValuesContainer for this class.  Those implementations
     * in turn get the data from the underlying Click Controls.
     */
    public FieldData getFieldData() {
        return new FieldData(this.getFieldType(), 
                 (getFieldAttributesContainer() == null
                        ? this.preservedAttributes 
                        : getFieldAttributesContainer().getFieldAttributes()), 
                 (getFieldValuesContainer() == null 
                        ? this.preservedValues
                        : getFieldValuesContainer().getValues()));
    }
    
    /**
     * Returns a simple summary of a potentially complex set of field values
     * using the pattern specified in getFieldSummaryValuePattern().
     */
    public String getValueSummary() {
        StringBuffer sb = new StringBuffer();
        String pattern = this.getFieldSummaryValuePattern();
        if (pattern == null) {
            pattern = (this.getRequiredPartNames().isEmpty() ? "" : "{" + this.getRequiredPartNames().iterator().next() + "}");
        }
        for (List<NameValuePair> parts : this.getFieldData().getParts()) {
            String valueSummary = pattern;
            for (NameValuePair part : parts) {
                valueSummary = valueSummary.replace("{" + part.getName() + "}", part.getValue());
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(valueSummary);
        }
        return sb.toString();
    }

    /**
     * A method that allows subclasses to implement a pattern
     * that is used to generate a plain-text summary of a single
     * value.  The format for this pattern is as follows:
     * "{partName} {otherPartName}" where "partName" and "otherPartName"
     * are the names of parts for this field.  If a field value summary
     * cannot be expressed in this way, subclasses should override
     * the getValueSummary() method.
     * 
     * If not overridden or if the method returns null, the summary will
     * include just the first part of the data for every value.
     */
    protected String getFieldSummaryValuePattern() {
        return null;
    }
    
    /**
     * Implements UIField; returns this object.
     */
    public Control asClickControl() {
        return this;
    }

    /**
     * Implements UIField; returns false.  Subclasses that want to take
     * advantage of this feature may override this method.
     */
    public boolean suppressIfEmptyAndReadOnly() {
        return false;
    }
    
}
