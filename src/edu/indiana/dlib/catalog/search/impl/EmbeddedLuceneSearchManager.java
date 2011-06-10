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
package edu.indiana.dlib.catalog.search.impl;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.util.Version;

import edu.indiana.dlib.catalog.config.DataFormatException;
import edu.indiana.dlib.catalog.index.ItemMetadataLuceneIndex;
import edu.indiana.dlib.catalog.search.BrowseQuery;
import edu.indiana.dlib.catalog.search.BrowseResults;
import edu.indiana.dlib.catalog.search.SearchException;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.SearchQuery;
import edu.indiana.dlib.catalog.search.SearchResults;
import edu.indiana.dlib.catalog.search.UnsupportedQueryException;

/**
 * This is a terrible and incomplete Lucene SearchManager implementation
 * that was quickly thrown together to support an embedded index
 * for the purpose of demonstrating this application with
 * minimal setup.  This should never be used in a real system and
 * is likely to be the cause of any related bugs when used in 
 * testing.
 */
public class EmbeddedLuceneSearchManager implements SearchManager {

    private ItemMetadataLuceneIndex index;
    
    private QueryParser parser;
    
    public EmbeddedLuceneSearchManager(String directory) {
        try {
            String photocatHome = System.getenv("PHOTOCAT_HOME");
            if (photocatHome != null && !directory.startsWith("/")) {
                File homeDir = new File(photocatHome);
                index = new ItemMetadataLuceneIndex(new File(homeDir, directory));
            } else {
                index = new ItemMetadataLuceneIndex(new File(directory));
            }
            
            parser = new QueryParser(Version.LUCENE_32, index.getEverythingFieldName(), index.getAnalyzer());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public SearchResults search(SearchQuery query) throws SearchException, UnsupportedQueryException {
        String fullQuery = "+" + index.getCollectionIdFieldName() + ":\"" + query.getCollectionId() + "\"" + (query.getEnteredQuery() != null && query.getEnteredQuery().length() > 0 ? " AND (" + translateUserQuery(query.getEnteredQuery()) + ")" : "") + (query.getFilterQuery() != null ? " AND (" + query.getFilterQuery() + ")" : "");
        try {
            return index.search(parser.parse(fullQuery), query);
        } catch (ParseException ex) {
            throw new UnsupportedQueryException(ex);
        } catch (IOException ex) {
            throw new SearchException(ex);
        } catch (DataFormatException ex) {
            throw new SearchException(ex);
        }
    }
    
    private String translateUserQuery(String userQuery) {
        return userQuery;
    }

    public String getSyntaxNotes() {
        return "Lucene QueryParser syntax.";
    }

    public BrowseResults browse(BrowseQuery browseQuery) throws SearchException, UnsupportedQueryException {
        try {
            return new DefaultBrowseResults(browseQuery, index.getBrowseResults(parser, new MatchAllDocsQuery(), index.getPartFieldName(browseQuery.getFieldType(), browseQuery.getPartName())));
        } catch (IOException ex) {
            throw new SearchException(ex);
        }
    }

    public String getFieldAttributeIndexName(String fieldType, String attributeName) {
        return index.getAttributeFieldName(fieldType, attributeName);
    }

    public String getFieldPartIndexName(String fieldType, String partName) {
        return index.getPartFieldName(fieldType, partName);
    }

    public String getPartExactMatchQueryClause(String fieldType, String partName, String value) {
        return index.getFieldNameExact(getFieldPartIndexName(fieldType, partName)) + ":\"" + value + "\"";
    }

    public String getAttributeExactMatchQueryClause(String fieldType, String attributeName, String value) {
        return index.getFieldNameExact(getFieldPartIndexName(fieldType, attributeName)) + ":\"" + value + "\"";
    }
}
