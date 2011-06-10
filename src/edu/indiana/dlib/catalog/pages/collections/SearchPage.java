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
package edu.indiana.dlib.catalog.pages.collections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.click.control.Checkbox;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
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
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.config.ItemMetadata;
import edu.indiana.dlib.catalog.config.RepositoryException;
import edu.indiana.dlib.catalog.search.SearchException;
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
    
    private HiddenField total;
    
    private HiddenField current;
    
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

        this.queryInput = new TextField("query");
        this.queryInput.setWidth("32em");
        searchForm.add(this.queryInput);
        
        this.filterField = new FilterQueryField("filter");
        searchForm.add(this.filterField);
        
        this.pageSize = new Select("pageSize");
        this.pageSize.setMultiple(false);
        this.pageSize.add(new Option("20", "20 " + getMessage("form-label-page-size")));
        this.pageSize.add(new Option("50", "50 " + getMessage("form-label-page-size")));
        this.pageSize.add(new Option("100", "100 " + getMessage("form-label-page-size")));
        searchForm.add(this.pageSize);
        
        this.current = new HiddenField("currentOffset", Integer.class);
        searchForm.add(this.current);
        
        this.total = new HiddenField("total", Integer.class);
        searchForm.add(this.total);
        
        searchForm.add(new Submit("action", getMessage("form-submit"), this, "onSearch"));

        this.first = new Submit("action-first", getMessage("form-first"), this, "onJumpToFirst");
        searchForm.add(this.first);
        this.last = new Submit("action-last", getMessage("form-last"), this, "onJumpToLast");
        searchForm.add(this.last);
        this.prev = new Submit("action-prev", getMessage("form-previous"), this, "onJumpToPrevious");
        searchForm.add(this.prev);
        this.next = new Submit("action-next", getMessage("form-next"), this, "onJumpToNext");
        searchForm.add(this.next);
        
        BatchManager bm = getBatchManager();
        if (bm != null) {
            save = new Submit("action-save", getMessage("form-save"), this, "onSave");
            searchForm.add(save);
        }
        
        batchSubmitButtons = new ArrayList<Submit>();
        if (!bm.listOpenBatches(user.getUsername(), collection.getId()).isEmpty()) {
            for (Batch batch : bm.listOpenBatches(user.getUsername(), collection.getId())) {
                Submit submit = new Submit("actionUpdateBatch-" + batch.getId(), getMessage("form-add-selected-to-batch", batch.getName()), this, "onSaveSelectionToBatch");
                searchForm.add(submit);
                batchSubmitButtons.add(submit);
            }

        }
        
        checkboxes = new HashMap<String, Checkbox>();
        loadSearchResults();
        
        if (getBatchManager() == null) {
            searchForm.remove(save);
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
         return onSearch(Math.max(((Integer) this.total.getValueObject()).intValue() - new Integer((String) this.pageSize.getSelectedValues().get(0)), 0));
     }
     
     public boolean onJumpToPrevious() {
         return onSearch(Math.max(((Integer) this.current.getValueObject()).intValue() - new Integer((String) this.pageSize.getSelectedValues().get(0)), 0));
     }
     
     public boolean onJumpToNext() {
         return onSearch(Math.min(((Integer) this.current.getValueObject()).intValue() + new Integer((String) this.pageSize.getSelectedValues().get(0)), ((Integer) this.total.getValueObject()) - 1));
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
        return new DefaultSearchQuery(offset, Integer.parseInt((String) this.pageSize.getSelectedValues().get(0)), this.queryInput.getValue(), this.filterField.getValue(), collection.getId());        
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
            current.setValueObject(new Integer(this.searchResults.getStartingIndex()));
            total.setValueObject(new Integer(this.searchResults.getTotalResultCount()));
            
            if (searchResults.getStartingIndex() == 0) {
                first.setDisabled(true);
                prev.setDisabled(true);
            }
            if (searchResults.getStartingIndex() + this.searchResults.getSearchQuery().getMaxRecords() >= this.searchResults.getTotalResultCount()) {
                last.setDisabled(true);
                next.setDisabled(true);
            }
            storeSearchResults();
            
            setUpCheckboxes();
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
        ItemManager im = this.getItemManager();
        List<SearchResultItemSummary> results = new ArrayList<SearchResultItemSummary>();
        if (searchResults.getResults() != null) {
            for (ItemMetadata item : searchResults.getResults()) {
                try {
                    SearchResultItemSummary summary = new SearchResultItemSummary(im.fetchItem(item.getId()), super.collection); 
                    results.add(summary);
                } catch (RepositoryException ex) {
                    results.add(null);
                }
            }
        }
        addModel("results", results);
        addModel("resultsSummaryText", getMessage("results-summary", new Integer(searchResults.getStartingIndex() + 1), new Integer(this.searchResults.getStartingIndex() + this.searchResults.getResults().size()), new Integer(this.searchResults.getTotalResultCount())));
        
        if (searchResults == null || searchResults.getTotalResultCount() == 0) {
            save.setDisabled(true);
        }
    }
    
    private void storeSearchResults() {
        getContext().getSession().setAttribute("saved-search-results", searchResults);
    }
    
    private void loadSearchResults() {
        searchResults = (SearchResults) getContext().getSession().getAttribute("saved-search-results");
        if (searchResults != null) {
            setUpCheckboxes();
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
        if (!getBatchManager().listOpenBatches(user.getUsername(), collection.getId()).isEmpty()) {
            for (ItemMetadata item : searchResults.getResults()) {
                Checkbox checkbox = new Checkbox("checkbox-" + (i ++));
                checkbox.addStyleClass("imageSelectCheck");
                checkbox.setAttribute("onClick", "determineCheckBoxSelection(this)");
                checkboxes.put(item.getId(), checkbox);
                searchForm.add(checkbox);
            }
        }
    }

}
