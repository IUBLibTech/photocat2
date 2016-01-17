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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.click.Control;
import org.apache.click.control.FileField;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Label;
import org.apache.click.control.Panel;
import org.apache.click.control.Submit;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;
import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.accesscontrol.AuthorizationSystemException;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.config.FileSubmissionStatus;
import edu.indiana.dlib.catalog.config.HistoryEnabledItemManager;
import edu.indiana.dlib.catalog.config.HistoryEnabledItemManager.VersionInformation;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.config.ItemMetadata;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.config.OptimisticLockingException;
import edu.indiana.dlib.catalog.config.RepositoryException;
import edu.indiana.dlib.catalog.config.VocabularySourceConfiguration;
import edu.indiana.dlib.catalog.config.impl.FileSubmitter;
import edu.indiana.dlib.catalog.fields.UIField;
import edu.indiana.dlib.catalog.fields.click.control.AutoCompleteEnabled;
import edu.indiana.dlib.catalog.fields.click.control.ClassTogglingLink;
import edu.indiana.dlib.catalog.fields.click.control.MissingField;
import edu.indiana.dlib.catalog.fields.click.control.UnrecognizedFieldDataContainer;
import edu.indiana.dlib.catalog.fields.click.control.ViewVersionActionLink;
import edu.indiana.dlib.catalog.fields.click.control.autocomplete.AutoCompleteAjaxTargetControl;
import edu.indiana.dlib.catalog.fields.click.control.autocomplete.VocabularySourceAutoCompleteTextField;
import edu.indiana.dlib.catalog.pages.collections.panel.FileUploadHelpPanel;
import edu.indiana.dlib.catalog.pages.collections.panel.HelpPanel;
import edu.indiana.dlib.catalog.search.SearchResults;
import edu.indiana.dlib.catalog.vocabulary.ManagedVocabularySource;
import edu.indiana.dlib.catalog.vocabulary.VocabularySource;
import edu.indiana.dlib.catalog.vocabulary.VocabularySourceInitializationException;
import edu.indiana.dlib.catalog.vocabulary.impl.DefaultVocabularyTerm;
import edu.indiana.dlib.catalog.vocabulary.impl.VocabularySourceFactory;

/**
 * The page that displays an item for the user to edit.
 */
public class EditItemPage extends CollectionPage implements AutoCompleteEnabled {
    
    private Logger LOGGER = Logger.getLogger(EditItemPage.class);
    
    public static final String ITEM_ID_PARAM_NAME = "id";
    
    public Item item;
    
    public Form form;
    
    public FileField fileUploadField;
    
    public Panel itemPreviewPanel;
    
    public Panel itemHistoryPanel;
    
    public Panel transformationPanel;
    
    private AutoCompleteAjaxTargetControl autoCompleter;
    
    private List<HiddenField> controlFields;
    
    private Map<String, HiddenField> hiddenFieldNameToControlMap;
    
