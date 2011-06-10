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
import java.util.ArrayList;
import java.util.List;

import org.apache.click.Control;
import org.apache.click.control.AbstractContainer;
import org.apache.click.control.Checkbox;
import org.apache.click.control.Label;
import org.apache.click.util.HtmlStringBuffer;

import edu.indiana.dlib.catalog.config.Definitions;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.fields.click.control.FieldValuesContainer;
import edu.indiana.dlib.catalog.vocabulary.VocabularySource;
import edu.indiana.dlib.catalog.vocabulary.VocabularySourceInitializationException;
import edu.indiana.dlib.catalog.vocabulary.VocabularyTerm;
import edu.indiana.dlib.catalog.vocabulary.impl.DefaultVocabularyTerm;
import edu.indiana.dlib.catalog.vocabulary.impl.VocabularySourceFactory;

public class CheckboxArrayFieldValuesContainer extends AbstractContainer implements FieldValuesContainer {

    private String idPartName;
    
    private String valuePartName;
    
    private String authorityPartName;
    
    private boolean isDefault;
    
    public CheckboxArrayFieldValuesContainer(FieldConfiguration config, Definitions defs, String idPartName, String valuePartName, String authorityPartName) throws IOException, VocabularySourceInitializationException {
        
        this.isDefault = false;
        
        this.idPartName = idPartName;
        this.valuePartName = valuePartName;
        this.authorityPartName = authorityPartName;
        
        // for each source
        VocabularySourceFactory factory = VocabularySourceFactory.getInstance();
        for (VocabularySource source : factory.getVocabularySources(config, defs)) {
            for (VocabularyTerm term : source.listAllTerms(100, 0)) {
                this.add(new TermCheckbox(config.getFieldType(), term));
                this.add(new Label(term.getDisplayName()));
            }
        }
    }
    
    public List<List<NameValuePair>> getValues() {
        List<List<NameValuePair>> valueList = new ArrayList<List<NameValuePair>>();
        for (Control control : this.getControls()) {
            if (control instanceof TermCheckbox) {
                TermCheckbox tcb = (TermCheckbox) control;
                if (tcb.isChecked()) {
                    List<NameValuePair> parts = new ArrayList<NameValuePair>();
                    if (this.idPartName != null && tcb.term.getId() != null) {
                        parts.add(new NameValuePair(this.idPartName, tcb.term.getId()));
                    }
                    if (this.valuePartName != null && tcb.term.getDisplayName() != null) {
                        parts.add(new NameValuePair(this.valuePartName, tcb.term.getDisplayName()));
                    }
                    if (this.authorityPartName != null && tcb.term.getAuthorityId() != null && tcb.term.getAuthorityId() != null) {
                        parts.add(new NameValuePair(this.authorityPartName, tcb.term.getAuthorityId()));
                    }
                    valueList.add(parts);
                }
            }
        }
        return valueList;
    }


    public void setDefaultValues(List<List<NameValuePair>> values) {
        if ((values != null && !values.isEmpty()) && (this.getValues() == null || this.getValues().isEmpty())) {
            this.setValues(values);
            this.isDefault = true;
        }
    }
    
    public void setValues(List<List<NameValuePair>> values) {
        List<VocabularyTerm> selectedTerms = new ArrayList<VocabularyTerm>();
        if (values != null) {
            for (List<NameValuePair> value : values) {
                String id = null;
                String label = null;
                String authority = null;
                for (NameValuePair part : value) {
                    if (part.getName().equals(this.idPartName)) {
                        id = part.getValue();
                    } else if (part.getName().equals(this.valuePartName)) {
                        label = part.getValue();
                    } else if (part.getName().equals(this.authorityPartName)) {
                        authority = part.getValue();
                    }
                }
                selectedTerms.add(new DefaultVocabularyTerm(id, label, authority));
            }
        }
        for (Control control : this.getControls()) {
            if (control instanceof TermCheckbox) {
                TermCheckbox tcb = (TermCheckbox) control;
                if (hasMatchingTerm(selectedTerms, tcb.term)) {
                    tcb.setChecked(true);
                } else {
                    tcb.setChecked(false);
                }
            }
        }
        this.isDefault = false;
    }
    
    /**
     * Render this container children to the specified buffer.
     *
     * @see #getControls()
     *
     * @param buffer the buffer to append the output to
     */
    protected void renderChildren(HtmlStringBuffer buffer) {
        int columns = 8;
        int grouping = 2;
        int index = 0;
        buffer.elementStart("table");
        buffer.closeTag();
        buffer.append("\n");
        buffer.elementStart("tr");
        buffer.closeTag();
        buffer.append("\n");
        buffer.elementStart("td");
        buffer.closeTag();
        buffer.append("\n");
        for (Control control : getControls()) {
            if (index % columns == 0 && index != 0) {
                buffer.elementEnd("tr");
                buffer.append("\n");
                buffer.elementStart("tr");
                buffer.closeTag();
                buffer.append("\n");
            }
            if (index % grouping == 0 && index != 0) {
                buffer.elementEnd("td");
                buffer.append("\n");
                buffer.elementStart("td");
                buffer.closeTag();
                buffer.append("\n");
            }
            control.render(buffer);
            index ++;
        }
        buffer.elementEnd("td");
        buffer.append("\n");
        buffer.elementEnd("tr");
        buffer.append("\n");
        buffer.elementEnd("table");
        buffer.append("\n");
    }
    
    /**
     * Traverses the list to locate a term that "matches" the given term.
     * A matching term is one that has the same authority and for which
     * all other non-null fields are equal.  (or if authorities are to
     * be ignored, just has a matching name)
     */
    private boolean hasMatchingTerm(List<VocabularyTerm> terms, VocabularyTerm term) {
        for (VocabularyTerm existingTerm : terms) {
            if (this.authorityPartName == null || term.getAuthorityId().equals(existingTerm.getAuthorityId())) {
                if (term.getId() != null) {
                    if (term.getId().equals(existingTerm.getId())) {
                        return true;
                    }
                } else if (term.getDisplayName() != null && existingTerm.getId() == null && term.getDisplayName().equals(existingTerm.getDisplayName())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private class TermCheckbox extends Checkbox {
        
        private VocabularyTerm term;
        
        public TermCheckbox(String fieldType, VocabularyTerm term) {
            super(fieldType + "_" + (term.getId() == null ? term.getDisplayName() : term.getId()));
            this.term = term;
            this.setLabel(term.getDisplayName());
        }
    }

}
