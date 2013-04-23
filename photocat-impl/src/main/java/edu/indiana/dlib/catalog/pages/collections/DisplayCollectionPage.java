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

import java.util.HashMap;
import java.util.Map;

import org.apache.click.control.Form;
import org.apache.click.control.Submit;
import org.apache.click.extras.control.IntegerField;

import edu.indiana.dlib.catalog.config.ItemManager;
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
        
        createNewRecordForm = new Form("createNewRecordForm");
        createNewRecordForm.add(new Submit(getMessage("form-create-new-record"), this, "onCreateNewRecord"));
        addControl(this.createNewRecordForm);
    }
    
    public void onInit() {
        super.onInit();
        if (!getItemManager().isRecordCreationEnabled(collection)) {
            createNewRecordForm.setDisabled(true);
        } else {
            for (String name : getItemManager().getRequiredArgumentNames(collection)) {
                IntegerField field = new IntegerField(name);
                field.setRequired(true);
                createNewRecordForm.add(field);
            }
        }
    }
    
    /**
     * The callback method for the submit button on the createNewRecordForm.
     * This method creates a new empty record and then redirects the user
     * to the conf-edit page for that record (after waiting for the index
     * to be updated to include the new record).
     */
    public boolean onCreateNewRecord() {
        if (createNewRecordForm.isValid()) {
            ItemManager im = getItemManager();
        	Map<String, String> args = new HashMap<String, String>();
        	for (String name : im.getRequiredArgumentNames(collection)) {
        	    args.put(name, String.valueOf(createNewRecordForm.getField(name).getValue()));
            }
        	try {
        	    setRedirect("edit-item.htm?id=" + im.createNewItem(collection, user, args));
                return false;
        	} catch (RepositoryException ex) {
        	    createNewRecordForm.setError(ex.getMessage());
        	    return true;
        	}
        } else {
            return true;
        }
    }
    
    protected String getBreadcrumbs() {
        return getMessage("breadcrumbs");
    }
    
    protected String getTitle() {
        return getMessage("title", this.collection.getCollectionMetadata().getFullName());
    }
    
    
}
