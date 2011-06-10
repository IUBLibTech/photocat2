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
import java.util.List;

import org.apache.click.control.AbstractLink;
import org.apache.click.control.Column;
import org.apache.click.control.Form;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;
import org.apache.click.extras.control.LinkDecorator;

import edu.indiana.dlib.catalog.batch.Batch;
import edu.indiana.dlib.catalog.batch.BatchManager;
import edu.indiana.dlib.catalog.batch.impl.click.control.BatchTable;
import edu.indiana.dlib.catalog.batch.impl.click.control.OpenLinkDecorator;
import edu.indiana.dlib.catalog.fields.click.control.RelativeActionLink;

/**
 * <p>
 *   A page with the primary purpose of listing the batches
 *   for the current user and collection.
 * </p>
 * <p>
 *   From this page a user may get an overview of the batches,
 *   create new batches and navigate to other pages to perform
 *   operations on batches or browse/manage the contents of 
 *   an individual batch.
 * </p>
 * <p>
 *   This page adds the form "batchForm" to the page model
 *   as well as the table "batches".
 * </p>
 */
public class BatchesPage extends CollectionPage {

    private RelativeActionLink editItemsLink;
    
    private RelativeActionLink manageBatchLink;
    
    private RelativeActionLink toggleOpenLink;
    
    private BatchTable batchTable;
    
    private Form batchForm;
    
    public void onInit() {
        super.onInit();
        BatchManager bm = getBatchManager();
        if (bm != null) {
            try {
                List<Batch> batches = bm.listAllBatches(user.getUsername(), collection.getId());
                if (batches.isEmpty()) {
                    // skip it, and don't create a batch table
                } else {
                    batchTable = new BatchTable("batches", batches);
                    
                    editItemsLink = new RelativeActionLink("edit", getMessage("label-batch-edit"), this, "onEditClick");
                    addControl(editItemsLink);
                    
                    manageBatchLink = new RelativeActionLink("manage", getMessage("label-batch-manage"), this, "onManageClick");
                    addControl(manageBatchLink);
                    
                    Column actionsColumn = new Column("action", getMessage("label-batch-action"));
                    actionsColumn.setDecorator(new LinkDecorator(batchTable, new AbstractLink[] { editItemsLink, manageBatchLink }, "id"));
                    actionsColumn.setSortable(false);
                    batchTable.addColumn(actionsColumn);
                    
                    toggleOpenLink = new RelativeActionLink("openClose", this, "onToggleOpenClick");
                    addControl(toggleOpenLink);
                    Column openCloseColumn = new Column("open/close", getMessage("label-batch-open-close"));
                    openCloseColumn.setDecorator(new OpenLinkDecorator(batchTable, toggleOpenLink, "id", bm, getMessage("label-batch-open"), getMessage("label-batch-close")));
                    openCloseColumn.setSortable(false);
                    batchTable.addColumn(openCloseColumn);
                    
                    addControl(batchTable);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            
            // This is disabled until we add more robust mechanisms for editing
            // the contents of batches.
            batchForm = new Form("batchForm");
            TextField nameField = new TextField("name");
            nameField.setRequired(true);
            batchForm.add(nameField);
            Submit newBatchButton = new Submit("new", getMessage("button-new-batch"), this, "onCreateNewBatch");
            batchForm.add(newBatchButton);
            this.addControl(batchForm);
        }
    }
    
    public boolean onCreateNewBatch() {
        try {
            if (batchForm.isValid()) {
                getBatchManager().createNewBatch(user.getUsername(), collection.getId(), batchForm.getField("name").getValue(), null);
                setRedirect("batches.htm");
                return false;
            } else {
                return true;
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
    
    public boolean onEditClick() {
        int batchId = editItemsLink.getValueInteger();
        setRedirect("edit-batch.htm?batchId=" + batchId);
        return false;
    }
    
    
    public boolean onManageClick() {
        int batchId = manageBatchLink.getValueInteger();
        setRedirect("view-batch.htm?batchId=" + batchId);
        return false;
    }
    
    public boolean onToggleOpenClick() {
        BatchManager bm = getBatchManager();
        int batchId = toggleOpenLink.getValueInteger();
        try {
            if (!bm.isBatchOpen(user.getUsername(), collection.getId(), batchId)) {
                bm.openBatch(user.getUsername(), collection.getId(), batchId);
            } else {
                bm.closeBatch(user.getUsername(), collection.getId(), batchId);
            }
            setRedirect("batches.htm");
            return false;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
    public List<Element> getHeadElements() { 
        // We use lazy loading to ensure the CSS import is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 

            CssImport cssImport = new CssImport("/css/batches.css"); 
            headElements.add(cssImport); 
        } 
        return headElements; 
    }
}
