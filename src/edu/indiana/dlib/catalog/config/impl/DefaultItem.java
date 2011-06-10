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
package edu.indiana.dlib.catalog.config.impl;

import java.util.Collection;
import java.util.List;

import edu.indiana.dlib.catalog.config.DataView;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemMetadata;
import edu.indiana.dlib.catalog.config.NameValuePair;

public class DefaultItem implements Item {
    
    private ItemMetadata metadata;

    private List<DataView> dataViewList;
    
    private DataView preview;
    
    private List<NameValuePair> controlFields;
    
    public DefaultItem(ItemMetadata metadata, List<DataView> dataViews, DataView preview, List<NameValuePair> controlFields) {
        this.metadata = metadata;
        this.dataViewList = dataViews;
        this.preview = preview;
        this.controlFields = controlFields;
    }
    
    public String getCollectionId() {
        return this.metadata.getCollectionId();
    }

    public String getIdWithinCollection() {
        return this.metadata.getId().substring(this.metadata.getId().lastIndexOf('/') + 1);
    }
    
    public String getId() {
        return this.metadata.getId();
    }

    public ItemMetadata getMetadata() {
        return this.metadata;
    }

    public List<NameValuePair> getControlFields() {
        return this.controlFields;
    }

    public DataView getPreview() {
        return this.preview;
    }

    public Collection<DataView> listDataViews() {
        return this.dataViewList;
    }

}
