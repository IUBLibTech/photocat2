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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.click.Context;
import org.apache.click.control.Column;
import org.apache.click.control.Decorator;
import org.apache.click.control.FileField;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Submit;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;
import org.apache.click.extras.control.LinkDecorator;
import org.apache.commons.fileupload.FileItem;

import edu.indiana.dlib.catalog.accesscontrol.AuthorizationSystemException;
import edu.indiana.dlib.catalog.config.Aspect;
import edu.indiana.dlib.catalog.config.FileSubmissionStatus;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.config.impl.FileSubmitter;
import edu.indiana.dlib.catalog.fields.click.control.RelativeActionLink;

public class ManageFilesPage extends CollectionPage {
    
    public static final String ITEM_ID_PARAM_NAME = "id";
    
    public Item item;
    
    private FileSubmitter fs;

    private Table table;
    private RelativeActionLink deleteLink;
    
    private Table inProcessTable;
    
    private Form form;
    
    /**
     * Fetches the item in question and then determines whether the
     * current user can edit that item. 
     */
    public boolean onSecurityCheck() {
        if (!super.onSecurityCheck()) {
            return false;
        } else {
            String itemId = getContext().getRequest().getParameter(ITEM_ID_PARAM_NAME);
            if (itemId != null) {            
                try {
                    item = getItemManager().fetchItemIncludingPrivateMetadata(itemId, collection);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            } else {
                throw new RuntimeException(getMessage("error-no-item"));
            }
            try {
                boolean allow = getAuthorizationManager().canEditItem(item, collection, unit, user) && getAuthorizationManager().canManageCollection(collection, unit, user);
                if (!allow) {
                    setRedirect("unauthorized.htm");
                    return false;
                }
                return true;
            } catch (AuthorizationSystemException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    public void onInit() {
        super.onInit();

        
        deleteLink = new RelativeActionLink("Delete", this, "onDeleteClick");
        deleteLink.setParameter("id", item.getId());
        addControl(deleteLink);
        
        ItemManager im = getItemManager();
        fs = im.getFileSubmitter(collection);
        
        // this check should be the same as the check to determine whether to 
        // link to this page... (on the item-preview-panel.htm)
        if (!(fs != null && (fs.isFileSubmissionAvailable(item) || fs.isFileRemovalAvailable(item, item.getId())))) {
            setRedirect("edit-item.htm?id=" + item.getId());
            return;
        }
        
        if (fs.isFileSubmissionAvailable(item)) {
            form = new Form("form");
            FileField upload = new FileField("upload");
            upload.setLabel(getMessage("upload-label"));
            form.add(upload);
            
            form.add(new Submit(getMessage("upload-submit"), this, "onUpload"));
            form.add(new HiddenField("id", item.getId()));
            addControl(form);
        }

        table = new Table("table");
        table.setPageSize(10);
        table.setShowBanner(false);
        table.setSortable(false);
        table.setClass(Table.CLASS_BLUE1);
        
        Column c1 = new Column("image");
        c1.setDecorator(new Decorator() {
            public String render(Object object, Context context) {
                Aspect a = (Aspect) object;
                StringBuffer sb = new StringBuffer();
                if (a.getThumbnailView() != null) {
                    sb.append("<img src=\"" + a.getThumbnailView().getURL() + "\" alt=\"thumbnail image\"/>");
                } else {
                    sb.append("Image \"" + a.getId().substring(a.getId().lastIndexOf('/') + 1) + "\" submitted...");
                }
                return sb.toString();
            }});
        table.addColumn(c1);
        
        Column c2 = new Column("action");
        c2.setDecorator(new LinkDecorator(table, deleteLink, "id"));
        table.addColumn(c2);
        table.setDataProvider(new DataProvider() {
            public Iterable getData() {
                List<Aspect> aspects = new ArrayList<Aspect>();
                for (Aspect a : item.getAspects()) {
                    if (fs.isFileRemovalAvailable(item, a.getId())) {
                        aspects.add(a);
                    }
                }
                return aspects;
            }});
        table.addColumn(new Column("id"));
        if (table.getDataProvider().getData().iterator().hasNext()) {
            addControl(table);
        }
        
        inProcessTable = new Table("inProcessTable");
        inProcessTable.setPageSize(10);
        inProcessTable.setShowBanner(false);
        inProcessTable.setSortable(false);
        inProcessTable.setClass(Table.CLASS_BLUE1);
        Column c3 = new Column("status");
        c3.setDecorator(new Decorator() {
            public String render(Object object, Context context) {
                Aspect a = (Aspect) object;
                FileSubmissionStatus status = fs.getFileSubmissionStatus(item, a.getId());
                switch (status.getStatusCode()) {
                    case PENDING_PROCESSING:
                        return getMessage("status-pending-processing", format.date(status.getLastActionDate()));
                    case FILE_VALIDATION_ERROR:
                        return getMessage("status-validation-error", format.date(status.getLastActionDate()));
                    case PENDING_INGEST:
                        return getMessage("status-pending-ingest", format.date(status.getLastActionDate()));
                    case INGESTED:
                        return getMessage("status-ingested", format.date(status.getLastActionDate()));
                    default:
                        return getMessage("status-not-configured");
                }
            }});
        inProcessTable.addColumn(c3);
        inProcessTable.setDataProvider(new DataProvider() {
            public Iterable getData() {
                List<Aspect> aspects = new ArrayList<Aspect>();
                for (Aspect a : item.getAspects()) {
                    if (!fs.getFileSubmissionStatus(item, a.getId()).getStatusCode().equals(FileSubmissionStatus.Status.PENDING_SUBMISSION) && !fs.isFileRemovalAvailable(item, a.getId())) {
                        aspects.add(a);
                    }
                }
                return aspects;
            }});
        inProcessTable.addColumn(new Column("id"));
        if (inProcessTable.getDataProvider().getData().iterator().hasNext()) {
            addControl(inProcessTable);
        }
        
    }
    
    public boolean onUpload() {
        try {
            FileItem fileItem = ((FileField) form.getControl("upload")).getFileItem();
            if (fileItem != null && fileItem.getSize() > 0) {
                LOGGER.info(fileItem.getName() + " was uploaded...");
                getItemManager().getFileSubmitter(collection).submitFile(fileItem.getInputStream(), item);
                setRedirect("manage-files.htm?id=" + item.getId());
                return false;
            } else {
                LOGGER.info("No file uploaded.");
                return true;
            }
        } catch (Throwable t) {
            LOGGER.error("Error uploading file!", t);
            form.setError(getMessage("upload-error"));
            return true;
        }
    }
    
    public boolean onDeleteClick() { 
        String id = deleteLink.getValue(); 
        try {
            LOGGER.info("Deleting images from " + item.getId() + ".");
            ItemManager im = getItemManager();
            im.getFileSubmitter(collection).removeFile(item, id);
            
            
            setRedirect("manage-files.htm?id=" + item.getId());
            return false;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public boolean onCancelClick() {
        setRedirect("edit-item.htm?id=" + item.getId());
        return false;
    }
    
    protected String getTitle() {
        return getMessage("title", item.getIdWithinCollection());
    }
    
    public List<Element> getHeadElements() { 
        // We use lazy loading to ensure the CSS import is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 

            CssImport cssImport = new CssImport("/css/manage-files.css"); 
            headElements.add(cssImport); 
        } 
        return headElements; 
    }

}
