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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.click.Control;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Label;
import org.apache.click.control.Panel;
import org.apache.click.control.Submit;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;
import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.accesscontrol.AuthorizationSystemException;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.HistoryEnabledItemManager;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.config.OptimisticLockingException;
import edu.indiana.dlib.catalog.config.RepositoryException;
import edu.indiana.dlib.catalog.config.HistoryEnabledItemManager.VersionInformation;
import edu.indiana.dlib.catalog.fields.UIField;
import edu.indiana.dlib.catalog.fields.click.control.AutoCompleteEnabled;
import edu.indiana.dlib.catalog.fields.click.control.ClassTogglingLink;
import edu.indiana.dlib.catalog.fields.click.control.MissingField;
import edu.indiana.dlib.catalog.fields.click.control.UnrecognizedFieldDataContainer;
import edu.indiana.dlib.catalog.fields.click.control.ViewVersionActionLink;
import edu.indiana.dlib.catalog.fields.click.control.autocomplete.AutoCompleteAjaxTargetControl;
import edu.indiana.dlib.catalog.fields.click.control.autocomplete.VocabularySourceAutoCompleteTextField;
import edu.indiana.dlib.catalog.pages.collections.panel.HelpPanel;

/**
 * The page that displays an item for the user to edit.
 */
public class EditItemPage extends CollectionPage implements AutoCompleteEnabled {
    
    private Logger LOGGER = Logger.getLogger(EditItemPage.class);
    
    public static final String ITEM_ID_PARAM_NAME = "id";
    
    public Item item;
    
    public Form form;
    
    public Panel itemPreviewPanel;
    
    public Panel itemHistoryPanel;
    
    public Panel transformationPanel;
    
    private AutoCompleteAjaxTargetControl autoCompleter;
    
    private List<HiddenField> controlFields;
    
    private Map<String, HiddenField> hiddenFieldNameToControlMap;
    
