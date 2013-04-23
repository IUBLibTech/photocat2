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
package edu.indiana.dlib.catalog.pages;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.click.element.CssImport;
import org.apache.click.element.Element;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.ConfigurationManagerException;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.publicfacing.config.impl.SearchLinks;
import edu.indiana.dlib.catalog.publicfacing.controls.BrowsePanel;
import edu.indiana.dlib.catalog.search.SearchException;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.SearchResultItemSummary;
import edu.indiana.dlib.catalog.search.UnsupportedQueryException;
import edu.indiana.dlib.catalog.search.structured.DefaultPagingSpecification;
import edu.indiana.dlib.catalog.search.structured.DefaultStructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.SearchConstraint;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchResults;
import edu.indiana.dlib.catalog.search.structured.constraints.CollectionSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.OrSearchConstraintGroup;
import edu.indiana.dlib.catalog.search.structured.constraints.SerializableSearchConstraint;

/**
 * Takes a special serialization of a StructuredSearchQuery as its
 * parameters.  There are helper methods to generate this.
 * 
 * If no parameters are specified this page performs a new
 * search with no user-provided constraints (which will 
 * result in all public items).
 *
 * If the provided search constraints match the stored "current"
 * search results, no new search is performed, but the cached
 * results are presented.  To determine the query parameters
 * for the current search results set, the convenient method 
 * getCurrentSearchURLQuery() is implemented.
 */
public class SearchPage extends PublicBorderPage {
    
    private static final String STARTING_INDEX_PARAM_NAME = "start";
    private static final String MAX_RECORDS_PARAM_NAME = "max";
    private static final String CONSTRAINT_PARAM_NAME = "c";
        
    /**
     * The query being searched.  This was built based on the 
     * parameters passsed to this page or loaded as the "current 
     * search".  To build a URL with appropriate parameters, see 
     * the method {@link getParameterMapForQuery()}. 
     */
    private StructuredSearchQuery query;

    public StructuredSearchResults searchResults;
    
    public void onInit() {
        super.onInit();
        // determine the query
        Map<String, String[]> parameterMap = getContext().getRequest().getParameterMap();
        searchResults = getCurrentSearchResults(getContext().getSession());
        StructuredSearchQuery cachedQuery = getCurrentSearchQuery(getContext().getSession());
        query = getQueryFromParameters(parameterMap);
        try {
            if (cachedQuery != null && searchResults != null && getSafeQuery(query, getConfigurationManager(), getSearchManager()).equals(cachedQuery)) {
                // we don't need to perform the search, it's the currently cached search
                LOGGER.debug("Loaded cached search results for query: " + getContext().getRequest().getQueryString());
            } else {
                searchResults = null;
            }
        } catch (ConfigurationManagerException ex) {
            LOGGER.error("Error creating safe query!", ex);
        }
        
        ConfigurationManager cm = getConfigurationManager();
        SearchManager sm = getSearchManager();
        if (cm != null && sm != null) {
            addControl(new BrowsePanel(cm, sm, query, (collection != null ? collection : unit)));
            addModel("links", new SearchLinks("search.htm", query, getScopeId(), sm));
        }
    }
    
