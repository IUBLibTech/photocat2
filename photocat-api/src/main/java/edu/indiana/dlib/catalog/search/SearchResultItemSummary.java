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
package edu.indiana.dlib.catalog.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.indiana.dlib.catalog.config.Aspect;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.DataView;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.NameValuePair;

/**
 * A simple class containing just the information needed
 * to be displayed on a search results page.  In computing
 * this information it's often appropriate to consult not
 * only the metadata backing the result, but also the
 * configuration.  
 */
public class SearchResultItemSummary {

    private List<NameValuePair> fieldsToDisplay;
    
    private String imageUrl;
    
    private String identifier;
    
    private String collectionIdentifier;
    
    private String localIdentifier;
    
    public SearchResultItemSummary(Item item, CollectionConfiguration config) {
        this.identifier = item.getId();
        this.collectionIdentifier = item.getCollectionId();
        this.localIdentifier = item.getIdWithinCollection();
        Collection<Aspect> aspects = item.getAspects();
        if (aspects.size() > 0) {
            DataView preview = aspects.iterator().next().getThumbnailView();
            if (preview != null) {
                this.imageUrl = preview.getURL().toString();
            }
        }
        
        // now we process the fields
        this.fieldsToDisplay = new ArrayList<NameValuePair>();
        for (String fieldType : item.getMetadata().getRepresentedFieldTypes()) {
            FieldConfiguration fieldConf = config.getFieldConfiguration(fieldType);
            if (fieldConf != null && Boolean.TRUE.equals(fieldConf.isDisplayedInCatalogingBriefView())) {
                this.fieldsToDisplay.add(new NameValuePair(fieldConf.getDisplayLabel(), config.getValueSummary(item.getMetadata(), fieldType)));
            }
        }
        
    }

    /**
     * Gets the identifier of the search result item. 
     */
    public String getIdentifier() {
        return this.identifier;
    }
    
    public String getCollectionIdentifier() {
        return this.collectionIdentifier;
    }
    
    /**
     * Gets a local identifier that is fit for display
     * in search results but is not the complete identifier
     * of the item.
     */
    public String getLocalIdentifier() {
        return this.localIdentifier;
    }

    
    /**
     * Gets the URL for the image preview of this item.
     */
    public String getImageUrl() {
        return this.imageUrl;
    }
   
    /**
     * Gets the fields that should be displayed in this
     * search result.
     */
    public List<NameValuePair> getFields() {
        return Collections.unmodifiableList(this.fieldsToDisplay);
    }
}
