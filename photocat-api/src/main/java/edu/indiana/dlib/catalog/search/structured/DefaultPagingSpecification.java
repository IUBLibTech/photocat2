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

import edu.indiana.dlib.catalog.search.SearchResults;

public class DefaultPagingSpecification implements PagingSpecification {

    private int max;
    
    private int startIndex;
    
    public DefaultPagingSpecification() {
        max = 20;
        startIndex = 0;
    }
    
    public DefaultPagingSpecification pageSize(int pageSize) {
        max = pageSize;
        return this;
    }
    
    public int getMaxRecords() {
        return max;
    }

    public DefaultPagingSpecification startingIndex(int start) {
        startIndex = start;
        return this;
    }
    
    public int getStartingIndex() {
        return startIndex;
    }
    
    public boolean equals(Object o) {
        if (o instanceof DefaultPagingSpecification) {
            DefaultPagingSpecification other = (DefaultPagingSpecification) o;
            return other.max == max && other.startIndex == startIndex;
        } else {
            return false;
        }
    }
    
    public int hashCode() {
        return max * startIndex;
    }
    
    public DefaultPagingSpecification getNextPage(SearchResults results) {
        if (results.getResults().size() + results.getStartingIndex() >= results.getTotalResultCount()) {
            return null;
        } else {
            return new DefaultPagingSpecification().pageSize(max).startingIndex(results.getStartingIndex() + results.getResults().size());
        }
    }
    
    public static DefaultPagingSpecification getNextPage(PagingSpecification pagingSpec) {
        return new DefaultPagingSpecification().pageSize(pagingSpec.getMaxRecords()).startingIndex(pagingSpec.getStartingIndex() + pagingSpec.getMaxRecords());
    }
    
    public static DefaultPagingSpecification getPreviousPage(PagingSpecification pagingSpec) {
        if (pagingSpec.getStartingIndex() == 0) {
            return null;
        } else {
            return new DefaultPagingSpecification().pageSize(pagingSpec.getMaxRecords()).startingIndex(Math.max(0, pagingSpec.getStartingIndex() - pagingSpec.getMaxRecords()));
        }
    }

}
