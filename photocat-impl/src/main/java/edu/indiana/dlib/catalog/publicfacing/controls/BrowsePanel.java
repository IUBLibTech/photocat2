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
package edu.indiana.dlib.catalog.publicfacing.controls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.click.control.Panel;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;
import org.apache.click.element.JsImport;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.ConfigurationManagerException;
import edu.indiana.dlib.catalog.publicfacing.config.impl.SearchLinks;
import edu.indiana.dlib.catalog.search.BrowseQuery;
import edu.indiana.dlib.catalog.search.BrowseResult;
import edu.indiana.dlib.catalog.search.BrowseResults;
import edu.indiana.dlib.catalog.search.BrowseSet;
import edu.indiana.dlib.catalog.search.SearchException;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.UnsupportedQueryException;
import edu.indiana.dlib.catalog.search.impl.CollectionBrowseSet;
import edu.indiana.dlib.catalog.search.impl.DefaultBrowseQuery;
import edu.indiana.dlib.catalog.search.impl.DefaultBrowseResult;
import edu.indiana.dlib.catalog.search.impl.DefaultBrowseResults;
import edu.indiana.dlib.catalog.search.structured.DefaultPagingSpecification;
import edu.indiana.dlib.catalog.search.structured.SearchConstraint;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.constraints.CollectionSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.OrSearchConstraintGroup;
import edu.indiana.dlib.catalog.search.structured.constraints.SerializableSearchConstraint;

/**
 * Presents a list of categories by which a user may browse.
 * Use cases:
 *   Browse/Refine all items
 *   Browse/Refine all items in a unit
 *   Browse/Refine all items in a collection
 *   
 *   There are some grand visions for this panel that will have
 *   to be implemented over time in the following blocks.
 *   <ul>
 *     <li>Support for hierarchical unit/collection display</li>
 *     <li>Support for hierarchical facets based on vocabulary structure</li>
 *   </ul>
 *   
 *   The following variables are (sometimes) made available to the panel:
 *   <ul>
 *     <li>
 *       originalSearch - the original search if this panel is to represent
 *       refinements
 *     </li>
 *     <li>
 *       scope - the short name of the collection or unit to which the
 *       scope has been limited
 *     </li>
 *     <li>
 *     </li>
 *   </ul>
 */
public class BrowsePanel extends Panel {

    private ConfigurationManager cm;
    
    private SearchManager sm;
    
    private List<BrowseResults> facets;
    
    private CollectionConfiguration scope;
    
    private StructuredSearchQuery originalSearchQuery;
    
    private boolean isGlobalSplashPage;
    
    /**
     * When the scope is global (not set to a unit or collection), this map
     * contains the number of hits for each scope.
     */
    public Map<CollectionConfiguration, Integer> scopeCountMap;
    
    public BrowsePanel(ConfigurationManager cm, SearchManager searchMan) {
        this(cm, searchMan, null, null);
    }
    
    public BrowsePanel(ConfigurationManager cm, SearchManager searchMan, StructuredSearchQuery originalSearch, CollectionConfiguration scope) {
        super("browsePanel", "browse-panel.htm");
        
        sm = searchMan;
        this.cm = cm;
        this.scope = scope;
        
        if (scope != null) {
            getModel().put("scope", scope.getCollectionMetadata().getShortName());
        } else if (originalSearch == null) {
            // global scope
            isGlobalSplashPage = true;
            try {
                BrowseResults results = doBrowse(new CollectionBrowseSet("collection"));
                scopeCountMap = new HashMap<CollectionConfiguration, Integer>();
                for (CollectionConfiguration u: cm.getCollectionConfigurations(ConfigurationManager.UNIT, true)) {
                    int unitTotal = 0;
                    for (CollectionConfiguration c : cm.getChildren(u.getId(), ConfigurationManager.COLLECTION, true)) {
                        if (c.isPublic()) {
                            for (BrowseResult result : results.listBrowseResults()) {
                                if (result.getFieldValue().equals(c.getId())) {
                                    scopeCountMap.put(c, result.getHitCount());
                                    unitTotal += result.getHitCount();
                                }
                            }
                        }
                    }
                    scopeCountMap.put(u, unitTotal);
                }
                for (CollectionConfiguration c : cm.getOrphans(ConfigurationManager.COLLECTION, true)) {
                    for (BrowseResult result : results.listBrowseResults()) {
                        if (result.getFieldValue().equals(c.getId())) {
                            if (result.getHitCount() > 0) {
                                scopeCountMap.put(c, result.getHitCount());
                            }
                        }
                    }
                }
                getModel().put("scopeCountMap", scopeCountMap);
                getModel().put("cm", cm);
            } catch (Throwable t) {
                t.printStackTrace();
                // fall through, this feature will be disabled
            }
        }
        originalSearchQuery = originalSearch;
        getModel().put("originalSearch", originalSearch);
    }
    
