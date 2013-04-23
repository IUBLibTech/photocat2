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
package edu.indiana.dlib.catalog.pages.collections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.click.control.Checkbox;
import org.apache.click.control.Form;
import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;
import org.apache.click.element.JsImport;

import edu.indiana.dlib.catalog.asynchronous.UserOperationManager;
import edu.indiana.dlib.catalog.batch.Batch;
import edu.indiana.dlib.catalog.batch.BatchManager;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.search.SearchException;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.SearchQuery;
import edu.indiana.dlib.catalog.search.SearchResultItemSummary;
import edu.indiana.dlib.catalog.search.SearchResults;
import edu.indiana.dlib.catalog.search.UnsupportedQueryException;
import edu.indiana.dlib.catalog.search.impl.DefaultSearchQuery;
import edu.indiana.dlib.catalog.search.impl.SaveSearchOperation;
import edu.indiana.dlib.catalog.search.impl.click.control.FilterQueryField;

/**
 * <p>
 *   A page that presents a search form and processes search requests.
 *   <br />
 *   This following use cases must be supported:
 *   <ol>
 *     <li>
 *       A user goes to the page with no query and is presented with
 *       all the search results in the collection as well as an empty search
 *       form
 *     </li>
 *     <li>
 *       A user is directed to the page with a filter (from the browse page)
 *       and is presented with that filter, the empty search form and all
 *       the results that match that filter
 *     </li>
 *     <li>
 *       A user goes to the page (likely bookmarked) with a full suite of
 *       search parameters and is presented with the search results appropriate
 *       for those parameters
 *     </li>
 *   </ol>
 * </p>
 */
public class SearchPage extends CollectionPage {

    public Form searchForm;

    /**
     * The search results, fetched as part of processing
     * or loaded from the session (for POST operations).
     */
    private SearchResults searchResults;
    
    private TextField queryInput;
    
    private FilterQueryField filterField;
    
    private Select pageSize;
    
    private Submit first;
    private Submit prev;
    private Submit next;
    private Submit last;

    private Submit save;
    
    /**
     * A map from item identifiers to a checkbox
     * to select that item.
     */
    public Map<String, Checkbox> checkboxes;
    
    public List<Submit> batchSubmitButtons;
    
    public SearchPage() {
        super();
    }
    
    public void onInit() {
        super.onInit();
        searchForm = new Form("searchForm");
        searchForm.setMethod("get");

        queryInput = new TextField("query");
        queryInput.setWidth("32em");
        searchForm.add(queryInput);
        
        filterField = new FilterQueryField("filter");
        searchForm.add(filterField);
        
        pageSize = new Select("pageSize");
        pageSize.setMultiple(false);
        pageSize.add(new Option("20", "20 " + getMessage("form-label-page-size")));
        pageSize.add(new Option("50", "50 " + getMessage("form-label-page-size")));
        pageSize.add(new Option("100", "100 " + getMessage("form-label-page-size")));
        searchForm.add(pageSize);
        
        searchForm.add(new Submit("action", getMessage("form-submit"), this, "onSearch"));

        first = new Submit("action-first", getMessage("form-first"), this, "onJumpToFirst");
        searchForm.add(first);
        last = new Submit("action-last", getMessage("form-last"), this, "onJumpToLast");
        searchForm.add(last);
        prev = new Submit("action-prev", getMessage("form-previous"), this, "onJumpToPrevious");
        searchForm.add(prev);
        next = new Submit("action-next", getMessage("form-next"), this, "onJumpToNext");
        searchForm.add(next);
        
        BatchManager bm = getBatchManager();
        if (bm != null) {
            save = new Submit("action-save", getMessage("form-save"), this, "onSave");
            searchForm.add(save);
        }
        
        batchSubmitButtons = new ArrayList<Submit>();
        if (!bm.listOpenBatches(user.getUsername(), collection.getId()).isEmpty()) {
            for (Batch batch : bm.listOpenBatches(user.getUsername(), collection.getId())) {
                Submit submit = new Submit("actionUpdateBatch-" + batch.getId(), getMessage("form-add-selected-to-batch", format.escape(batch.getName())), this, "onSaveSelectionToBatch");
                searchForm.add(submit);
                batchSubmitButtons.add(submit);
            }

        }
        
        checkboxes = new HashMap<String, Checkbox>();
        if (getBatchManager() == null) {
            searchForm.remove(save);
        }
        
        /* 
         * Loads any cached search results... these will
         * either be used in the event that we're returning
         * to a a search, or used to improve performance in
         * the event that we are getting the next page, or
         * overwritten if we're performing a new search.
         */
        loadSearchResults();
        
        /*
         * For a form submission (like save results to batch)
         * we need to set up the checkboxes after having loaded
         * the results but before processing of that button.
         * In other cases, this can be deferred until onRender.
         */
        if (searchForm.isFormSubmission()) {
            setUpCheckboxes();
        }
        
        addControl(searchForm);
    }
    
