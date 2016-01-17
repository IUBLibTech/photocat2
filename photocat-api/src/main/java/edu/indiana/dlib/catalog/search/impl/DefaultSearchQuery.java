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
package edu.indiana.dlib.catalog.search.impl;

import edu.indiana.dlib.catalog.search.SearchQuery;

/**
 * A default implementation of SearchQuery that accepts
 * all variables as parameters to the constructor.
 */
public class DefaultSearchQuery implements SearchQuery {

    private int maxRecords;
    
    private int startingIndex;
    
    private String query;
    
    private String parsedQuery;
    
    private String collectionId;
    
    public DefaultSearchQuery(int startingIndex, int maxRecords, String query, String parsedQuery, String collectionId) {
        this.startingIndex = startingIndex;
        this.maxRecords = maxRecords;
        this.query = query;
        this.parsedQuery = parsedQuery;
        this.collectionId = collectionId;
    }
    
    public int getMaxRecords() {
        return this.maxRecords;
    }

    public String getEnteredQuery() {
        return this.query;
    }
    
    public String getFilterQuery() {
        return this.parsedQuery;
    }

    public int getStartingIndex() {
        return this.startingIndex;
    }
    
    public String getCollectionId() {
        return this.collectionId;
    }

}
