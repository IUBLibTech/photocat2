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
package edu.indiana.dlib.catalog.publicfacing.config.impl;

import edu.indiana.dlib.catalog.pages.SearchPage;
import edu.indiana.dlib.catalog.search.BrowseQuery;
import edu.indiana.dlib.catalog.search.BrowseResult;
import edu.indiana.dlib.catalog.search.BrowseSet;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.impl.CollectionBrowseSet;
import edu.indiana.dlib.catalog.search.structured.DefaultPagingSpecification;
import edu.indiana.dlib.catalog.search.structured.DefaultStructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.SearchConstraint;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.constraints.CollectionSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.FieldPartValueSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.QueryClauseSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.SerializableSearchConstraint;

public class SearchLinks {

    private String urlRoot;
    
    private StructuredSearchQuery submittedQuery;
    
    private SearchManager sm;
    
    private String scopeId;
    
    public SearchLinks(String urlRoot, StructuredSearchQuery query, String scopeId, SearchManager sm) {
        this.urlRoot = urlRoot;
        submittedQuery = query;
        this.scopeId = scopeId;
        this.sm = sm;
        
        if (submittedQuery == null) {
            submittedQuery = new DefaultStructuredSearchQuery();
            ((DefaultStructuredSearchQuery) submittedQuery).setPagingSpecification(new DefaultPagingSpecification().pageSize(15));
        }
    }
    
    public String getUrlToToggleConstraint(SearchConstraint constraint) {
        if (constraint instanceof SerializableSearchConstraint) {
            StructuredSearchQuery newQuery = SearchPage.toggleConstraint(submittedQuery, (SerializableSearchConstraint) constraint, sm);
            return urlRoot + "?" + SearchPage.getURLQueryFromParameterMap(SearchPage.getParameterMapForQuery(newQuery, sm, scopeId), true);
        } else {
            throw new RuntimeException(constraint.getClass().getName() + " is not serializable!");
        }
    }
    
    public String getUrlToToggleFacet(BrowseQuery query, BrowseResult result) {
        BrowseSet set = query.getBrowseSet();
        if (set instanceof CollectionBrowseSet) {
            String collectionId = result.getFieldValue();
            String collectionDisplayName = result.getFieldDisplayLabel();
            CollectionSearchConstraint constraint = new CollectionSearchConstraint(collectionId, collectionDisplayName);
            StructuredSearchQuery newQuery = SearchPage.toggleConstraint(submittedQuery, constraint, sm);
            return urlRoot + "?" + SearchPage.getURLQueryFromParameterMap(SearchPage.getParameterMapForQuery(newQuery, sm, scopeId), true);
        } else {
            QueryClauseSearchConstraint constraint = new QueryClauseSearchConstraint(set.getDisplayName() + ": " + result.getFieldDisplayLabel(), result.getQuery());
            StructuredSearchQuery newQuery = SearchPage.toggleConstraint(submittedQuery, constraint, sm);
            return urlRoot + "?" + SearchPage.getURLQueryFromParameterMap(SearchPage.getParameterMapForQuery(newQuery, sm, scopeId), true);
        }
    }
    
    public boolean isFacetGroupUsed(BrowseQuery query) {
        BrowseSet set = query.getBrowseSet();
        for (SearchConstraint existingConstraint : submittedQuery.getSearchConstraints()) {
            if (existingConstraint instanceof FieldPartValueSearchConstraint) {
                if (((FieldPartValueSearchConstraint) existingConstraint).getDisplayLabel().equals(set.getDisplayName())) {
                    return true;
                }
            } else if (existingConstraint instanceof CollectionSearchConstraint) {
                if (((CollectionSearchConstraint) existingConstraint).getDisplay().equals(set.getDisplayName())) {
                    return true;
                }
            }
        } 
        return false;
    }
    
    public boolean isFacetSelected(BrowseQuery query, BrowseResult result) {
        BrowseSet set = query.getBrowseSet();
        if (set instanceof CollectionBrowseSet) {
            String collectionId = result.getFieldValue();
            String collectionDisplayName = result.getFieldDisplayLabel();
            CollectionSearchConstraint constraint = new CollectionSearchConstraint(collectionId, collectionDisplayName);
            return SearchPage.isConstraintApplied(submittedQuery, constraint, sm);
        } else {
            QueryClauseSearchConstraint constraint = new QueryClauseSearchConstraint(set.getDisplayName() + ": " + result.getFieldDisplayLabel(), result.getQuery());
            return SearchPage.isConstraintApplied(submittedQuery, constraint, sm);
        }
    }
    
}
