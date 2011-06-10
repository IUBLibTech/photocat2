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
package edu.indiana.dlib.catalog.pages.collections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.click.control.ActionLink;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;

import edu.indiana.dlib.catalog.accesscontrol.AuthorizationSystemException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.VocabularySourceConfiguration;
import edu.indiana.dlib.catalog.fields.click.control.RelativeActionLink;
import edu.indiana.dlib.catalog.search.BrowseResult;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.impl.DefaultBrowseQuery;
import edu.indiana.dlib.catalog.vocabulary.ManagedVocabularySource;
import edu.indiana.dlib.catalog.vocabulary.VocabularySource;
import edu.indiana.dlib.catalog.vocabulary.VocabularySourceInitializationException;
import edu.indiana.dlib.catalog.vocabulary.VocabularyTerm;
import edu.indiana.dlib.catalog.vocabulary.impl.DefaultVocabularyTerm;
import edu.indiana.dlib.catalog.vocabulary.impl.VocabularySourceFactory;
import edu.indiana.dlib.catalog.vocabulary.impl.VocabularySourceTable;

public class SourcesPage extends CollectionPage {

    public static String VOCABULARY_SOURCE_PARAM_NAME = "vocabularySourceId";
    
    public VocabularySource currentSource;
    
    public VocabularySourceTable termTable;

    public List<ManagedVocabularySource> managedSources;
    
    public List<VocabularySource> unmanagedSources;
    
    public Map<String, String> sourceIdToFieldNameMap;
    
    public ActionLink resynchronizeAllLink;
    
    public void onInit() {
        super.onInit();
        
        this.managedSources = new ArrayList<ManagedVocabularySource>();
        this.unmanagedSources = new ArrayList<VocabularySource>();
        this.sourceIdToFieldNameMap = new HashMap<String, String>();
        
        // Get the VocabularySource instances
        String vocabularySourceId = getContext().getRequestParameter(VOCABULARY_SOURCE_PARAM_NAME);
        try {
            for (FieldConfiguration fieldConfig : collection.listFieldConfigurations()) {
                for (VocabularySourceConfiguration sourceConfig : fieldConfig.getVocabularySources()) {
                    VocabularySource source = VocabularySourceFactory.getInstance().getVocabularySource(sourceConfig, collection.getDefinitions().getSourceDefinition(sourceConfig.getType()));
                    if (source instanceof ManagedVocabularySource) {
                        this.managedSources.add((ManagedVocabularySource) source);
                    } else {
                        this.unmanagedSources.add(source);
                    }
                    this.sourceIdToFieldNameMap.put(source.getId(), fieldConfig.getDisplayLabel());
                    if (sourceConfig.getId().equals(vocabularySourceId)) {
                        this.currentSource = source; 
                    }
                }
            }
            
            // set up the form
            if (this.currentSource != null) {
                this.termTable = new VocabularySourceTable("termTable", this.currentSource, getAuthorizationManager().canManageCollection(collection, user));
                this.termTable.getControlLink().setParameter(VOCABULARY_SOURCE_PARAM_NAME, this.currentSource.getId());
                this.termTable.getDeleteLink().setParameter(VOCABULARY_SOURCE_PARAM_NAME, this.currentSource.getId());
                this.termTable.setPageSize(25);
                this.addControl(this.termTable);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (VocabularySourceInitializationException ex) {
            throw new RuntimeException(ex);
        } catch (AuthorizationSystemException ex) {
            throw new RuntimeException(ex);
        }
        
        try {
            if (!managedSources.isEmpty() && getAuthorizationManager().canManageApplication(user)) { 
                resynchronizeAllLink = new RelativeActionLink("resynchronize", getMessage("button-resynchronize"));
                resynchronizeAllLink.setListener(this, "onResynchronizeAll");
                this.addControl(resynchronizeAllLink);
            } else {
                System.out.println(user.getUsername() + " cannot manage " + collection.getCollectionMetadata().getName() + " collection.");
            }
        } catch (AuthorizationSystemException ex) {
            throw new RuntimeException(ex);
        }
        
    }

    public List<Element> getHeadElements() { 
        // We use lazy loading to ensure the CSS import is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 

            CssImport cssImport = new CssImport("/css/sources.css"); 
            headElements.add(cssImport); 
        } 
        return headElements; 
    }
    
    protected String getBreadcrumbs() {
        if (this.currentSource != null) {
            return super.getBreadcrumbs() + getMessage("title-no-source") + getMessage("breadcrumbs-spacer");
        } else {
            return super.getBreadcrumbs();
        }
    }
    
    protected String getTitle() {
        if (currentSource != null && currentSource.getDisplayName() != null) {
            return currentSource.getDisplayName();
        } else {
            return getMessage("title-no-source");
        }
    }

    private List<String[]> getTypeAndPartBoundToSource(VocabularySource source) {
        List<String[]> ids = new ArrayList<String[]>();
        for (FieldConfiguration fieldConfig : collection.listFieldConfigurations()) {
            for (VocabularySourceConfiguration sourceConfig : fieldConfig.getVocabularySources()) {
                if (sourceConfig.getId().equals(source.getId())) {
                    ids.add(new String[] {fieldConfig.getFieldType(), sourceConfig.getValueBinding() });
                }
            }
        }
        return ids;
    }
    
    public boolean onResynchronizeAll() {
        SearchManager sm = getSearchManager();
        for (ManagedVocabularySource source : this.managedSources) {
            try {
                List<VocabularyTerm> terms = new ArrayList<VocabularyTerm>();
                for (String[] typePart : getTypeAndPartBoundToSource(source)) {
                    for (BrowseResult result : sm.browse(new DefaultBrowseQuery(collection.getId(), typePart[0], typePart[1], 0, 10000)).listBrowseResults()) {
                        terms.add(new DefaultVocabularyTerm(result.getFieldValue(), null));
                    }
                }

                source.clear();
                source.addTerms(terms);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return true;
    }

}
