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
import java.util.Collections;

import org.apache.click.control.Field;
import org.apache.click.control.TextArea;
import org.apache.click.control.TextField;
import org.apache.click.element.JsImport;
import org.apache.click.util.HtmlStringBuffer;

import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.config.VocabularySourceConfiguration;
import edu.indiana.dlib.catalog.fields.click.control.AbstractUIField;
import edu.indiana.dlib.catalog.fields.click.control.FieldExposingAttributesContainer;
import edu.indiana.dlib.catalog.fields.click.control.autocomplete.SourceSelectorField;
import edu.indiana.dlib.catalog.fields.click.control.autocomplete.VocabularySourceAutoCompleteTextField;
import edu.indiana.dlib.catalog.vocabulary.VocabularySourceInitializationException;
import edu.indiana.dlib.catalog.vocabulary.impl.VocabularySourceFactory;

public class PersonPicturedUIField extends AbstractUIField {

    public PersonPicturedUIField(FieldDefinition def, FieldConfiguration conf) throws ConfigurationException, IOException, VocabularySourceInitializationException {
        super(def, conf);
        
        RepeatableValueGroupContainer fc = new RepeatableValueGroupContainer(conf, def);
        TextField nameField = new TextField();
        TextField affiliationField = new TextField();
        
        if (conf.getVocabularySources() != null) {
            for (VocabularySourceConfiguration source : conf.getVocabularySources()) {
                if (source.getAuthorityBinding() != null) {
                    throw new ConfigurationException(this.getClass().getCanonicalName() + " (implementing " + conf.getFieldType() + ") does not support vocabulary sources with authorities!");
                }
                if (source.getValueBinding().equals("entered name")) {
                    SourceSelectorField sourceSelector = new SourceSelectorField(conf.getFieldType() + "_name_authority", VocabularySourceFactory.getInstance().getVocabularySource(source, def.getDefinitions().getSourceDefinition(source.getType())));
                    nameField = new VocabularySourceAutoCompleteTextField(conf.getFieldType() + "_name_value", sourceSelector);
                    fc.setPartField("name authority", sourceSelector, true);
                } else if (source.getValueBinding().equals("affiliation")) {
                    if (conf.isPartDisabled("affiliation")) {
                        throw new ConfigurationException(getMessage("exception-bound-field-may-not-be-disabled", "affiliation"));
                    }
                    SourceSelectorField sourceSelector = new SourceSelectorField(conf.getFieldType() + "_affiliation_name", VocabularySourceFactory.getInstance().getVocabularySource(source, def.getDefinitions().getSourceDefinition(source.getType())));
                    affiliationField = new VocabularySourceAutoCompleteTextField(conf.getFieldType() + "_affiliation_value", sourceSelector);
                    fc.setPartField("affiliation authority", sourceSelector, true);
                } else {
                    throw new ConfigurationException(this.getClass().getCanonicalName() + " (implementing " + conf.getFieldType() + ") has the vocabulary \"" + source.getId() + "\" bound to an illegal field, \"" + source.getValueBinding() + "\"!");
                }
            }
        }
        if (conf.isPartDisabled("entered name")) {
            throw new ConfigurationException(getMessage("exception-field-may-not-be-disabled", "entered name"));
        }
        fc.setPartField("entered name", nameField);
        if (!conf.isPartDisabled("affiliation")) {
            fc.setPartField("affiliation", affiliationField);
            fc.setShowingLabels(true);
        } else {
            fc.setShowingLabels(false);
            nameField.setWidth("32em");
        }
        valuesContainer = fc;
        add(valuesContainer);
        
        FieldExposingAttributesContainer ac = new FieldExposingAttributesContainer(conf);
        LocationInPictureTextArea textArea = new LocationInPictureTextArea();
        textArea.setRows(4);
        textArea.setCols(48);
        textArea.setNameField(nameField);
        if (!conf.isAttributeDisabled("location in picture")) {
            ac.setAttributeField("location in picture", textArea);
        }
        attributesContainer = ac;
        add(attributesContainer);
        
    }

    /**
     * Returns the required parts "entered name" and "affiliation"
     */
    public Collection<String> getRequiredPartNames() {
        return Arrays.asList(new String[] { "entered name", "affiliation" });
    }

    /**
     * Returns the optional parts, "given name", "family name", "nick name".
     */
    public Collection<String> getOptionalPartNames() {
        return Arrays.asList(new String[] {"name authority", "affiliation authority"});
    }
    
    /**
     * Returns the required attribute "location in picture".
     */
    public Collection<String> getRequiredAttributeNames() {
        return Collections.singleton("location in picture");
    }
    
    /**
     * @return true
     */
    public boolean supportsAttachedVocabularySources() {
        return true;
    }
    
    public class LocationInPictureTextArea extends TextArea {
        
        private Field nameField;
        
        public void setNameField(Field nameField) {
            this.nameField = nameField;
        }
        
        public void onRender() { 
            super.onRender();
            this.headElements = getHeadElements();
            JsImport jsImport = new JsImport("/js/import-values.js");
            if (!headElements.contains(jsImport)) {
                headElements.add(jsImport);
            }
        }
        
        public void render(HtmlStringBuffer buffer) {
            super.render(buffer);
            buffer.elementStart("a");
            buffer.appendAttribute("onclick", "importValues('" + nameField.getId() + "', '" + this.getId() + "'); return false;");
            buffer.closeTag();
            buffer.elementStart("img");
            buffer.appendAttribute("src", getContext().getRequest().getContextPath() + "/images/import.png");
            buffer.appendAttribute("title", getMessage("import-values"));
            buffer.appendAttribute("alt", getMessage("import-values"));
            buffer.elementEnd();
            buffer.elementEnd("a");
        }
    }
    
}