    public boolean onSearch() {
        return onSearch(0);
    }
     public boolean onJumpToFirst() {
         return onSearch(0);
     }
     
     public boolean onJumpToLast() {
         return onSearch(Math.max(searchResults.getTotalResultCount() - new Integer((String) pageSize.getSelectedValues().get(0)), 0));
     }
     
     public boolean onJumpToPrevious() {
         return onSearch(Math.max(searchResults.getStartingIndex() - new Integer((String) pageSize.getSelectedValues().get(0)), 0));
     }
     
     public boolean onJumpToNext() {
         return onSearch(Math.min(searchResults.getStartingIndex() + new Integer((String) pageSize.getSelectedValues().get(0)), (searchResults.getTotalResultCount()) - 1));
     }
    
    public boolean onSearch(int offset) {
        try {
            search(getSearchQueryFromForm(offset));
            return true;
        } catch (Throwable t) {
            errorMessage = getMessage("error-search", (t.getMessage() != null ? " (" + t.getMessage() + ")" : ""));
            return true;
        }
    }

    private DefaultSearchQuery getSearchQueryFromForm(int offset) {
        return new DefaultSearchQuery(offset, Integer.parseInt((String) pageSize.getSelectedValues().get(0)), queryInput.getValue(), filterField.getValue(), collection.getId());        
    }
    
    /**
     * When the button is clicked to "save" the search results this
     * method spawns an asynchronous operation, disabled the button
     * and renders the page again. 
     */
    public boolean onSave() {
        DefaultSearchQuery query = getSearchQueryFromForm(0);
        SaveSearchOperation op = new SaveSearchOperation(query, getSearchManager(), getBatchManager(), user.getUsername(), collection.getId(), getMessage("batch-name", query.getEnteredQuery(), getFormat().currentDate()), getMessage("save-batch"));
        UserOperationManager opMan = UserOperationManager.getOperationManager(getContext().getRequest(), user.getUsername());
        opMan.queueOperation(op);
        save.setDisabled(true);
        return true;
    }
    
