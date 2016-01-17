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

import java.util.Collection;
import java.util.Collections;

import org.apache.click.control.Option;
import org.apache.click.control.Select;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.fields.UIField;
import edu.indiana.dlib.catalog.fields.click.control.AbstractUIField;
import edu.indiana.dlib.catalog.fields.click.control.ValuePreservingFieldAttributesContainer;

public class DLPStatusUIField extends AbstractUIField implements UIField {

    public DLPStatusUIField(FieldDefinition def, FieldConfiguration conf, CollectionConfiguration c) throws ConfigurationException {
        super(def, conf, c);
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
            add(new Option("auto generated", "auto-generated"));
            add(new Option("in progress", "in progress"));
            add(new Option("minimal", "minimal"));
            add(new Option("pending completion", "pending completion"));
            add(new Option("cataloged", "cataloged"));
            setRequired(true);
        }
        
        public void validate() {
            if (this.getSelectedValues() != null && this.getSelectedValues().contains("auto generated")) {
                super.error = DLPStatusUIField.this.getMessage("invalid-status");
            }
        }
    }
    
}
