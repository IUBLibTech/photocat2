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

import org.apache.click.control.Form;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;

import edu.indiana.dlib.catalog.batch.Batch;
import edu.indiana.dlib.catalog.batch.BatchManager;
import edu.indiana.dlib.catalog.batch.impl.click.control.ItemPreviewAjaxBehavior;

/**
 * <p>
 *   A page to manage a batch.  Minimally you can view what items 
 *   are in the batch, remove items from the batch and manually 
 *   add items to the batch. 
 * </p>
 */
public class ViewBatchPage extends CollectionPage {

    public Batch batch;
    
    public String batchIdStr;
    
    private Form batchForm;

    private TextField nameField;
    
    private Select itemSelect;
    
    public void onInit() {
        super.onInit();
        BatchManager bm = getBatchManager();
        if (bm != null) {
            batchIdStr = getContext().getRequestParameter("batchId");
            try {
                batch = bm.openBatch(user.getUsername(), collection.getId(), Integer.parseInt(batchIdStr));
                batchForm = new Form("batchForm");
                batchForm.setActionURL("view-batch.htm?batchId=" + batchIdStr);
                
                nameField = new TextField("name", getMessage("label-batch-name"));
                nameField.setValue(batch.getName());
                nameField.setWidth("32em");
                nameField.setRequired(true);
                batchForm.add(nameField);
                
                itemSelect = new Select("itemSelect", true);
                itemSelect.setMultiple(true);
                itemSelect.addAll(batch.listItemIds());
                itemSelect.setSize(Math.min(15, itemSelect.getOptionList().size()));
                itemSelect.setRequired(true);
                itemSelect.addBehavior(new ItemPreviewAjaxBehavior("preview-div", getItemManager(), collection));
                batchForm.add(itemSelect);
                
                batchForm.add(new Submit("delete", getMessage("button-delete-item"), this, "onDeleteSelection"));
                batchForm.add(new Submit("rename", getMessage("button-rename-batch"), this, "onRename"));
                batchForm.add(new Submit("deleteBatch", getMessage("button-delete-batch"), this, "onDeleteClick"));
                batchForm.add(new Submit("editAllItems", getMessage("button-edit-batch"), this, "onBatchEditClick"));
                addControl(batchForm);
            } catch (NumberFormatException e) {
                // fall through, leaving the batch unloaded so the page can display
                // an error.
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            
        }
    }
    
    public boolean onDeleteSelection() {
        if (batchForm.isValid()) {
            for (Object selectedObject : itemSelect.getSelectedValues()) {
                batch.removeItemId((String) selectedObject);
                try {
                    getBatchManager().saveBatch(user.getUsername(), collection.getId(), batch);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            setRedirect("view-batch.htm?batchId=" + getContext().getRequestParameter("batchId"));
            return false;
        } else {
            return true;
        }
    }
    
    public boolean onDeleteClick() {
        try {
            getBatchManager().deleteBatch(user.getUsername(), collection.getId(), batch.getId());
            setRedirect("batches.htm");
            return false;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public boolean onBatchEditClick() {
        setRedirect("edit-batch.htm?batchId=" + batch.getId());
        return false;
    }
    
    public boolean onRename() {
        try {
            if (batchForm.isValid()) {
                batch.setName(nameField.getValue());
                getBatchManager().saveBatch(user.getUsername(), collection.getId(), batch);
                return true;
            } else {
                // fall through and present the error
                return true;
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    protected String getTitle() {
        if (batch != null) {
            return getMessage("title", batch.getName());
        } else {
            return getMessage("title-no-batch");
        }
    }
    
    
}
