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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.click.control.TextField;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.config.VocabularySourceConfiguration;
import edu.indiana.dlib.catalog.fields.click.control.AbstractUIField;
import edu.indiana.dlib.catalog.fields.click.control.ValuePreservingFieldAttributesContainer;
import edu.indiana.dlib.catalog.fields.click.control.autocomplete.SourceSelectorField;
import edu.indiana.dlib.catalog.fields.click.control.autocomplete.VocabularySourceAutoCompleteTextField;
import edu.indiana.dlib.catalog.vocabulary.VocabularySourceInitializationException;
import edu.indiana.dlib.catalog.vocabulary.impl.VocabularySourceFactory;

public class SetUIField extends AbstractUIField {

    public SetUIField(FieldDefinition def, FieldConfiguration conf, CollectionConfiguration c) throws ConfigurationException, IOException, VocabularySourceInitializationException {
        super(def, conf, c);
        
        attributesContainer = new ValuePreservingFieldAttributesContainer();
        add(attributesContainer);
        
        TextField setName = new TextField();
        TextField itemName = new TextField();
        
        RepeatableValueGroupContainer vc = new RepeatableValueGroupContainer(conf, def);
        
        if (conf.getVocabularySources() != null) {
            for (VocabularySourceConfiguration source : conf.getVocabularySources()) {
                if (source.getAuthorityBinding() != null) {
                    throw new ConfigurationException(this.getClass().getCanonicalName() + " (implementing " + conf.getFieldType() + ") does not support vocabulary sources with authorities!");
                }
                if (source.getValueBinding().equals("set name")) {
                    SourceSelectorField sourceSelector = new SourceSelectorField(conf.getFieldType() + "_set_name_authority", VocabularySourceFactory.getInstance().getVocabularySource(source, def.getDefinitions().getSourceDefinition(source.getType()), c.getId()));
                    setName = new VocabularySourceAutoCompleteTextField(conf.getFieldType() + "_set_name_value", sourceSelector);
                    vc.setPartField("set name authority", sourceSelector, true);
                } else {
                    throw new ConfigurationException(this.getClass().getCanonicalName() + " (implementing " + conf.getFieldType() + ") has the vocabulary \"" + source.getId() + "\" bound to an illegal field, \"" + source.getValueBinding() + "\"!");
                }
            }
        }
        
        vc.setPartField("set name", setName);
        vc.setPartField("item name", itemName);
        vc.setShowingLabels(true);
        valuesContainer = vc;
        add(valuesContainer);
    }

    public Collection<String> getRequiredPartNames() {
        return Arrays.asList(new String[] { "set name", "item name" });
    }
    
    /**
     * @return true
     */
    public boolean supportsAttachedVocabularySources() {
        return true;
    }

}
