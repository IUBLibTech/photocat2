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

import java.util.ArrayList;
import java.util.List;

import edu.indiana.dlib.catalog.asynchronous.Dialog;
import edu.indiana.dlib.catalog.asynchronous.Operation;
import edu.indiana.dlib.catalog.batch.Batch;
import edu.indiana.dlib.catalog.batch.BatchManager;
import edu.indiana.dlib.catalog.config.ItemMetadata;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.SearchResults;

/**
 * A simple Operation that fetches the results of a given 
 * search query and stores them to a Batch.  Once completed
 * this operation opens and saves the batch.  No interaction
 * is required.
 */
public class SaveSearchOperation implements Operation {

    private boolean triggerAbort;
    
    private DefaultSearchQuery query;
    
    private SearchManager sm;
    
    private BatchManager bm;
    
    private String username;
    
    private String collectionId;
    
    private String batchName;
    
    private String operationName;
    
    private double percentComplete;
    
    private boolean started;
    
    private boolean complete;
    
    public SaveSearchOperation(DefaultSearchQuery query, SearchManager sm, BatchManager bm, String username, String collectionId, String batchName, String operationName) {
        this.query = query;
        this.sm = sm;
        this.bm = bm;
        this.username = username;
        this.collectionId = collectionId;
        this.batchName = batchName;
        percentComplete = -1;
        this.operationName = operationName;
    }
    
    public String getDescription() {
        return operationName;
    }

    public void abort() {
        triggerAbort = true;
    }


    public double getEstimatedPercentCompleted() {
        return percentComplete;
    }

    public void run() {
        try {
            started = true;
            SearchResults results = sm.search(query);
            List<String> ids = new ArrayList<String>(results.getTotalResultCount());
            do {
                for (ItemMetadata im : results.getResults()) {
                    if (triggerAbort) {
                        return;
                    }
                    ids.add(im.getId());
                    percentComplete = ((double) ids.size() / (double) results.getTotalResultCount() );
                }
                query = new DefaultSearchQuery(ids.size(), 100, query.getEnteredQuery(), query.getFilterQuery(), query.getCollectionId());
                results = sm.search(query);
            } while (ids.size() < results.getTotalResultCount());
            Batch batch = bm.createNewBatch(username, collectionId, batchName, ids);
            bm.openBatch(batch.getUserId(), batch.getCollectionId(), batch.getId());
        } catch (Exception ex) {
            
        }
        
    }

    public boolean hasStarted() {
        return started;
    }

    /**
     * Always returns false because this operation does not
     * require human interaction.
     */
    public boolean requiresUserInteraction() {
        return false;
    }

    /**
     * Always returns null because this Operation is non-interactive.
     */
    public Dialog getInteractionDialog() {
        return null;
    }

    /**
     * Always throws an IllegalStateException because this Operation
     * is non-interactive.
     */
    public void respondToInteractionDialog(Dialog dialog, String response) {
        throw new IllegalStateException();
    }
}
