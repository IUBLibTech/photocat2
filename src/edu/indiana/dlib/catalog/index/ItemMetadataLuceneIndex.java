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
package edu.indiana.dlib.catalog.index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

import edu.indiana.dlib.catalog.config.DataFormatException;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemMetadata;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.config.impl.DefaultItemMetadata;
import edu.indiana.dlib.catalog.search.BrowseResult;
import edu.indiana.dlib.catalog.search.SearchQuery;
import edu.indiana.dlib.catalog.search.SearchResults;
import edu.indiana.dlib.catalog.search.impl.DefaultBrowseResult;
import edu.indiana.dlib.catalog.search.impl.DefaultSearchResults;

/**
 * This is a terrible and incomplete Lucene Index implementation
 * that was quickly thrown together to support an embedded index
 * for the purpose of demonstrating this application with
 * minimal setup.  This should never be used in a real system and
 * is likely to be the cause of any related bugs when used in 
 * testing.
 */
public class ItemMetadataLuceneIndex {

    private BasicSupportAnalyzer analyzer;
    
    private Directory niofsDir;
    
    public ItemMetadataLuceneIndex(File indexDirectory) throws CorruptIndexException, LockObtainFailedException, IOException {
        analyzer = new BasicSupportAnalyzer();
        niofsDir = new NIOFSDirectory(indexDirectory);
    }
    
    public SearchResults search(Query luceneQuery, SearchQuery query) throws IOException, DataFormatException {
        IndexSearcher searcher = new IndexSearcher(niofsDir);
        List<ItemMetadata> results = new ArrayList<ItemMetadata>();
        TopDocs hits = searcher.search(luceneQuery, query.getMaxRecords());
        for (int i = query.getStartingIndex(); i < query.getMaxRecords() && i < hits.scoreDocs.length; i ++) {
            Document doc = searcher.doc(hits.scoreDocs[i].doc);
            ByteArrayInputStream is = new ByteArrayInputStream(doc.getFieldable(getItemMetadataFieldName()).stringValue().getBytes("UTF-8"));
            results.add(new DefaultItemMetadata(is));
        }
        return new DefaultSearchResults(query.getStartingIndex(), hits.totalHits, query, results);
    }
    
    /**
     * Searches the search results for the given number of facet/value
     * pairs.
     * @throws IOException 
     */
    public List<BrowseResult> getBrowseResults(QueryParser indexQueryParser, Query query, String fieldName) throws IOException {
        IndexSearcher searcher = new IndexSearcher(niofsDir);
        List<BrowseResult> results = new ArrayList<BrowseResult>();
        CachingWrapperFilter original = new CachingWrapperFilter(new QueryWrapperFilter(query));
        List<String> values = getSortedFieldsForFacet(getFieldNameFacet(fieldName));
        if (!values.isEmpty()) {
            for (int i = 0; i < values.size(); i ++) {
                String facetValue = values.get(i);
                int hits = searcher.search(new TermQuery(new Term(getFieldNameFacet(fieldName), facetValue)), original, 1).totalHits;
                if (hits > 0) {
                    results.add(new DefaultBrowseResult(facetValue, hits, getFieldNameFacet(fieldName) + ":\"" + facetValue + "\""));
                }
            }
        }
        return results;
    }
    
    /**
     * Iterates over the index to find all possible values for the 
     * given field name.  This method is relatively slow and the 
     * result of which should be cached and reused when possible.
     * This method does not check to see if any of the indices backed
     * by the IndexReaders have changed on disk.  Such checks and the
     * subsequent refreshes should be performed by the caller if an
     * up-to-date value is required.
     * @param fieldName the exact-match field name for the field whose 
     *    represented values are being queried
     */
    private List<String> getSortedFieldsForFacet(String fieldName) throws IOException {
        IndexSearcher searcher = new IndexSearcher(niofsDir);
        IndexReader reader = searcher.getIndexReader();
        Set<String> facetValues = new HashSet<String>();
        TermEnum termEnum = reader.terms(new Term(fieldName, ""));
        do {
            Term term = termEnum.term();
            if (term.field().equals(fieldName)) {
                facetValues.add(term.text());
            } else {
                break;
            }
        } while (termEnum.next());
        ArrayList<String> sortedValues = new ArrayList<String>(facetValues);
        Collections.sort(sortedValues);
        return sortedValues;
    }

    
    public BasicSupportAnalyzer getAnalyzer() {
        return analyzer;
    }

