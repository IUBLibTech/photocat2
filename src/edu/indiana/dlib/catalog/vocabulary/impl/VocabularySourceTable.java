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
package edu.indiana.dlib.catalog.vocabulary.impl;

import java.io.IOException;
import java.util.List;

import org.apache.click.Context;
import org.apache.click.control.ActionLink;
import org.apache.click.control.Column;
import org.apache.click.control.Decorator;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.PagingDataProvider;
import org.apache.click.extras.control.LinkDecorator;
import org.apache.click.util.ClickUtils;

import edu.indiana.dlib.catalog.fields.click.control.RelativeActionLink;
import edu.indiana.dlib.catalog.vocabulary.ManagedVocabularySource;
import edu.indiana.dlib.catalog.vocabulary.VocabularySource;
import edu.indiana.dlib.catalog.vocabulary.VocabularyTerm;

public class VocabularySourceTable extends Table {
    
    private VocabularySource source;
    
    private ActionLink deleteLink;
    
    public VocabularySourceTable(String name, VocabularySource source, boolean allowManagement) {
        super(name);
        this.source = source;
        this.deleteLink = new RelativeActionLink(getMessage("link-delete"), this, "onDeleteClick");
        this.add(this.deleteLink);
        Column termNameColumn = new Column("displayName", getMessage("label-term-name"));
        termNameColumn.setSortable(false);
        termNameColumn.setDecorator(new Decorator() {
            public String render(Object object, Context context) {
                VocabularyTerm term = (VocabularyTerm) object;
                return ClickUtils.escapeHtml(term.getDisplayName());
            }});
        this.addColumn(termNameColumn);
        
        for (final String property : this.source.getSupportedProperties()) {
            Column propertyColumn = new Column(property, property);
            propertyColumn.setSortable(false);
            propertyColumn.setDecorator(new Decorator() {
                public String render(Object object, Context context) {
                    VocabularyTerm term = (VocabularyTerm) object;
                    return ClickUtils.escapeHtml(getPropertyValuesSummary(term.getPropertyValues(property)));
                }});
            this.addColumn(propertyColumn);
        }
        
        // Add a column to delete a term
        if (allowManagement && this.source instanceof ManagedVocabularySource) {
            Column column = new Column(getMessage("label-action")); 
            column.setDecorator(new LinkDecorator(this, deleteLink, "displayName")); 
            column.setSortable(false); 
            this.addColumn(column); 
        }
        
        this.setPageSize(10);
        this.setPageNumber(0);
        this.setDataProvider(new VocabularySourcePagingDataProvider());
        this.setClass(CLASS_BLUE1);
    }
    
    public ActionLink getDeleteLink() {
        return this.deleteLink;
    }
    
    public ActionLink getControlLink() {
        if (controlLink == null) {
            controlLink = new RelativeActionLink();
        }
        return controlLink;
    }
    
    public boolean onDeleteClick() {
        String term = deleteLink.getValue();
        try {
            ((ManagedVocabularySource) source).removeTerm(new DefaultVocabularyTerm(term, term, this.source.getId()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return true; 
    }
    
    /**
     * Presents a given list of values as a string.  The current implementation
     * separates different values with a comma or returns an empty string if
     * no values exist.
     */
    private static String getPropertyValuesSummary(List<String> values) {
        if (values == null ||  values.isEmpty()) {
            return "";
        } else {
            StringBuffer sb = new StringBuffer();
            for (String value : values) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(value);
            }
            return sb.toString();
        }
    }
    
    private class VocabularySourcePagingDataProvider implements PagingDataProvider {

        public int size() {
            return source.getTermCount();
        }

        public Iterable getData() {
            return source.listAllTerms(getPageSize(), getFirstRow());
        }
        
    }
    
}
