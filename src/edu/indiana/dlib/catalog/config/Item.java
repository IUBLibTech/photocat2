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
package edu.indiana.dlib.catalog.config;

import java.util.Collection;
import java.util.List;

/**
 * An object encapsulating an "item" within the cataloging system.
 * This item is some data files and some metadata.  Implementations
 * may vary dramatically on how this item information is maintained
 * or compiled.
 */
public interface Item {
    
    /**
     * Gets the current metadata for this item.
     * @return
     */
    public ItemMetadata getMetadata();
    

    /**
     * Gets all known data views (representing accessible data files)
     * for this item.
     */
    public Collection<DataView> listDataViews();
    
    /**
     * Gets the DataView from the list of data views that is the 
     * default preview view.  This value will be strctly == to 
     * one of the values returned by listDataViews().  
     */
    public DataView getPreview();
    
    /**
     * Gets the identifier that is used locally to identify the item within a 
     * single collection.
     */
    public String getIdWithinCollection();

    /**
     * Gets the collection to which this item belongs. (Implementations
     * should simply pass through to the ItemMetadata object).
     */
    public String getCollectionId();
    
    /**
     * Gets the id (typically from the ItemMetadata).
     */
    public String getId();
    
    /**
     * Gets a List of name value pairs that should be retained through 
     * editing roundtrips.  These may be used by the underlying 
     * implementation to improve performance, perform optimistic locking
     * or other sorts of tracking.  Implementers must beware that 
     * these values may be forged or modified during the roundtrip and
     * therefore should not expose access to anything critical.
     */
    public List<NameValuePair> getControlFields();
    
}