    public void clearIndex() throws IOException {
        clearIndex(niofsDir, analyzer);
    }
    
    public static synchronized void clearIndex(Directory indexDirectory, Analyzer analyzer) throws IOException {
        IndexWriter writer = new IndexWriter(indexDirectory, new IndexWriterConfig(Version.LUCENE_32, analyzer));
        try {
            writer.deleteAll();
            writer.commit();
        } finally {
            writer.close();
        }
        
    }
    
    public void indexItem(Item item) throws ParserConfigurationException, IOException, TransformerException {
        Document indexDoc = new Document();
        
        // add item Id
        addKeywordField(getIdFieldName(), item.getId(), indexDoc);
        addKeywordField(getEverythingFieldName(), item.getId(), indexDoc);
        
        // add local item Id
        addKeywordField(getLocalIdFieldName(), item.getIdWithinCollection(), indexDoc);
        addKeywordField(getEverythingFieldName(), item.getIdWithinCollection(), indexDoc);
        
        // add collection Id
        addKeywordField(getCollectionIdFieldName(), item.getCollectionId(), indexDoc);
        addKeywordField(getEverythingFieldName(), item.getCollectionId(), indexDoc);
        
        ItemMetadata im = item.getMetadata();
        
        for (String fieldType : im.getRepresentedFieldTypes()) {
            FieldData data = im.getFieldData(fieldType);
            for (NameValuePair attribute : data.getAttributes()) {
                addKeywordField(getAttributeFieldName(fieldType, attribute.getName()), attribute.getValue(), indexDoc);
                addKeywordField(getEverythingFieldName(), attribute.getValue(), indexDoc);
            }
            for (List<NameValuePair> values : data.getParts()) {
                for (NameValuePair part : values) {
                    addKeywordField(getPartFieldName(fieldType, part.getName()), part.getValue(), indexDoc);
                    addKeywordField(getEverythingFieldName(), part.getValue(), indexDoc);
                }
            }
        }
        
        // add whole serialized item metadata record
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        im.writeOutXML(baos);
        addRecordField(getItemMetadataFieldName(), baos.toString("UTF-8"), indexDoc);
        
        addDocument(niofsDir, analyzer, new Term(getIdFieldName(), im.getId()), indexDoc);
    }
    
    public static synchronized void addDocument(Directory indexDirectory, Analyzer analyzer, Term termToDelete, Document doc) throws IOException {
        IndexWriter writer = new IndexWriter(indexDirectory, new IndexWriterConfig(Version.LUCENE_32, analyzer));
        try {
            writer.deleteDocuments(termToDelete);
            writer.addDocument(doc);
            writer.commit();
        } finally {
            writer.close();
        }
    }
    
    public String getAttributeFieldName(String fieldType, String attributeName) {
        return fieldType + "-attribute-" + attributeName;
    }
    
    public String getPartFieldName(String fieldType, String partName) {
        return fieldType + "-part-" + partName;
    }
    
    public String getItemMetadataFieldName() {
        return "metadata";
    }
    
    public String getEverythingFieldName() {
        return "keyword";
    }

    public String getIdFieldName() {
        return "id";
    }
    
    public String getLocalIdFieldName() {
        return "localId";
    }
    
    public String getCollectionIdFieldName() {
        return "collectionId";
    }
    
    public String getFieldNameExact(String fieldName) {
        return fieldName + analyzer.getExactSuffix();
    }
    
    public String getFieldNameFacet(String fieldName) {
        return fieldName + analyzer.getFacetSuffix();
    }
    
    private void addKeywordField(String name, String value, Document doc) {
        doc.add(new Field(name, value, Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field(name + analyzer.getExactSuffix(), value, Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field(name + analyzer.getFacetSuffix(), value, Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field(name + analyzer.getStemSuffix(), value, Field.Store.NO, Field.Index.ANALYZED));
    }
    
    private void addRecordField(String name, String value, Document doc) {
        doc.add(new Field(name, value, Field.Store.YES, Field.Index.NOT_ANALYZED));
    }
    
}