    public EditItemPage() {
        this.form = new Form();
        this.form.setColumns(2);
        this.form.setLabelAlign(Form.ALIGN_RIGHT);
        this.form.setLabelsPosition(Form.POSITION_LEFT);
        
        this.itemPreviewPanel = new Panel("itemPreviewPanel", "item-preview-panel.htm");
        this.addControl(this.itemPreviewPanel);
        
        this.itemHistoryPanel = new Panel("itemHistoryPanel", "item-history-panel.htm");
        this.addControl(this.itemHistoryPanel);
        
        this.transformationPanel = new Panel("transformationPanel", "transformation-panel.htm");
        this.addControl(this.transformationPanel);
    }

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
                    this.item = this.getItemManager().fetchItem(itemId);
                    this.itemPreviewPanel.addModel("item", this.item);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            } else {
                throw new RuntimeException(getMessage("error-no-item"));
            }
            try {
                return getAuthorizationManager().canEditItem(this.item, this.collection, this.user);
            } catch (AuthorizationSystemException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    /**
     * Builds the form based on the item's configuration and definition.
     */
    public void onInit() {
        super.onInit();
        Collection<String> unrecognizedTypes = new HashSet<String>(this.item.getMetadata().getRepresentedFieldTypes());
        for (FieldConfiguration fieldConfig : this.collection.listFieldConfigurations()) {

            /* 
             * This block creates and adds the label which also serves
             * as a link to usage notes for the field. 
             */
            unrecognizedTypes.remove(fieldConfig.getFieldType());
            Panel helpPanel = new HelpPanel(fieldConfig);
            ClassTogglingLink helpLink = new ClassTogglingLink(fieldConfig.getFieldType() + "_label", (fieldConfig.getDisplayLabel() == null ? fieldConfig.getFieldType() : fieldConfig.getDisplayLabel()), (String) helpPanel.getModel().get("helpPanelId"), "helpHidden", "helpDisplayed");
            form.add(helpLink);

            /* 
             * This block creates and adds the field.  
             * If an error is encountered while creating the field, a 
             * dummy field is added expressing the error and preserving
             * the page layout 
             */
            try {
                UIField field = this.collection.newInstance(fieldConfig.getFieldType());
                FieldData currentValue = this.item.getMetadata().getFieldData(fieldConfig.getFieldType());
                field.setFieldData(currentValue);
                field.setDefaultValue(this.collection.getDefaultValue(fieldConfig.getFieldType()));
                form.add(field.asClickControl());

                /*
                 * Special handling for read-only fields that should be suppressed.
                 */
                if (fieldConfig.isReadOnly() && field.suppressIfEmptyAndReadOnly() && currentValue == null) {
                    form.remove(helpLink);
                    form.remove(field.asClickControl());
                    continue;
                }

            } catch (Throwable t) {
                form.add(new MissingField(fieldConfig, this.collection.getFieldDefinition(fieldConfig.getFieldType()), t));
            }
            
            /*
             * This block adds the help panel (which is hidden by default)
             * in it's own double-wide cell in the layout 
             */
            form.add(helpPanel, 2);
        }
        
        // Add unrecognized data information
        try {
            if (getAuthorizationManager().canManageCollection(collection, user)) {
                for (String fieldType : unrecognizedTypes) {
                    form.add(new Label(getMessage("label-unrecognized", fieldType)));
                    form.add(new UnrecognizedFieldDataContainer(this.item.getMetadata().getFieldData(fieldType)));
                }
            }
        } catch (AuthorizationSystemException ex) {
            throw new RuntimeException(ex);
        }

        // add autocompleter
        this.autoCompleter = new AutoCompleteAjaxTargetControl("auto_complete_target", this.collection);
        this.addControl(this.autoCompleter);

        // add hidden control fields
        this.controlFields = new ArrayList<HiddenField>();
        this.hiddenFieldNameToControlMap = new HashMap<String, HiddenField>();
        if (item.getControlFields() != null) {
            for (NameValuePair controlField : this.item.getControlFields()) {
                HiddenField hiddenControlField = new HiddenField("control_field_" + controlField.getName(), String.class);
                hiddenControlField.setValue(controlField.getValue());
                form.add(hiddenControlField);
                this.controlFields.add(hiddenControlField);
                this.hiddenFieldNameToControlMap.put(controlField.getName(), hiddenControlField);
            }
        }
        
        // Add a submission button
        Submit submitButton = new Submit("save", getMessage("form-save"), this, "onSave");
        form.add(submitButton);
        
        // Add a deletion link
        try {
            if (collection.getCollectionMetadata().allowRecordDeletion() && getAuthorizationManager().canRemoveItem(item, collection, user)) {
                Submit removeButton = new Submit("remove", getMessage("form-remove"), this, "onRemove");
                form.add(removeButton);
            }
        } catch (AuthorizationSystemException ex) {
            // log the error but don't expose collection editing
            LOGGER.error("Error checking user authorization!", ex);
        }
        
        form.setActionURL("edit-item.htm?id=" + this.getContext().getRequestParameter("id"));
        this.addControl(form);
        
        // add the historic versions if available
        if (getHistoryEnabledItemManager() != null) {
            HistoryEnabledItemManager manager = getHistoryEnabledItemManager();
            try {
                List<ViewVersionActionLink> historyLinks = new ArrayList<ViewVersionActionLink>();
                for (VersionInformation version : manager.getItemMetadataHistory(item.getId())) {
                    ViewVersionActionLink link = new ViewVersionActionLink(version, manager, collection);
                    link.setParameter(ITEM_ID_PARAM_NAME, item.getId());
                    historyLinks.add(link);
                    this.addControl(link);
                }
                this.itemHistoryPanel.addModel("historyLinks", historyLinks);
            } catch (RepositoryException ex) {
                throw new RuntimeException(ex);
            }
        } 
    }
    
    public List<Element> getHeadElements() { 
        // We use lazy loading to ensure the CSS import is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 

            CssImport cssImport = new CssImport("/css/edit-item.css"); 
            headElements.add(cssImport); 
        } 
        return headElements; 
    }
    
    /**
     * The method that performs the action of the "remove item" button.
     * The current implementation verifies that the current user can
     * perform a delete operation and that the collection allows it 
     * before saving and then deleting the object.  The save operation
     * is useful because in the even of an accidental deletion, if the
     * underlying system supports undeleting items the final copy will
     * include any changes made to the fields before the user deleted
     * the item.
     */
    public boolean onRemove() {
        try {
            if (collection.getCollectionMetadata().allowRecordDeletion() && getAuthorizationManager().canRemoveItem(item, collection, user)) {
                getItemManager().saveItemMetadata(item, user);
                getItemManager().removeItem(item, collection, user);
                setRedirect("display-collection.htm");
                return true;
            } else {
                errorMessage = getMessage("error-not-authorized-to-delete");
                return false;
            }
        } catch (AuthorizationSystemException ex) {
            throw new RuntimeException(ex);
        } catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        } catch (OptimisticLockingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public boolean onSave() {
        if (this.form.isValid()) {
            for (Control control : this.form.getControls()) {
                if (control instanceof UIField) {
                    UIField uiField = (UIField) control;
                    this.item.getMetadata().setFieldValue(uiField.getFieldType(), uiField.getFieldData());
                }
            }
            
            // Update the item to reflect the control fields from the original request
            List<NameValuePair> controlFields = this.item.getControlFields();
            if (controlFields != null) {
                controlFields.clear();
                for (HiddenField field : this.controlFields) {
                    controlFields.add(new NameValuePair(field.getName().substring("control_field_".length()), field.getValue()));
                }
            }
            try {
                this.getItemManager().saveItemMetadata(item, user);
                setRedirect("edit-item.htm?" + ITEM_ID_PARAM_NAME + "=" + item.getId());
            } catch (OptimisticLockingException ex) {
                this.errorMessage = getMessage("error-optimistic-locking", (ex.getMessage() != null ? "  (" + ex.getMessage() + ")" : ""));
            } catch (RepositoryException ex) {
                this.errorMessage = getMessage("error-save", (ex.getMessage() != null ? "  (" + ex.getMessage() + ")" : ""));
            }
        }
        return true;
    }

    public void onRender() {
        super.onRender();
        this.transformationPanel.addModel("transformations", collection.getTransformationConfigurations());
    }
    
    public AutoCompleteAjaxTargetControl getAutoCompleteAjaxTargetControl() {
        return this.autoCompleter;
    }

    /**
     * Implements AutoCompleteEnabled, though the current implementation of this
     * method does nothing. 
     */
    public void registerAutoCompleteField(VocabularySourceAutoCompleteTextField field) {
        
    }
    
    protected String getTitle() {
        return getMessage("title", item.getIdWithinCollection());
    }
    
}
