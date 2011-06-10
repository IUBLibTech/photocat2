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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.fields.click.control.AbstractUIField;
import edu.indiana.dlib.catalog.fields.click.control.ValuePreservingFieldAttributesContainer;
import edu.indiana.dlib.catalog.vocabulary.VocabularySourceInitializationException;

public class EthnicGroupUIField extends AbstractUIField {

    public EthnicGroupUIField(FieldDefinition def, FieldConfiguration conf) throws ConfigurationException, IOException, VocabularySourceInitializationException {
        super(def, conf);
        attributesContainer = new ValuePreservingFieldAttributesContainer();
        add(attributesContainer);
        
        // create a container for the values, and add a field 
        // for each part that's configured
        if (conf.getVocabularySources() != null) {
            valuesContainer = new CheckboxArrayFieldValuesContainer(conf, def.getDefinitions(), null, "ethnic group", "authority");
        } else {
            throw new UnsupportedOperationException(this.getClass().getName() + " requires at least one controlled vocabulary source.");
        }
        add(valuesContainer);
    }

    /**
     * Returns the required parts "ethnic group" and "authority"
     */
    public Collection<String> getRequiredPartNames() {
        return Arrays.asList(new String[] { "ethnic group", "authority" });
    }

    /**
     * @return true
     */
    public boolean supportsAttachedVocabularySources() {
        return true;
    }
}