    public void onRender() {
        List<BrowseSet> facetsToExpose = null;
        if (scope == null) {
            facetsToExpose = cm.getGlobalFacets();
        } else {
            facetsToExpose = new ArrayList<BrowseSet>(scope.getCollectionMetadata().getFacets());
        }
        
        facets = new ArrayList<BrowseResults>();
        addModel("facets", facets);

        Map<BrowseResults, List<BrowseResult>> facetSummaries = new HashMap<BrowseResults, List<BrowseResult>>();
        addModel("facetSummaries", facetSummaries);
        
        Map<BrowseResults, String> facetNames = new HashMap<BrowseResults, String>();
        addModel("facetNames", facetNames);
        
        Map<BrowseResults, String> facetIds = new HashMap<BrowseResults, String>();
        addModel("facetIds", facetIds);
        
        addModel("links", new SearchLinks("search.htm", originalSearchQuery, scope == null ? null : scope.getId(), sm));
        
        if (facetsToExpose != null) {
            for (BrowseSet browseSet : facetsToExpose) {
                if (!(isGlobalSplashPage && browseSet instanceof CollectionBrowseSet)) {
                    try {
                        BrowseResults results = doBrowse(browseSet);

                        /* THIS is a hack to insert collection names into the facet */
                        if (results.getBrowseQuery().getBrowseSet() instanceof CollectionBrowseSet) {
                            List<BrowseResult> remappedResults = new ArrayList<BrowseResult>();
                            for (BrowseResult result : results.listBrowseResults()) {
                                remappedResults.add(new DefaultBrowseResult(result.getFieldValue(), cm.getCollectionConfiguration(result.getFieldValue()).getCollectionMetadata().getShortName(), result.getHitCount(), result.getQuery()));
                            }
                            results = new DefaultBrowseResults(results.getBrowseQuery(), remappedResults);
                        }
                        
                        facets.add(results);
                        facetSummaries.put(results, getBrowseSummary(results.listBrowseResults(), 10));
                        facetNames.put(results, browseSet.getDisplayName());
                        facetIds.put(results, browseSet.getDisplayName().replace(" ", "-"));
                    } catch (SearchException ex) {
                        throw new RuntimeException(ex);
                    } catch (UnsupportedQueryException ex) {
                        throw new RuntimeException(ex);
                    } catch (ConfigurationManagerException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }
    
    private BrowseResults doBrowse(BrowseSet set) throws SearchException, UnsupportedQueryException, ConfigurationManagerException {
        // These constraints (used to generate the facet lists) must contain:
        // 1.  All of the constraints associated with the user query
        // 2.  All of the constraints associated with the page scope
        // 3.  All of the constraints required of any search !!! SECURITY IMPLICATIONS
        List<SearchConstraint> constraintsForFacets = new ArrayList<SearchConstraint>();
        if (originalSearchQuery != null) {
            constraintsForFacets.addAll(originalSearchQuery.getSearchConstraints());
        }
        if (scope == null) {
            // no further constraint
        } else if (scope.getCollectionMetadata().isCollection()) {
            constraintsForFacets.add(new CollectionSearchConstraint(scope, true));
        } else if (scope.getCollectionMetadata().isUnit()) {
            List<SerializableSearchConstraint> collectionConstraints = new ArrayList<SerializableSearchConstraint>();
            for (CollectionConfiguration collection : cm.getChildren(scope.getId(), ConfigurationManager.COLLECTION, true)) {
                collectionConstraints.add(new CollectionSearchConstraint(collection.getId()));
            }
            constraintsForFacets.add(new OrSearchConstraintGroup("unit: " + scope.getCollectionMetadata().getShortName(), collectionConstraints, true));
        }
        constraintsForFacets.addAll(cm.getPublicRecordsSearchConstraints());
        
        BrowseQuery safeQuery = new DefaultBrowseQuery(set, constraintsForFacets, new DefaultPagingSpecification().startingIndex(0).pageSize(1000));
        //BrowseQuery safeQuery = new FacetBrowseQuery(facet, new ArrayList<SearchConstraint>(constraintsForFacets), 0, 1000);
        BrowseResults results = sm.browse(safeQuery);
        return results;
    }
    
    public List<Element> getHeadElements() { 
        // Use lazy loading to ensure the JS is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 

            // Include the control's external Css resource 
            headElements.add(new CssImport("/css/browse-panel.css"));
            headElements.add(new JsImport("/js/jquery/jquery-1.7.1.min.js"));
            headElements.add(new JsImport("/js/browse-panel.js"));
        } 
        return headElements; 
    } 
    
    public List<BrowseResult> getBrowseSummary(List<BrowseResult> fullResults, int summarySize) {
        // first check if results are hierarchical
        for (BrowseResult result : fullResults) {
            if (result.listBrowseResults() != null) {
                return Collections.emptyList();
            }
        }
        
        // return (without a summary) if the summary 
        // would be just as long as the full set
        if (summarySize >= fullResults.size()) {
            return Collections.emptyList();
        }
        List<BrowseResult> summary = new ArrayList<BrowseResult>(fullResults);
        Collections.sort(summary, new Comparator<BrowseResult>() {
            public int compare(BrowseResult o1, BrowseResult o2) {
                return o2.getHitCount() - o1.getHitCount();
            }});
        return summary.subList(0, summarySize);
    }
    
}
