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
package edu.indiana.dlib.catalog.batch;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
import edu.indiana.dlib.catalog.asynchronous.Dialog;
import edu.indiana.dlib.catalog.asynchronous.Operation;
import edu.indiana.dlib.catalog.asynchronous.PresentErrorDialog;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.config.ItemMetadata;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.structured.DefaultStructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchResults;
import edu.indiana.dlib.catalog.search.structured.constraints.CollectionSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.FieldPartValueSearchConstraint;

/**
 * Locates all records with a matching value and replaces/removes
 * that value from the record.
 */
public class SearchAndReplaceOperation implements Operation {

    protected Logger LOGGER = Logger.getLogger(SearchAndReplaceOperation.class);
    
    private boolean abort;

    private String description;
    
    private double percentComplete;
    
    private SearchManager sm;
    
    private ItemManager itemManager;
    
    private BatchManager bm;
    
    private CollectionConfiguration collection;
    
    private UserInfo user;
    
    private FieldConfiguration field;
    
    private String partName;
    
    private String value;
    
    private String replacement;
    
    private Throwable error;
    
    public SearchAndReplaceOperation(String description, SearchManager sm, ItemManager im, BatchManager bm, CollectionConfiguration collection, UserInfo user, FieldConfiguration field, String partName, String value, String replacement) {
        abort = false;
        this.description = description;
        this.sm = sm;
        this.itemManager = im;
        this.bm = bm;
        this.collection = collection;
        this.user = user;
        this.field = field;
        this.partName = partName;
        this.value = value;
        this.replacement = replacement;
    }
    
    public void abort() {
        abort = true;
    }

    public String getDescription() {
        return description;
    }

    public double getEstimatedPercentCompleted() {
        return percentComplete;
    }

    public synchronized Dialog getInteractionDialog() {
        if (error == null) {
            return new PresentErrorDialog("An error occurred while processing your request!", this);
        } else {
            return null;
        }
    }

   public synchronized boolean requiresUserInteraction() {
        return error != null;
    }

    public void respondToInteractionDialog(Dialog dialog, String response) {
    }

    public void run() {
        StructuredSearchQuery query = new DefaultStructuredSearchQuery(new CollectionSearchConstraint(collection, true), new FieldPartValueSearchConstraint(field.getFieldType(), partName, value));
        List<String> idsUpdated = new ArrayList<String>();
        int progress = 0;
        try {
            List<String> itemIdsToUpdate = new ArrayList<String>();
            while (query != null) {
                StructuredSearchResults results = sm.search(query);
                for (Item item : results.getResults()) {
                    if (abort) {
                        return;
                    }
                    itemIdsToUpdate.add(item.getId());
                }
                query = DefaultStructuredSearchQuery.nextPageQuery(results);
            }

            for (String itemId : itemIdsToUpdate) {
                if (abort) {
                    return;
                }
                percentComplete = (double) progress / (double) itemIdsToUpdate.size(); 
                Item item = itemManager.fetchItemIncludingPrivateMetadata(itemId, collection);
                FieldData fieldData = item.getMetadata().getFieldData(field.getFieldType());
                if (fieldData == null) {
                    // nothing to do here
                    throw new RuntimeException("No field to update for item " + item.getId());
                } else {
                    fieldData.replaceValuesWithPart(partName, value, replacement);
                    itemManager.saveItemMetadata(item, collection, user);
                    idsUpdated.add(item.getId());
                }
                progress ++;
            }
        } catch (Throwable t) {
            error = t;
            LOGGER.error("Error performing a search and replace operation!", t);
        } finally {
            try {
                Batch b = bm.createNewBatch(user.getUsername(), collection.getId(), "Items updated as part of search and replace operation.", idsUpdated);
                bm.openBatch(user.getUsername(), collection.getId(), b.getId());
            } catch (Throwable t) {
                error = t;
                LOGGER.error("Error updating a batch for a search and replace operation!", t);
            }
        }
    }

}
