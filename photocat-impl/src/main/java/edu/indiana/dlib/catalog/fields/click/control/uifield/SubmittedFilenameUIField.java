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
package edu.indiana.dlib.catalog.fields.click.control.uifield;

import java.util.List;

import org.apache.click.Control;
import org.apache.click.control.AbstractControl;
import org.apache.click.util.Format;
import org.apache.click.util.HtmlStringBuffer;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.fields.UIField;

public class SubmittedFilenameUIField extends AbstractControl implements UIField {

    private FieldDefinition def;
    
    private FieldConfiguration conf;
    
    private FieldData fieldData;
    
    public SubmittedFilenameUIField(FieldDefinition def, FieldConfiguration conf, CollectionConfiguration c) throws ConfigurationException {
        super(def.getType());
        List<String> validAttributeNames = def.getDataSpecification().getValidAttributeNames();
        List<String> validPartNames = def.getDataSpecification().getValidPartNames();
        if ((validAttributeNames != null && !validAttributeNames.isEmpty()) || (validPartNames == null || validPartNames.size() != 1 || !validPartNames.get(0).equals("filename"))) {
            throw new ConfigurationException(getMessage("error-invalid-one-part-no-attributes", this.getClass().getName(), "username"));
        }
        if (!conf.isReadOnly()) {
            throw new ConfigurationException(getMessage("error-not-read-only", conf.getDisplayLabel()));
        }
        if (conf.isRepeatable()) {
            throw new ConfigurationException(getMessage("error-repeatable", conf.getDisplayLabel()));
        }
        if (!conf.getFieldType().equals(def.getType())) {
            throw new ConfigurationException(getMessage("error-field-type-mismatch"));
        }
        
        this.def = def;
        this.conf = conf;
        
        this.fieldData = null;
    }
    
    public String getLabel() {
        return null;
    }
    
    public Control asClickControl() {
        return this;
    }

    public FieldConfiguration getFieldConfiguration() {
        return this.conf;
    }
    
    public FieldDefinition getFieldDefinition() {
        return this.def;
    }
    
    public String getFieldType() {
        return def.getType();
    }

    public String getValueSummary() {
        if (this.getFieldData() == null) {
            return getMessage("label-unknown");
        } else {
            List<String> filenames = this.getFieldData().getPartValues("filename");
            if (filenames == null || filenames.size() != 1) {
                return getMessage("label-unknown");
            } else {
                return filenames.get(0);
            }
        }
    }
    
    /**
     * A default implementation that simply escapes the result of
     * getValueSummary() for XHTML rendering.  This method should
     * be overridden if formatting should be incorporated into the
     * default display of this field.
     */
    public String getValueSummaryHtml() {
        return new Format().html(getValueSummary());
    }

    /**
     * This method does nothing and shouldn't be invoked.
     */
    public void setDefaultValue(FieldData values) {
    }

    public void setFieldData(FieldData values) {
        this.fieldData = values;
    }
    
    public FieldData getFieldData() {
        return this.fieldData;
    }
    
    /**
     * Lazily returns true in all cases.
     */
    public boolean isValueValid(FieldData values) {
        return true;
    }
    
    public void render(HtmlStringBuffer buffer) {
        buffer.append(this.getValueSummary());
    }
    
    /**
     * Implements UIField; returns true.
     */
    public boolean suppressIfEmptyAndReadOnly() {
        return true;
    }

    public boolean hasDerivativeParts() {
        return false;
    }
    
}
