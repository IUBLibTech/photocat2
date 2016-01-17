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

import edu.indiana.dlib.catalog.search.structured.StructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchResults;

/**
 * An interface with methods for every type of searching 
 * or browsing that is available in the cataloging application.
 * All implementations of this interface are expected to ensure
 * reasonable performance for all methods.
 */
public interface SearchManager {
    
    /**
     * Performs a search using the underlying search implementation.
     * @deprecated
     */
    public SearchResults search(SearchQuery query) throws SearchException, UnsupportedQueryException;

    /**
     * Performs a search using the underlying search implementation.
     */
    public StructuredSearchResults search(StructuredSearchQuery query) throws SearchException, UnsupportedQueryException; 
    
    /**
     * Returns a human-readable description of the expected query
     * syntax for this SearchManager implementation.
     */
    public String getSyntaxNotes();
    
    /**
     * Returns a BrowseResults object containing a list of values 
     * with their respective frequency counts.  If possible, implementations
     * should include as the first browse result the number of records that
     * have *NO* values entered.
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
