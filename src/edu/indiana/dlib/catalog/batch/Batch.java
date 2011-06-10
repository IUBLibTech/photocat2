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
package edu.indiana.dlib.catalog.batch;

import java.util.List;

/**
 * A Batch is essentially a named and ordered Set of item
 * identifiers.
 */
public interface Batch {

    /**
     * Gets a unique identifier for the batch.
     */
    public int getId();
    
    /**
     * Gets the user who own this batch.
     */
    public String getUserId();

    /**
     * Gets the collection this batch belongs in.  Every
     * item in this batch will be from this collection.
     */
    public String getCollectionId();
    
    /**
     * Gets the user-assigned name for this batch.
     */
    public String getName();
    
    /**
     * Sets the user-assigned name for this batch.
     * @param newName the new name for the batch
     */
    public void setName(String newName);

    /**
     * Gets the total number of items in this batch.
     */
    public int getSize();
    
    /**
     * Returns a List containing all of the item ids currently
     * included in this Batch.
     */
    public List<String> listItemIds();

    /**
     * Adds the given item identifier to this batch.
     * @param id the identifier to be added
     * @return true if the item id wasn't already in the batch
     */
    public boolean addItemId(String id);
    
    /**
     * Removes the given item identifier to this batch.
     * @param id the identifier to be removed
     * @return true if the item id was in the batch before
     * this method was called
     */
    public boolean removeItemId(String id);

}
