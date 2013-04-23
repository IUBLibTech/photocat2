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
package edu.indiana.dlib.catalog.search.structured;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DefaultStructuredSearchQuery implements StructuredSearchQuery {

    private PagingSpecification pagingSpecs;
    
    private List<SearchConstraint> searchConstraints;
    
    public DefaultStructuredSearchQuery(Collection<SearchConstraint> constraints) {
        pagingSpecs = new DefaultPagingSpecification();
        searchConstraints = new ArrayList<SearchConstraint>(constraints);
    }
    
    public DefaultStructuredSearchQuery(SearchConstraint ... searchConstraint) {
        pagingSpecs = new DefaultPagingSpecification();
        searchConstraints = Arrays.asList(searchConstraint);
    }
    
    public DefaultStructuredSearchQuery() {
        pagingSpecs = new DefaultPagingSpecification();
        searchConstraints = new ArrayList<SearchConstraint>();
    }
    
    public DefaultStructuredSearchQuery(PagingSpecification pspec, SearchConstraint ... searchConstraint) {
        pagingSpecs = pspec;
        searchConstraints = Arrays.asList(searchConstraint);
    }
    
    public DefaultStructuredSearchQuery(PagingSpecification pspec, List<SearchConstraint> constraints) {
        pagingSpecs = pspec;
        searchConstraints = constraints;
    }
    
    public PagingSpecification getPagingSpecification() {
        return pagingSpecs;
    }
    
    public void setPagingSpecification(PagingSpecification pspec) {
        pagingSpecs = pspec;
    }
    
    public List<SearchConstraint> getSearchConstraints() {
        return searchConstraints;
    }
    
    /**
     * DefaultStructuredSearchConstriants are considered equal
     * if they have the same constraints and paging specification.
     * The order of the constraints doesn't matter.
     */
    public boolean equals(Object o) {
        if (o instanceof DefaultStructuredSearchQuery) {
            DefaultStructuredSearchQuery q = (DefaultStructuredSearchQuery) o;
            List<SearchConstraint> c = new ArrayList<SearchConstraint>(q.getSearchConstraints());
            for (SearchConstraint sc : searchConstraints) {
                if (!c.remove(sc)) {
                    // the other object is missing a constraint
                    return false;
                }
            }
            if (!c.isEmpty()) {
                // the other object has an extra constraint
                return false;
            }
            if (pagingSpecs.equals(q.pagingSpecs)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    
    /**
     * Returns the StructuredSearchQuery for the next page or null if the current
     * results includes the last items.
     */
    public static DefaultStructuredSearchQuery nextPageQuery(StructuredSearchResults results) {
        DefaultStructuredSearchQuery query = new DefaultStructuredSearchQuery();
        query.searchConstraints = results.getSearchQuery().getSearchConstraints();
        query.pagingSpecs = DefaultPagingSpecification.getNextPage(results.getSearchQuery().getPagingSpecification());
        if (query.pagingSpecs == null) {
            return null;
        } else if (query.getPagingSpecification().getStartingIndex() >= results.getTotalResultsCount()) {
            // there's no more pages that we know about
            return null;
        } else {
            return query;
        }
    }

}
