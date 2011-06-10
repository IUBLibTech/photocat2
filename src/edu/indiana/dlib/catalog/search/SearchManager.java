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
package edu.indiana.dlib.catalog.search;

/**
 * An interface with methods for every type of searching 
 * or browsing that is available in the cataloging application.
 * All implementations of this interface are expected to ensure
 * reasonable performance for all methods.
 */
public interface SearchManager {
    
    /**
     * Performs a search using the underlying search implementation.
     */
    public SearchResults search(SearchQuery query) throws SearchException, UnsupportedQueryException;

    /**
     * Returns a human-readable description of the expected query
     * syntax for this SearchManager implementation.
     */
    public String getSyntaxNotes();
    
    /**
     * Returns a BrowseResults object containing a list of values 
     * with their respective frequency counts.
     */
    public BrowseResults browse(BrowseQuery browseQuery) throws SearchException, UnsupportedQueryException;
    
    /**
     * Determines the index name needed to search the underlying
     * search implementation for the given attribute on the given
     * field.
     */
    public String getFieldAttributeIndexName(String fieldType, String attributeName);
    
    /**
     * Determines the index name needed to search the underlying
     * search implementation for the given part of the given
     * field.
     */
    public String getFieldPartIndexName(String fieldType, String partName);

    public String getPartExactMatchQueryClause(String fieldType, String partName, String value);
    
    public String getAttributeExactMatchQueryClause(String fieldType, String attributeName, String value);
}
