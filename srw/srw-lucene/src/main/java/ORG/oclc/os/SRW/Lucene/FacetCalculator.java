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
package ORG.oclc.os.SRW.Lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;

import ORG.oclc.os.SRW.shared.FacetExtensionHandler;

import edu.indiana.dlib.search.facets.DefaultSearchFacet;
import edu.indiana.dlib.search.facets.DefaultSearchFacetValue;
import edu.indiana.dlib.search.facets.FacetRequestInfo;
import edu.indiana.dlib.search.facets.SearchFacet;
import edu.indiana.dlib.search.facets.SearchFacetValue;
import edu.indiana.dlib.search.indexing.FieldConfiguration;

/**
 * <p>
 *   A utility class with static methods to calculate facets or 
 *   field/value pairs (with hit counts) that may be used to 
 *   further restrict a search.  The method {@link
 *   #getFacets(QueryParser, IndexReader, Query, Hits, Collection,
 *   Searcher)} should be invoked and will delegate to 
 *   whichever method it deems to be the most efficient at 
 *   calculating the facet information.  In the current 
 *   implementation, two such methods exist.
 * </p>
 * <p>
 *   The first implementation gleans the facets by traversing
 *   the full result set.  It requires that the facet field that
 *   corresponds to every specified base field name be stored in
 *   the index without tokenization.  ({@link FieldConfiguration}
 *   is used to determine the "facet field")  The performance of 
 *   this algorithm is based on the product of the number of 
 *   results and the number of facet categories.  Since the time 
 *   and memory usage required to iterate over long result sets 
 *   in lucene scales poorly, an arbitrary limit is set ({@link 
 *   #MAXIMUM_HITS_TO_TRAVERSE}) after which this algorithm is not 
 *   used.  {@see #getFacetsByHitTraversal(Hits, Collection)}
 * </p>
 * <p>
 *   The second implementation requires that all possible values for
 *   the facet fields be determined (see {@link #getSortedFieldsForFacet(
 *   String, FieldConfiguration, List)}) and will perform a search for 
 *   the current criteria AND a match against each of these fields.  The
 *   performance of this algorithm is based on the product of the number
 *   of facet field/value pairs.  {@see #getFacetsBySearching(QueryParser,
 *   IndexReader, Query, Collection, Searcher)}
 * </p>
 */
public class FacetCalculator {
    
    private static Log LOG = LogFactory.getLog(FacetCalculator.class);
    
    private static Log PERFORMANCE_LOG = LogFactory.getLog("performance." + FacetCalculator.class.getName());

    /**
     * The maximum number of hits allowed for use of the 
     * hit-traversal algorithm.  If more hits exist, the
     * {@link #getFacets()} method will use the alternative
     * algorithm.  This should be set to the value at which
     * both algorithms take approximately the same amount 
     * of time for typical input. 
     */
    private static final int MAXIMUM_HITS_TO_TRAVERSE = 500;
    
    public static List<SearchFacet> getFacets(QueryParser indexQueryParser, Query query, Hits hits, List<FacetRequestInfo> requestedFacets, Map<String, List<String>> facetToValueMap, FieldConfiguration fc, Searcher searcher) {
        if (hits.length() > MAXIMUM_HITS_TO_TRAVERSE) {
            return getFacetsBySearching(indexQueryParser, query, requestedFacets, facetToValueMap, fc, searcher);
        } else { 
            return getFacetsByHitTraversal(hits, requestedFacets, fc);
        }
    }

    /**
     * Traverses the hits, and for each hit, searches for all fields
     * with one of the provided names.  For each matching field name,
     * the hit count is updated for the field's value.  If a field is
     * listed more than once in the same hit with the same value, the
     * count is only increased by once since it represents unique 
     * documents (hits) rather than unique instances of the term.
     */
    private static List<SearchFacet> getFacetsByHitTraversal(Hits hits, List<FacetRequestInfo> requestedFacets, FieldConfiguration fc) {
        
        Map<String, Map<String, Integer>> fieldToValueToCountMap = new HashMap<String, Map<String, Integer>>();
        
        long start = System.currentTimeMillis();
        for (FacetRequestInfo fri : requestedFacets) {
            String fieldName = fri.facetFieldName;
            Map<String, Integer> valueToCountMap = new HashMap<String, Integer>();
            fieldToValueToCountMap.put(fc.getFieldNameFacet(fieldName), valueToCountMap);
        }
        for (int i  = 0; i < hits.length() && i < MAXIMUM_HITS_TO_TRAVERSE; i ++) {
            try {
                HashSet<String> keys = new HashSet<String>();
                Document hit = hits.doc(i);
                for (FacetRequestInfo fri : requestedFacets) {
                    String fieldName = fri.facetFieldName;
                    Map<String, Integer> valueToCountMap = fieldToValueToCountMap.get(fc.getFieldNameFacet(fieldName));
                
                    Field fields[] = hit.getFields(fc.getFieldNameFacet(fieldName));
                    if (fields != null) {
                        for (Field field : fields) {
                            /**
                             * Sometimes a field will be listed more than once
                             * for the same document.  In such cases we need
                             * to only up the hit count once, since the hits is
                             * based on documents.
                             */
                            if (!keys.contains(field.stringValue())) {
                                if (valueToCountMap.containsKey(field.stringValue())) {
                                    valueToCountMap.put(field.stringValue(), valueToCountMap.get(field.stringValue()) + 1);
                                } else {
                                    valueToCountMap.put(field.stringValue(), 1);
                                }
                                keys.add(field.stringValue());
                            }
                        }
                    }
                }
            } catch (CorruptIndexException ex) {
                LOG.error("Error calculating facets!", ex);
            } catch (IOException ex) {
                LOG.error("Error calculating facets!", ex);
            }
        }
        
        List<SearchFacet> facets = new ArrayList<SearchFacet>();
        
        for (FacetRequestInfo fri : requestedFacets) {
            String fieldName = fri.facetFieldName;
            Map<String, Integer> valueToCountMap = fieldToValueToCountMap.get(fc.getFieldNameFacet(fieldName));
            ArrayList<SearchFacetValue> facetValues = new ArrayList<SearchFacetValue>();
            int maxFacetIndex = valueToCountMap.size();
            if (fri.facetRequestCount != FacetRequestInfo.UNLIMITED) {
                maxFacetIndex = fri.facetRequestCount + fri.facetOffset;
            }
            ArrayList<String> values = new ArrayList<String>(valueToCountMap.keySet());
            Collections.sort(values);
            for (int i = fri.facetOffset; i < values.size() && i < maxFacetIndex; i ++) {
                facetValues.add(new DefaultSearchFacetValue(values.get(i), values.get(i), valueToCountMap.get(values.get(i))));
            }
            facets.add(new DefaultSearchFacet(fieldName, fieldName, facetValues));
        }

        long end = System.currentTimeMillis();
        PERFORMANCE_LOG.info("Calculated facet information for " + requestedFacets.size() + " categories by traversing " + hits.length() + " results in " + (end - start) + "ms.");
        return facets;
    }