    public EditItemPage() {
        form = new Form();
        form.setColumns(2);
        form.setLabelAlign(Form.ALIGN_LEFT);
        form.setLabelsPosition(Form.POSITION_LEFT);
        
        itemPreviewPanel = new Panel("itemPreviewPanel", "item-preview-panel.htm");
        addControl(itemPreviewPanel);
        
        itemHistoryPanel = new Panel("itemHistoryPanel", "item-history-panel.htm");
        addControl(itemHistoryPanel);
        
        transformationPanel = new Panel("transformationPanel", "transformation-panel.htm");
        addControl(transformationPanel);
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
                    item = getItemManager().fetchItemIncludingPrivateMetadata(itemId, collection);
                    itemPreviewPanel.addModel("item", item);
                    FileSubmitter fs = getItemManager().getFileSubmitter(collection);
                    itemPreviewPanel.addModel("file-submitter", fs);
                    if (fs != null && (fs.isFileSubmissionAvailable(item) || fs.isFileRemovalAvailable(item, item.getId())) && getAuthorizationManager().canManageCollection(collection, unit, user)) {
                        itemPreviewPanel.addModel("manageLink", "<a href=\"manage-files.htm?id=" + item.getId() + "\">" + getMessage("manage-files") + "</a>");
                    }
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            } else {
                throw new RuntimeException(getMessage("error-no-item"));
            }
            try {
                boolean allow = getAuthorizationManager().canEditItem(item, collection, unit, user);
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
    
    /**
     * Builds the form based on the item's configuration and definition.
     */
    public void onInit() {
        super.onInit();
        
        ItemManager im = getItemManager();
        
        /*
         * Add the id parameter, we can't just include this in the target because
         * for multipart form submission it isn't processed as expected.
         */
        form.add(new HiddenField("id", item.getId()));
        
        /* Add a file upload field if appropriate */
        FileSubmitter fs = getItemManager().getFileSubmitter(collection);
        FileSubmissionStatus status = fs.getFileSubmissionStatus(item);
        if (status.getStatusCode().equals(FileSubmissionStatus.Status.SUBMISSION_NOT_CONFIGURED) || status.getStatusCode().equals(FileSubmissionStatus.Status.INGESTED)) {
            // do nothing
        } else if (status.getStatusCode().equals(FileSubmissionStatus.Status.PENDING_SUBMISSION)) {
    		Panel fileUploadHelpPanel = new FileUploadHelpPanel("upload");
            ClassTogglingLink helpLink = new ClassTogglingLink("upload_label", getMessage("upload"), (String) fileUploadHelpPanel.getModel().get("helpPanelId"), "helpHidden", "helpDisplayed");
            form.add(helpLink);
    		fileUploadField = new FileField("upload");
    		fileUploadField.setLabel("");
    		form.add(fileUploadField);
    		form.add(fileUploadHelpPanel, 2);
    	} else {
    		Panel fileUploadHelpPanel = new FileUploadHelpPanel("upload");
            ClassTogglingLink helpLink = new ClassTogglingLink("upload_label", getMessage("upload"), (String) fileUploadHelpPanel.getModel().get("helpPanelId"), "helpHidden", "helpDisplayed");
            form.add(helpLink);
            String label = "";
            switch (status.getStatusCode()) {
                case PENDING_PROCESSING:
                    label = getMessage("status-pending-processing", format.date(status.getLastActionDate()));
                    break;
                case FILE_VALIDATION_ERROR:
                    label = getMessage("status-validation-error", format.date(status.getLastActionDate()));
                    break;
                case PENDING_INGEST:
                    label = getMessage("status-pending-ingest", format.date(status.getLastActionDate()));
                    break;
                case INGESTED:
                    label = getMessage("status-ingested", format.date(status.getLastActionDate()));
                    break;
                default:
                    // unreachable.. 
                    label = getMessage("status-not-configured");
                    break;
            }
    		form.add(new Label("upload_status", label));
    		form.add(fileUploadHelpPanel, 2);
        }
        
        Collection<String> unrecognizedTypes = new HashSet<String>(item.getMetadata().getRepresentedFieldTypes());
        for (FieldConfiguration fieldConfig : collection.listFieldConfigurations(true)) {

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
                UIField field = collection.newInstance(fieldConfig.getFieldType());
                FieldData currentValue = item.getMetadata().getFieldData(fieldConfig.getFieldType());
                field.setFieldData(currentValue);
                field.setDefaultValue(collection.getDefaultValue(fieldConfig.getFieldType()));
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
                form.add(new MissingField(fieldConfig, collection.getFieldDefinition(fieldConfig), t));
            }
            
            /*
             * This block adds the help panel (which is hidden by default)
             * in it's own double-wide cell in the layout 
             */
            form.add(helpPanel, 2);
        }
        
        // Add unrecognized data information
        try {
            if (getAuthorizationManager().canManageCollection(collection, unit, user)) {
                for (String fieldType : unrecognizedTypes) {
                    form.add(new Label(getMessage("label-unrecognized", fieldType)));
                    form.add(new UnrecognizedFieldDataContainer(item.getMetadata().getFieldData(fieldType)));
                }
            }
        } catch (AuthorizationSystemException ex) {
            throw new RuntimeException(ex);
        }

        // add autocompleter
        autoCompleter = new AutoCompleteAjaxTargetControl("auto_complete_target", collection);
        addControl(autoCompleter);

        // add hidden control fields
        controlFields = new ArrayList<HiddenField>();
        hiddenFieldNameToControlMap = new HashMap<String, HiddenField>();
        if (item.getControlFields() != null) {
            for (NameValuePair controlField : item.getControlFields()) {
                HiddenField hiddenControlField = new HiddenField("control_field_" + controlField.getName(), String.class);
                hiddenControlField.setValue(controlField.getValue());
                form.add(hiddenControlField);
                controlFields.add(hiddenControlField);
                hiddenFieldNameToControlMap.put(controlField.getName(), hiddenControlField);
            }
        }
        
        // Add a submission button
        Submit submitButton = new Submit("save", getMessage("form-save"), this, "onSave");
        form.add(submitButton);
        
        // Add the "save and next" button if there's a result set in the session that has this item and it isn't the last item
        SearchResults results = SearchPage.getCurrentSearchResults(getContext().getSession());
        boolean isInSearch = false;
        if (results != null) {
        	for (int i = 0; i < results.getResults().size(); i ++) {
        		if (results.getResults().get(i).getId().equals(item.getId()) && (i + results.getStartingIndex() < (results.getTotalResultCount() - 1))) {
        			form.add(new Submit("save-and-next", getMessage("form-save-and-next"), this, "onSaveAndNext"));
        			form.add(new Submit("save-and-return", getMessage("form-save-and-return"), this, "onSaveAndReturn"));
        			addModel("search", results);
        			addModel("result-index", String.valueOf(i + results.getStartingIndex() + 1));
        			isInSearch = true;
        		}
        	}
        }
        if (!isInSearch && im.isRecordCreationEnabled(collection) && im.getRequiredArgumentNames(collection).isEmpty()) {
            form.add(new Submit("save-and-new", getMessage("form-save-and-new"), this, "onSaveAndNew"));
        }
        
        form.setActionURL("edit-item.htm?" + ITEM_ID_PARAM_NAME + "=" + getContext().getRequestParameter(ITEM_ID_PARAM_NAME));
        addControl(form);
        
        // add the historic versions if available
        if (getHistoryEnabledItemManager() != null) {
            HistoryEnabledItemManager manager = getHistoryEnabledItemManager();
            try {
                List<ViewVersionActionLink> historyLinks = new ArrayList<ViewVersionActionLink>();
                for (VersionInformation version : manager.getItemMetadataHistory(item.getId())) {
                    ViewVersionActionLink link = new ViewVersionActionLink(version, manager, collection);
                    link.setParameter(ITEM_ID_PARAM_NAME, item.getId());
                    historyLinks.add(link);
                    addControl(link);
                }
                itemHistoryPanel.addModel("historyLinks", historyLinks);
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
    
    public boolean onSave() {
        if (form.isValid()) {
            for (Control control : form.getControls()) {
                if (control instanceof UIField) {
                    UIField uiField = (UIField) control;
                    item.getMetadata().setFieldValue(uiField.getFieldType(), uiField.getFieldData());
                }
            }
            
            // Update the item to reflect the control fields from the original request
            List<NameValuePair> controlFields = item.getControlFields();
            if (controlFields != null) {
                controlFields.clear();
                for (HiddenField field : this.controlFields) {
                    controlFields.add(new NameValuePair(field.getName().substring("control_field_".length()), field.getValue()));
                }
            }
            try {
                getItemManager().saveItemMetadata(item, collection, user);
                updateManagedVocabularySourcesForSavedRecord(item.getMetadata(), collection);
                if (fileUploadField != null) {
                	FileItem fileItem = fileUploadField.getFileItem();
                	if (fileItem != null && fileItem.getSize() > 0) {
	                	LOGGER.info(fileItem.getName() + " was uploaded...");
	                	getItemManager().getFileSubmitter(collection).submitFile(fileUploadField.getFileItem().getInputStream(), item);
                	} else {
                		LOGGER.info("No file uploaded.");
                	}
                }
                
                setRedirect("edit-item.htm?" + ITEM_ID_PARAM_NAME + "=" + item.getId());
            } catch (OptimisticLockingException ex) {
                errorMessage = getMessage("error-optimistic-locking", (ex.getMessage() != null ? "  (" + ex.getMessage() + ")" : ""));
            } catch (RepositoryException ex) {
                errorMessage = getMessage("error-save", (ex.getMessage() != null ? "  (" + ex.getMessage() + ")" : ""));
            } catch (IOException ex) {
            	errorMessage = getMessage("error-upload");
			}
        }
        return true;
    }
    
    /**
     * Goes through the saved record and for each field that's attached
     * to a vocabulary source that supports implicit updates, adds the 
     * saved values to that vocabulary source.
     * @param im the updated (and saved) ItemMetadata
     * @param config the CollectionConfiguration
     */
    private void updateManagedVocabularySourcesForSavedRecord(ItemMetadata im, CollectionConfiguration config) {
        for (FieldConfiguration fieldConfig : config.listFieldConfigurations(true)) {
            FieldData data = im.getFieldData(fieldConfig.getFieldType());
            List<VocabularySourceConfiguration> sources = fieldConfig.getVocabularySources();
            if (sources != null && !sources.isEmpty() && data != null) {
                for (VocabularySourceConfiguration sourceConfig : sources) {
                    try {
                        VocabularySource source = VocabularySourceFactory.getInstance().getVocabularySource(sourceConfig, config.getSourceDefinition(fieldConfig, sourceConfig.getType()), collection.getId());
                        if (source instanceof ManagedVocabularySource) {
                            ManagedVocabularySource msource = (ManagedVocabularySource) source;
                            if (msource.isImplicitlyMaintained()) {
                                // alright, now we know it's implicitly maintained and we have access to
                                // the source, we need to find each part that is bound to this source
                                // and store it
                                for (List<NameValuePair> parts : data.getParts()) {
                                    String enteredValue = null;
                                    boolean isForThisSource = sourceConfig.getAuthorityBinding() == null;
                                    for (NameValuePair part : parts) {
                                        if (part.getName().equals(sourceConfig.getValueBinding())) {
                                            enteredValue = part.getValue();
                                        } else if (!isForThisSource && part.getName().equals(sourceConfig.getAuthorityBinding()) && part.getValue().equals(sourceConfig.getId())) {
                                            isForThisSource = true;
                                        }
                                    }
                                    if (isForThisSource && enteredValue != null && enteredValue.trim().length() != 0) {
                                        msource.addTerm(new DefaultVocabularyTerm(enteredValue, enteredValue, source.getId()));
                                    }
                                    
                                }
                            }
                        }
                    } catch (IOException ex) {
                        LOGGER.warn("Unable to get vocabulary source of type " + sourceConfig.getType() + " for field " + fieldConfig.getFieldType() + ".", ex);
                    } catch (VocabularySourceInitializationException ex) {
                        LOGGER.warn("Unable to get vocabulary source of type " + sourceConfig.getType() + " for field " + fieldConfig.getFieldType() + ".", ex);
                    }
                }
            }
        }
    }
    
    public boolean onSaveAndNext() {
    	onSave();
    	if (!form.isValid() || errorMessage != null) {
    		// the save failed...
    		return true;
    	} else {
    		// update the redirect to go to the next item
    		SearchResults results = SearchPage.getCurrentSearchResults(getContext().getSession());
            if (results != null) {
            	for (int i = 0; i < results.getResults().size(); i ++) {
            		if (results.getResults().get(i).getId().equals(item.getId()) && (i + results.getStartingIndex() < (results.getTotalResultCount() - 1))) {
            			if (i + 1 < results.getResults().size()) {
            				setRedirect("edit-item.htm?" + ITEM_ID_PARAM_NAME + "=" + results.getResults().get(i + 1).getId());
            			} else {
            				results = SearchPage.getNextPage(getContext().getSession(), getSearchManager());
            				if (results != null && !results.getResults().isEmpty()) {
            					setRedirect("edit-item.htm?" + ITEM_ID_PARAM_NAME + "=" + results.getResults().get(0).getId());
            				}
            			}
            		}
            	}
            }
    	}
    	return true;
    }
    
    public boolean onSaveAndReturn() {
    	onSave();
    	if (!form.isValid() || errorMessage != null) {
    		// the save failed...
    		return true;
    	} else {
    		// update the redirect to go to the search results
    		setRedirect("search.htm");
    	}
    	return true;
    }
    
    public boolean onSaveAndNew() {
        onSave();
        if (!form.isValid() || errorMessage != null) {
            // the save failed...
            return true;
        } else {
            try {
                setRedirect("edit-item.htm?id=" + getItemManager().createNewItem(collection, user, null));
                return true;
            } catch (RepositoryException ex) {
                throw new RuntimeException(ex);
            }            
        }
    }

    public void onRender() {
        super.onRender();
        transformationPanel.addModel("transformations", collection.getTransformationConfigurations());
    }
    
    public AutoCompleteAjaxTargetControl getAutoCompleteAjaxTargetControl() {
        return autoCompleter;
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
