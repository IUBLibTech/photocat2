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

import java.util.Collection;
import java.util.Collections;

import org.apache.click.control.Option;
import org.apache.click.control.Select;

import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.fields.UIField;
import edu.indiana.dlib.catalog.fields.click.control.AbstractUIField;
import edu.indiana.dlib.catalog.fields.click.control.ValuePreservingFieldAttributesContainer;

public class DLPStatusUIField extends AbstractUIField implements UIField {

    public DLPStatusUIField(FieldDefinition def, FieldConfiguration conf) throws ConfigurationException {
        super(def, conf);
        if (conf.isRepeatable()) {
            throw new ConfigurationException(getMessage("error-repeatable", conf.getDisplayLabel()));
        }
        if (!conf.getFieldType().equals(def.getType())) {
            throw new ConfigurationException(getMessage("error-field-type-mismatch"));
        }
        
        attributesContainer = new ValuePreservingFieldAttributesContainer();
        add(attributesContainer);

        Select statusSelector = new StatusSelect(conf.getFieldType());
        statusSelector.setReadonly(conf.isReadOnly());
     
        valuesContainer = new RepeatableValueGroupContainer(conf, def);
        ((RepeatableValueGroupContainer) valuesContainer).setPartField("status", statusSelector);
        add(valuesContainer);
    }
    
    /**
     * Returns the required parts "status".
     */
    public Collection<String> getRequiredPartNames() {
        return Collections.singleton("status");
    }
    
    public class StatusSelect extends Select {

        public StatusSelect(String name) {
            super(name);
            setMultiple(false);
            add(new Option("auto generated", DLPStatusUIField.this.getMessage("status-auto-generated")));
            add(new Option("in progress", DLPStatusUIField.this.getMessage("status-in-progress")));
            add(new Option("minimal", DLPStatusUIField.this.getMessage("status-minimal")));
            add(new Option("pending completion", DLPStatusUIField.this.getMessage("status-pending-completion")));
            add(new Option("cataloged", DLPStatusUIField.this.getMessage("status-cataloged")));
            setRequired(true);
        }
        
        public void validate() {
            if (this.getSelectedValues() != null && this.getSelectedValues().contains("auto generated")) {
                super.error = DLPStatusUIField.this.getMessage("invalid-status");
            }
        }
    }
    
}
