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
import java.util.ArrayList;
import java.util.List;

import org.apache.click.Control;
import org.apache.click.control.Field;
import org.apache.click.control.Form;
import org.apache.click.control.Option;
import org.apache.click.control.Panel;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;

import edu.indiana.dlib.catalog.asynchronous.UserOperationManager;
import edu.indiana.dlib.catalog.batch.Batch;
import edu.indiana.dlib.catalog.batch.BatchManager;
import edu.indiana.dlib.catalog.batch.impl.EditBatchOperation;
import edu.indiana.dlib.catalog.batch.impl.EditBatchProcess;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldData;
import edu.indiana.dlib.catalog.fields.UIField;
import edu.indiana.dlib.catalog.fields.click.control.AutoCompleteEnabled;
import edu.indiana.dlib.catalog.fields.click.control.ClassTogglingLink;
import edu.indiana.dlib.catalog.fields.click.control.MissingField;
import edu.indiana.dlib.catalog.fields.click.control.autocomplete.AutoCompleteAjaxTargetControl;
import edu.indiana.dlib.catalog.fields.click.control.autocomplete.VocabularySourceAutoCompleteTextField;
import edu.indiana.dlib.catalog.pages.collections.panel.HelpPanel;

/**
 * <p>
 *   A page that exposes metadata editing for all the records in a batch.
 * </p>
 * <p>
 *   The process of kicking off the update of all records in the batch has
 *   two steps, each with their own forms.  This page encapsulates that 
 *   process, but could just as easily be broken into two separate pages.
 * </p>
 * <p> 
 *   The first step presents all the read/write fields configured for 
 *   the current collection to the user for data entry. 
 * </p>
 * <p>
 *   The second step presents just those fields for which values were 
 *   entered and prompts the user for overwrite rules to deal with 
 *   the possibility of values already present in the specified fields.
 * </p>
 * {@see edu.indiana.dlib.catalog.batch.impl.EditBatchProcess}
 */
public class EditBatchPage extends CollectionPage implements AutoCompleteEnabled {

    private static final String BATCH_UPDATE_PROCESS_SESSION_ATTRIBUTE_NAME = "batch-update";  
    
    public Batch batch;
    
    public String batchIdStr;
    
    public Form form;
    
    public String explanation;
    
    private AutoCompleteAjaxTargetControl autoCompleter;
    
    private EditBatchProcess process;
    
    public void onInit() {
        super.onInit();
        BatchManager bm = getBatchManager();
        if (bm != null) {
            batchIdStr = getContext().getRequestParameter("batchId");
            try {
                batch = bm.openBatch(user.getUsername(), collection.getId(), Integer.parseInt(batchIdStr));

                process = (EditBatchProcess) getContext().getSession().getAttribute(BATCH_UPDATE_PROCESS_SESSION_ATTRIBUTE_NAME);
                
                form = new Form("form");
                form.setLabelAlign(Form.ALIGN_RIGHT);
                form.setLabelsPosition(Form.POSITION_LEFT);
                form.setActionURL("edit-batch.htm?batchId=" + batchIdStr);
                if (process == null) {
                    // step one, get field values
                    form.setColumns(2);
                    for (FieldConfiguration fieldConfig : this.collection.listFieldConfigurations()) {
    
                        /* 
                         * This block creates and adds the label which also serves
                         * as a link to usage notes for the field. 
                         */
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
                            form.add(field.asClickControl());
    
                            /*
                             * Special handling for read-only fields.
                             */
                            if (fieldConfig.isReadOnly()) {
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
                    
                    // add autocompleter
                    this.autoCompleter = new AutoCompleteAjaxTargetControl("auto_complete_target", this.collection);
                    this.addControl(this.autoCompleter);
    
                    // Add a submission button
                    Submit submitButton = new Submit("update", getMessage("form-go-to-step-two"), this, "onSubmitStepOne");
                    form.add(submitButton);
                    form.add(new Submit("cancel", getMessage("cancel"), this, "onCancel"));
                    addControl(form);
                    
                    explanation = getMessage("batch-step-one-explanation", batch.getSize());
                } else {           
                    // step 2
                    for (String type : process.getOperation().getRepresentedFieldTypes()) {
                        FieldConfiguration config = collection.getFieldConfiguration(type);
                        Select select = new Select(type, getMessage("label-select-action-html", config.getDisplayLabel()));
                        select.add(new Option("2", getMessage("select-skip")));
                        select.add(new Option("3", getMessage("select-overwrite")));
                        if (config.isRepeatable()) {
                            select.add(new Option("4", getMessage("select-merge")));
                        }
                        select.add(new Option("1", getMessage("select-prompt")));
                        form.add(select);
                    }
                    
                    // Add a submission button
                    Submit submitButton = new Submit("update", getMessage("form-update-x-records", batch.getSize()), this, "onSubmitStepTwo");
                    form.add(submitButton);
                    form.add(new Submit("cancel", getMessage("cancel"), this, "onCancel"));
                    addControl(form);
                    
                    explanation = getMessage("batch-step-two-explanation");
                }
            } catch (NumberFormatException e) {
                // fall through, leaving the batch unloaded so the page can display
                // an error.
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            
        }
    }
    
    public boolean onSubmitStepOne() {
        if (form.isValid()) {
            List<FieldData> fieldDataList = new ArrayList<FieldData>();
            for (Control control : form.getControls()) {
                if (control instanceof UIField) {
                    UIField uiField = (UIField) control;
                    FieldData data = uiField.getFieldData();
                    
                    if (!data.getParts().isEmpty()) {
                        LOGGER.debug("Including field " + data.getFieldType() + " with value \"" + uiField.getValueSummary() + "\".");
                        fieldDataList.add(uiField.getFieldData());
                    }
                }
            }
            EditBatchOperation op = new EditBatchOperation(batch.listItemIds(), fieldDataList, getItemManager(), user, collection);
            getContext().getSession().setAttribute(BATCH_UPDATE_PROCESS_SESSION_ATTRIBUTE_NAME, new EditBatchProcess(op));

            // redirect for step two in the process
            setRedirect("edit-batch.htm?batchId=" + this.batchIdStr);
            return false;
        } else {
            return true;
        }
    }
    
    public boolean onSubmitStepTwo() {
        // parse out the values
        EditBatchOperation operation = process.getOperation();
        for (Field field : form.getFieldList()) {
            if (field instanceof Select) {
                Select select = (Select) field;
                String fieldType = select.getName();
                LOGGER.debug(fieldType + ": " + select.getValue());
                if (select.getValue().equals("2")) {
                    operation.setFieldTypesToSkip(fieldType);
                } else if (select.getValue().equals("3")) {
                    operation.setOvewriteField(fieldType);
                } else if (select.getValue().equals("4")) {
                    operation.setRepeatableFieldTypesToCombine(fieldType);
                }
            }
        }
        UserOperationManager opMan = UserOperationManager.getOperationManager(getContext().getRequest(), user.getUsername());
        opMan.queueOperation(operation); 
        getContext().getSession().removeAttribute(BATCH_UPDATE_PROCESS_SESSION_ATTRIBUTE_NAME);
        setRedirect("batches.htm");
        return false;
    }
    
    public boolean onCancel() {
        getContext().getSession().removeAttribute(BATCH_UPDATE_PROCESS_SESSION_ATTRIBUTE_NAME);
        setRedirect("batches.htm");
        return false;
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
    
    
}