    /**
     * Searches the search results for the given number of facet/value
     * pairs.
     */
    private static List<SearchFacet> getFacetsBySearching(QueryParser indexQueryParser, Query query, List<FacetRequestInfo> requestedFacets, Map<String, List<String>> fieldToValueMap, FieldConfiguration fc, Searcher searcher) {
        long start = System.currentTimeMillis();
        List<SearchFacet> facets = new ArrayList<SearchFacet>();
        int searchCount = 0;
        try {
            CachingWrapperFilter original = new CachingWrapperFilter(new QueryWrapperFilter(query));
            for (FacetRequestInfo fri : requestedFacets) {
                String fieldBaseName = fri.facetFieldName;
                List<String> values = fieldToValueMap.get(fieldBaseName);
                if (values == null) {
                    LOG.error("No fields were specified for facet \"" + fieldBaseName + "\"!");
                } else {
                    if (!values.isEmpty()) {
                        DefaultSearchFacet facet = new DefaultSearchFacet(fieldBaseName, fieldBaseName, new ArrayList<SearchFacetValue>());
                        facets.add(facet);
                        int max = fri.facetRequestCount;
                        for (int i = fri.facetOffset; i < values.size() && (max == FacetRequestInfo.UNLIMITED || facet.getRankedFacetValues().size() < max); i ++) {
                            String facetValue = values.get(i);
                            Hits facetHits = searcher.search(new TermQuery(new Term(fc.getFieldNameFacet(fieldBaseName), facetValue)), original);
                            searchCount ++;
                            if (facetHits.length() > 0) {
                                facet.getRankedFacetValues().add(new DefaultSearchFacetValue(facetValue, facetValue, facetHits.length()));
                            }
                        }
                    }
                }
            }
        } catch (CorruptIndexException ex) {
            LOG.error("Unable to generate facet information!", ex);
        } catch (IOException ex) {
            LOG.error("Unable to generate facet information!", ex);
        }
        long end = System.currentTimeMillis();
        PERFORMANCE_LOG.info("Calculated facet information for " + requestedFacets.size() + " categories by performing " + searchCount + " additional searches in " + (end - start) + "ms.");
        return facets;
    }
    
    /**
     * Iterates over the index to find all possible values for the 
     * given field name.  This method is relatively slow and the 
     * result of which should be cached and reused when possible.
     * This method does not check to see if any of the indices backed
     * by the IndexReaders have changed on disk.  Such checks and the
     * subsequent refreshes should be performed by the caller if an
     * up-to-date value is required.
     */
    public static List<String> getSortedFieldsForFacet(String fieldNameBase, FieldConfiguration fc, List<IndexReader> indexReaders) throws IOException {
        Set<String> facetValues = new HashSet<String>();
        long start = System.currentTimeMillis();
        for (IndexReader reader : indexReaders) {
            TermEnum termEnum = reader.terms(new Term(fc.getFieldNameFacet(fieldNameBase), ""));
            do {
                Term term = termEnum.term();
                if (term.field().equals(fc.getFieldNameFacet(fieldNameBase))) {
                    facetValues.add(term.text());
                } else {
                    break;
                }
            } while (termEnum.next());
        }
        ArrayList<String> sortedValues = new ArrayList<String>(facetValues);
        Collections.sort(sortedValues);
        long end = System.currentTimeMillis();
        PERFORMANCE_LOG.debug("Calculated and sorted " + sortedValues.size() + " facet field values for \"" + fc.getFieldNameFacet(fieldNameBase) + "\" in " + (end - start) + "ms.");
        return sortedValues;
    }

}