    public void onRender() {
        super.onRender();
        SearchManager sm = getSearchManager();
        ItemManager im = getItemManager();
        ConfigurationManager cm = getConfigurationManager();
        try {
            StructuredSearchQuery safeQuery = getSafeQuery(query, cm, sm);

            // fetch and add the search results to the page model
            // (unless they've already been set from the cache)
            if (searchResults == null) {
                searchResults = sm.search(safeQuery);
                storeSearchResults(getContext().getSession(), searchResults, query);
            }

            // fetch and add the item to the page model
            List<SearchResultItemSummary> results = new ArrayList<SearchResultItemSummary>();
            if (searchResults.getResults() != null) {
                for (Item item : searchResults.getResults()) {
                    try {
                        SearchResultItemSummary summary = new SearchResultItemSummary(item, cm.getCollectionConfiguration(item.getCollectionId())); 
                        results.add(summary);
                    } catch (ConfigurationManagerException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            addModel("results", results);
            
            Map<String, String[]> parameters = new HashMap<String, String[]>(getContext().getRequest().getParameterMap());

            // compute and add paging URLs to the page model
            List<String> links = new ArrayList<String>();
            int currentPageIndex = query.getPagingSpecification().getStartingIndex() / query.getPagingSpecification().getMaxRecords(); 
            if (currentPageIndex > 0) {
                links.add("<a href=\"" + getUrlForPage(currentPageIndex, sm, true) + "\">" + getMessage("page-prev") + "</a>");
            }
            for (int i = currentPageIndex - 2; i <= currentPageIndex + 2; i ++) {
                if (i + 1 > searchResults.getTotalPageCount()) {
                    break;
                }
                if (i >= 0) {
                    if (i == currentPageIndex) {
                        links.add(String.valueOf(currentPageIndex + 1));
                    } else {
                        links.add("<a href=\"" + getUrlForPage(i + 1, sm, true) + "\">" + (i + 1) + "</a>");
                    }
                }
            }
            if (currentPageIndex < searchResults.getTotalPageCount() - 1) {
                links.add("<a href=\"" + getUrlForPage(currentPageIndex + 2, sm, true) + "\">" + getMessage("page-next") + "</a>");
            }
            addModel("pageLinks", links);
            
            // compute and add size URls to the page model
            List<String> pageSizes = new ArrayList<String>();
            Map<String, String> pageSizeLinks = new HashMap<String, String>();
            parameters.put(STARTING_INDEX_PARAM_NAME, new String[] { "0" });
            for (int size : new int[] {15, 30, 60}) {
                pageSizes.add(String.valueOf(size));
                parameters.put(MAX_RECORDS_PARAM_NAME, new String[] { String.valueOf(size)});
                pageSizeLinks.put("search.htm?" + String.valueOf(size), getURLQueryFromParameterMap(parameters, true));
            }
            addModel("pageSizes", pageSizes);
            addModel("pageSizeLinks", pageSizeLinks);
            
        } catch (SearchException ex) {
            throw new RuntimeException(ex);
        } catch (UnsupportedQueryException ex) {
            throw new RuntimeException(ex);
        } catch (ConfigurationManagerException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public List<Element> getHeadElements() { 
        // Use lazy loading to ensure the JS is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 

            headElements.add(new CssImport("/css/search.css"));
        } 
        return headElements; 
    } 
    
    private StructuredSearchQuery getSafeQuery(StructuredSearchQuery query, ConfigurationManager cm, SearchManager sm) throws ConfigurationManagerException {
        // create a query that incorporates the scope as well 
        // as any required constraints
        List<SearchConstraint> constraints = new ArrayList<SearchConstraint>();
        if (query != null) {
            constraints.addAll(query.getSearchConstraints());
        }
        if (collection != null) {
            constraints.add(new CollectionSearchConstraint(collection, true));
        } else if (unit != null) {
            List<SerializableSearchConstraint> collectionConstraints = new ArrayList<SerializableSearchConstraint>();
            for (CollectionConfiguration collection : cm.getChildren(unit.getId(), ConfigurationManager.COLLECTION, true)) {
                collectionConstraints.add(new CollectionSearchConstraint(collection.getId()));
            }
            constraints.add(new OrSearchConstraintGroup("unit: " + unit.getCollectionMetadata().getShortName(), collectionConstraints, true));
        }
        constraints.addAll(cm.getPublicRecordsSearchConstraints());
        return (query != null ? new DefaultStructuredSearchQuery(query.getPagingSpecification(), constraints) : new DefaultStructuredSearchQuery(new DefaultPagingSpecification().pageSize(15), constraints));

    }
    
    /**
     * Stores the results of a search to the session.  These may later be retrieved by 
     * invoking getCurrentSearchResults().  This allows for the concept of a current
     * result set that persists between page views.
     */
    private synchronized static void storeSearchResults(HttpSession session, StructuredSearchResults searchResults, StructuredSearchQuery safeQuery) {
        session.setAttribute("saved-structured-search-results", searchResults);
        session.setAttribute("saved-structured-search-query", safeQuery);
    }
    
    /**
     * Gets the search results for the last qualifying search or null if a disqualifying search
     * has been performed since the last qualifying search.  
     */
    public static StructuredSearchResults getCurrentSearchResults(HttpSession session) {
        return (StructuredSearchResults) session.getAttribute("saved-structured-search-results");
    }
    
    public static StructuredSearchQuery getCurrentSearchQuery(HttpSession session) {
        return (StructuredSearchQuery) session.getAttribute("saved-structured-search-query");
    }
    
    /**
     * Gets the parameters that need to be provided to this page to "return" to the current
     * search.  This method may return null if there is no "current" search.
     */
    public static String getCurrentSearchURLQuery(HttpSession session, String scope, SearchManager sm, boolean filter) {
        StructuredSearchQuery query = (StructuredSearchQuery) session.getAttribute("saved-structured-search-query");
        if (query == null) {
            return null;
        } else {
            return getURLQueryFromParameterMap(getParameterMapForQuery(query, sm, scope), filter);
        }
    }
    
    public static StructuredSearchResults getNextPage(HttpSession session, SearchManager sm) {
        StructuredSearchResults currentPage = getCurrentSearchResults(session);
        StructuredSearchQuery query = getCurrentSearchQuery(session);
        if (currentPage == null) {
            return null;
        } else {
            try {
                StructuredSearchQuery nextPageQuery = new DefaultStructuredSearchQuery(DefaultPagingSpecification.getNextPage(currentPage.getSearchQuery().getPagingSpecification()), currentPage.getSearchQuery().getSearchConstraints());
                StructuredSearchResults searchResults = sm.search(nextPageQuery);
                storeSearchResults(session, searchResults, query);
                return searchResults;
            } catch (SearchException ex) {
                return null;
            } catch (UnsupportedQueryException ex) {
                return null;
            }
        }
    }
    
    public static StructuredSearchResults getPreviousPage(HttpSession session, SearchManager sm) {
        StructuredSearchResults currentPage = getCurrentSearchResults(session);
        StructuredSearchQuery query = getCurrentSearchQuery(session);
        if (currentPage == null) {
            return null;
        } else {
            try {
                StructuredSearchQuery previousPageQuery = new DefaultStructuredSearchQuery(DefaultPagingSpecification.getPreviousPage(currentPage.getSearchQuery().getPagingSpecification()), currentPage.getSearchQuery().getSearchConstraints());
                StructuredSearchResults searchResults = sm.search(previousPageQuery);
                storeSearchResults(session, searchResults, query);
                return searchResults;
            } catch (SearchException ex) {
                return null;
            } catch (UnsupportedQueryException ex) {
                return null;
            }
        }
    }

    public static String getURLQueryFromParameterMap(Map<String, String[]> map, boolean filter) {
        StringBuffer sb = new StringBuffer();
        for (String paramName : map.keySet()) {
            for (String value : map.get(paramName)) {
                if (sb.length() > 0) {
                    if (filter) {
                        sb.append("&amp;");
                    } else {
                        sb.append("&");
                    }
                }
                try {
                    sb.append(URLEncoder.encode(paramName, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    sb.append(URLEncoder.encode(paramName) + "=" + URLEncoder.encode(value));
                }
            }
        }
        return sb.toString();
    }
    
    private static StructuredSearchQuery getQueryForOffset(StructuredSearchQuery query, int offset) {
        return new DefaultStructuredSearchQuery(
                new DefaultPagingSpecification().pageSize(query.getPagingSpecification().getMaxRecords()).startingIndex(offset),
                query.getSearchConstraints());
    }
    
    private String getUrlForPage(int pageNumber, SearchManager sm, boolean filter) {
        String urlRoot = "search.htm";
        int offset = query.getPagingSpecification().getMaxRecords() * (pageNumber - 1);
        return urlRoot + "?" + SearchPage.getURLQueryFromParameterMap(SearchPage.getParameterMapForQuery(SearchPage.getQueryForOffset(query, offset), sm, getScopeId()), filter);
    }
    
    public static Map<String, String[]> getParameterMapForQuery(StructuredSearchQuery query, SearchManager sm, String scopeId) {
        Map<String, String[]> map = new HashMap<String, String[]>();
        if (scopeId != null) {
            map.put(SCOPE_PARAM_NAME, new String[] { scopeId });
        }
        if (query.getSearchConstraints() != null) {
            for (SearchConstraint constraint : query.getSearchConstraints()) {
                String[] values = map.get(CONSTRAINT_PARAM_NAME);
                if (map.containsKey(CONSTRAINT_PARAM_NAME)) {
                    // add it
                    String[] newValues = new String[values.length + 1];
                    System.arraycopy(values, 0, newValues, 0, values.length);
                    newValues[values.length] = SerializableSearchConstraint.serialize(constraint, sm);
                    map.put(CONSTRAINT_PARAM_NAME, newValues);
                } else {
                    map.put(CONSTRAINT_PARAM_NAME, new String[] { SerializableSearchConstraint.serialize(constraint, sm)});
                }
            }
        }
        
        map.put(MAX_RECORDS_PARAM_NAME, new String[] { String.valueOf(query.getPagingSpecification().getMaxRecords())});
        map.put(STARTING_INDEX_PARAM_NAME, new String[] { String.valueOf(query.getPagingSpecification().getStartingIndex())});
        return map;
    }
    
    public static boolean isConstraintApplied(StructuredSearchQuery query, SerializableSearchConstraint constraint, SearchManager sm) {
        for (SearchConstraint existingConstraint : query.getSearchConstraints()) {
            if (SerializableSearchConstraint.serialize(existingConstraint, sm).equals(SerializableSearchConstraint.serialize(constraint, sm))) {
                return true;
            }
        }
        return false;
    }
    
    public static StructuredSearchQuery toggleConstraint(StructuredSearchQuery query, SerializableSearchConstraint toggleConstraint, SearchManager sm) {
        int maxRecords = query.getPagingSpecification().getMaxRecords();
        int startingIndex = query.getPagingSpecification().getStartingIndex();
        List<SearchConstraint> constraints = new ArrayList<SearchConstraint>();
        boolean removed = false;
        for (SearchConstraint existingConstraint : query.getSearchConstraints()) {
            if (!SerializableSearchConstraint.serialize(existingConstraint, sm).equals(SerializableSearchConstraint.serialize(toggleConstraint, sm))) {
                constraints.add(existingConstraint);
            } else {
                removed = true;
            }
        }
        if (!removed) {
            constraints.add(toggleConstraint);
        }
        return new DefaultStructuredSearchQuery(new DefaultPagingSpecification().pageSize(maxRecords).startingIndex(0), constraints);
    }

    public static boolean isExplicitSearch(Map<String, String[]> parameterMap) {
        return (parameterMap.containsKey(CONSTRAINT_PARAM_NAME) || parameterMap.containsKey(STARTING_INDEX_PARAM_NAME) || parameterMap.containsKey(MAX_RECORDS_PARAM_NAME));
    }
    
    public static StructuredSearchQuery getQueryFromParameters(Map<String, String[]> parameterMap) {
        int maxrecords = 15;
        int startingIndex = 0;
        List<SearchConstraint> constraints = new ArrayList<SearchConstraint>();
        for (String paramName : parameterMap.keySet()) {
            String[] paramValues = parameterMap.get(paramName);
            if (paramName.equals(MAX_RECORDS_PARAM_NAME)) {
                maxrecords = Integer.parseInt(paramValues[0]);
            } else if (paramName.equals(STARTING_INDEX_PARAM_NAME)) {
                startingIndex = Integer.parseInt(paramValues[0]);
            } else if (paramName.equals(CONSTRAINT_PARAM_NAME)) {
                for (String value : paramValues) {
                    constraints.add(SerializableSearchConstraint.deserialize(value));
                }
            }
        }
        return new DefaultStructuredSearchQuery(new DefaultPagingSpecification().pageSize(maxrecords).startingIndex(startingIndex), constraints);
    }
    
}
