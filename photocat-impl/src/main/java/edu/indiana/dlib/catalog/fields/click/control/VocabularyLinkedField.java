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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.element.Element;
import org.apache.click.element.JsScript;
import org.apache.click.extras.control.AbstractContainerField;
import org.apache.click.extras.control.AutoCompleteTextField;
import org.apache.click.util.HtmlStringBuffer;

import edu.indiana.dlib.catalog.servlets.CollectionPathRedirectionServlet;
import edu.indiana.dlib.catalog.vocabulary.VocabularySource;

/**
 * A field that represents an input box which is bound to one 
 * or more vocabulary sources.  If multiple sources are bound
 * to the field, an option is displayed that allows the selection
 * of a controlled vocabulary source.
 * 
 * Furthermore, this field displays a link to explore the
 * selected (or implied) vocabulary.  
 * 
 * When a value is entered that isn't present in the linked
 * vocabulary a little indicator is also displayed.
 * 
 */
public class VocabularyLinkedField extends AbstractContainerField {

    private Select vocabularySelect;

    private Map<String, VocabularySource> sourceMap;
    
    public VocabularyLinkedField(List<VocabularySource> sources) {
        this.sourceMap = new HashMap<String, VocabularySource>();
        this.vocabularySelect = new Select("linked_field_select");
        for (VocabularySource source : sources) {
            Option option = new Option(source.getId(), source.getDisplayName());
            this.vocabularySelect.add(option);
            this.sourceMap.put(option.getValue(), source);
        }
        this.vocabularySelect.setMultiple(false);
        this.vocabularySelect.setSelectedValues(Collections.singletonList(sources.get(0).getId()));
        this.add(this.vocabularySelect);
        
        this.add(new VocabularyBackedAutoCompleteTextField("name"));
    }
    
    private VocabularySource getCurrentSource() {
        List<Object> selectedValues = this.vocabularySelect.getSelectedValues();
        if (selectedValues != null && !selectedValues.isEmpty()) {
            return this.sourceMap.get(selectedValues.get(0));
        } else {
            return null;
        }
    }
    
    public class VocabularyBackedAutoCompleteTextField extends AutoCompleteTextField {
        
        public VocabularyBackedAutoCompleteTextField(String name) {
            super(name);
        }
        
        public List getAutoCompleteList(String criteria) {
            VocabularySource source = getCurrentSource();
            List<String> terms = new ArrayList<String>();
            if (source == null) {
                return Collections.EMPTY_LIST;
            } else {
                return source.getTermsWithPrefix(criteria, 25, 0);
            }
        }

        public List<Element> getHeadElements() {
            if (headElements == null) {
                headElements = super.getHeadElements();
            }
            
            // This overrides the parent's headElement
            // to better compute the path for the AJAX call
            String fieldId = getId();
            JsScript script = new JsScript();
            script.setId(fieldId + "_autocomplete");
            if (headElements.contains(script)) {
                headElements.remove(script);
                
                // Script must be executed as soon as browser dom is ready
                script.setExecuteOnDomReady(true);
                String contextPath = getContext().getRequest().getContextPath();
                HtmlStringBuffer buffer = new HtmlStringBuffer(150);
                buffer.append("new Ajax.Autocompleter(");
                buffer.append("'").append(fieldId).append("'");
                buffer.append(",'").append(fieldId).append("_auto_complete_div'");
                buffer.append(",'").append(contextPath).append(CollectionPathRedirectionServlet.getRequestPath(getContext().getRequest())).append(
                    "'");
                buffer.append(",").append(getAutoCompleteOptions()).append(");");
                script.setContent(buffer.toString());
                headElements.add(script);
            }
            return headElements;

        }

        /**
         * Returns "{minChars:3}".
         */
        public String getAutoCompleteOptions() {
            return "{minChars:3}";
        }
    }
    
}
