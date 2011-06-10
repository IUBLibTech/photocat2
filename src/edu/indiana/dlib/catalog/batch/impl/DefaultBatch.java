/**
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
package edu.indiana.dlib.catalog.batch.impl;

import java.util.ArrayList;
import java.util.List;

import edu.indiana.dlib.catalog.batch.Batch;

/**
 * A bare-bones implementation of Batch with setter and
 * getter methods for all the relevant properties.
 */
public class DefaultBatch implements Batch {

    private int id;
    
    private String userId;
    
    private String collectionId;
    
    private String name;
    
    private List<String> ids;
    
    public DefaultBatch(int id, String userId, String collectionId, String name) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.collectionId = collectionId;
        ids = new ArrayList<String>();
    }
    
    public DefaultBatch(int id, String userId, String collectionId, String name, List<String> ids) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.collectionId = collectionId;
        this.ids = ids;
    }
    
    public int getId() {
        return id;
    }
    
    public String getCollectionId() {
        return collectionId;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String newName) {
        name = newName;
    }

    public boolean addItemId(String id) {
        if (ids.contains(id)) {
            return false;
        } else {
            ids.add(id);
            return true;
        }
    }
    
    public int getSize() {
        return ids.size();
    }

    public List<String> listItemIds() {
        return ids;
    }

    public boolean removeItemId(String id) {
        return ids.remove(id);
    }
    
    public boolean equals(Batch batch) {
        return (batch != null && batch.getId() == this.id && batch.getCollectionId() == this.collectionId && batch.getUserId() == this.userId);
    }
    
    public int hashCode() {
        return (this.id + this.userId + this.collectionId).hashCode();
    }
    
    /**
     * Overrides the default method to return the batches name 
     * followed by the id, userId and collectionId in parentheses.
     */
    public String toString() {
        return name + " (" + id + ", " + userId + ", " + collectionId + ")";
    }
    
}