    public boolean onSaveSelectionToBatch() {
        // determine which batch
        Batch batch = null;
        for (Submit submit : batchSubmitButtons) {
            if (submit.isClicked()) {
                String batchId = submit.getName().substring("actionUpdateBatch-".length());
                for (Batch openBatch : getBatchManager().listOpenBatches(user.getUsername(), collection.getId())) {
                    if (openBatch.getId() == Integer.parseInt(batchId)) {
                        batch = openBatch;
                        break;
                    }
                }
            }
        }
        if (batch != null && checkboxes != null) {
            // determine which items are checked and update the batch
            // unchecking them as we go
            for (Map.Entry<String, Checkbox> mapEntry : checkboxes.entrySet()) {
                if (mapEntry.getValue().isChecked()) {
                    mapEntry.getValue().setChecked(false);
                    batch.addItemId(mapEntry.getKey());
                    //LOGGER.debug("Added item \"" + mapEntry.getKey() + "\" to batch \"" + batch.getName() + "\".");
                } else {
                    //LOGGER.debug(mapEntry.getValue().getName() + " was not checked.");
                }
            }
            
            // save the batch
            try {
                getBatchManager().saveBatch(user.getUsername(), collection.getId(), batch);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return true;
    }
    
    
    public List<Element> getHeadElements() {
        // We use lazy loading to ensure the CSS import is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 
            headElements.add(new CssImport("/style/searchresults.css"));
            headElements.add(new JsImport("/js/multi-select-checkbox.js"));
        } 
        return headElements;
    }

    /**
     * Performs the search.
     */
    private void search(SearchQuery query) throws SearchException, UnsupportedQueryException {
        try {
            searchResults = getSearchManager().search(query);
            storeSearchResults(getContext().getSession(), searchResults, filterField.getQueryDisplay());
        } catch (SearchException ex) {
            ex.printStackTrace();
            throw ex;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new SearchException(t);
        }
        
    }
    
    /**
     * Performs the heavy-lifting associated with generating 
     * the list of SearchResultItemSummary objects that are 
     * set in the model as "results".
     */
    public void onRender() {
        super.onRender();
        if (searchResults != null) {
            setUpCheckboxes();
            setUpSearchButtons();
            setUpSearchFields();
            ItemManager im = getItemManager();
            List<SearchResultItemSummary> results = new ArrayList<SearchResultItemSummary>();
            if (searchResults.getResults() != null) {
                for (Item item : searchResults.getResults()) {
                    SearchResultItemSummary summary = new SearchResultItemSummary(item, collection); 
                    results.add(summary);
                }
            }
            addModel("results", results);
            addModel("resultsSummaryText", getMessage("results-summary", new Integer(searchResults.getStartingIndex() + 1), new Integer(searchResults.getStartingIndex() + searchResults.getResults().size()), new Integer(searchResults.getTotalResultCount())));
            
            if (searchResults.getTotalResultCount() == 0) {
                save.setDisabled(true);
            }
        }
    }
    
    private void setUpCheckboxes() {
        if (!checkboxes.isEmpty()) {
            for (Checkbox checkbox : checkboxes.values()) {
                searchForm.remove(checkbox);
            }
            checkboxes.clear();
        }
        int i = 0;
        if (!getBatchManager().listOpenBatches(user.getUsername(), collection.getId()).isEmpty() && searchResults != null) {
            for (Item item : searchResults.getResults()) {
                Checkbox checkbox = new Checkbox("checkbox-" + (i ++));
                checkbox.addStyleClass("imageSelectCheck");
                checkbox.setAttribute("onClick", "determineCheckBoxSelection(this)");
                checkboxes.put(item.getId(), checkbox);
                searchForm.add(checkbox);
            }
        }
    }
    
    /**
     * The search result set navigation buttons may be disabled or enabled
     * depending on characteristics of the search.  This method
     */
    private void setUpSearchButtons() {
        if (searchResults.getStartingIndex() == 0) {
            first.setDisabled(true);
            prev.setDisabled(true);
        }
        if (searchResults.getStartingIndex() + searchResults.getSearchQuery().getMaxRecords() >= searchResults.getTotalResultCount()) {
            last.setDisabled(true);
            next.setDisabled(true);
        }
    }

    private void setUpSearchFields() {
        if (searchResults.getSearchQuery().getEnteredQuery() != null) {
            queryInput.setValue(searchResults.getSearchQuery().getEnteredQuery());
        }
        if (searchResults.getSearchQuery().getFilterQuery() != null) {
            filterField.setValue(getBrowseQueryName(getContext().getSession()), searchResults.getSearchQuery().getFilterQuery());
        }
        pageSize.setValue(String.valueOf(searchResults.getSearchQuery().getMaxRecords()));
    }
    
    private static void storeSearchResults(HttpSession session, SearchResults searchResults) {
        session.setAttribute("saved-search-results", searchResults);
    }
    
    private static void storeSearchResults(HttpSession session, SearchResults searchResults, String browseQueryName) {
        session.setAttribute("saved-search-results", searchResults);
        session.setAttribute("browse-query-name", browseQueryName);
    }
    
    private static String getBrowseQueryName(HttpSession session) {
        return (String) session.getAttribute("browse-query-name");
    }
    
    /**
     * Loads the search results that have been stored in the
     * session and updates the form fields to reflect the
     * values that should have been entered.
     */
    private void loadSearchResults() {
        searchResults = (SearchResults) getContext().getSession().getAttribute("saved-search-results");
        if (searchResults != null) {
            SearchQuery query = searchResults.getSearchQuery();
        }
    }
    
    public static SearchResults getCurrentSearchResults(HttpSession session) {
    	return (SearchResults) session.getAttribute("saved-search-results");
    }
    
    public static SearchResults getNextPage(HttpSession session, SearchManager sm) {
    	SearchResults currentPage = getCurrentSearchResults(session);
    	if (currentPage == null) {
    		return null;
    	} else {
    		DefaultSearchQuery nextPageQuery = new DefaultSearchQuery(currentPage.getStartingIndex() + currentPage.getResults().size(), currentPage.getSearchQuery().getMaxRecords(), currentPage.getSearchQuery().getEnteredQuery(), currentPage.getSearchQuery().getFilterQuery(), currentPage.getSearchQuery().getCollectionId());
    		SearchResults results;
			try {
				results = sm.search(nextPageQuery);
			} catch (SearchException ex) {
				return null;
			} catch (UnsupportedQueryException ex) {
				return null;
			}
    		storeSearchResults(session, results);
    		return results;
    	}
    }
}
