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

import org.apache.click.control.Form;
import org.apache.click.control.Submit;

import edu.indiana.dlib.catalog.config.RepositoryException;

public class DisplayCollectionPage extends CollectionPage {

    /**
     * A form that is displayed when the collection is configured to allow 
     * the creation of new records if the given user has permission to 
     * create records.  When these conditions aren't met, this form will
     * be null.
     */
    public Form createNewRecordForm;

    public DisplayCollectionPage() {
        super();
        
        this.createNewRecordForm = new Form("createNewRecordForm");
        this.createNewRecordForm.add(new Submit(getMessage("form-create-new-record"), this, "onCreateNewRecord"));
        this.addControl(this.createNewRecordForm);
    }
    
    public void onInit() {
        super.onInit();
        if (!collection.getCollectionMetadata().allowRecordCreation()) {
            this.createNewRecordForm.setDisabled(true);
        }
    }
    
    /**
     * The callback method for the submit button on the createNewRecordForm.
     * This method creates a new empty record and then redirects the user
     * to the conf-edit page for that record (after waiting for the index
     * to be updated to include the new record).
     */
    public boolean onCreateNewRecord() {
        try {
            this.setRedirect("edit-item.htm?id=" + getItemManager().createNewItem(collection, user));
            return true;
        } catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    protected String getBreadcrumbs() {
        return getMessage("breadcrumbs");
    }
    
    protected String getTitle() {
        return getMessage("title", this.collection.getCollectionMetadata().getName());
    }
    
    
}
