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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.click.control.Field;
import org.apache.click.control.FileField;
import org.apache.click.control.Form;
import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;

import edu.indiana.dlib.catalog.asynchronous.Dialog;
import edu.indiana.dlib.catalog.asynchronous.UserOperationManager;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.dataimport.DataImportProcess;
import edu.indiana.dlib.catalog.dataimport.FieldMapping;
import edu.indiana.dlib.catalog.dataimport.Metadata;
import edu.indiana.dlib.catalog.dataimport.operations.RecordImportOperation;

/**
 * <p>
 *   A page that walks a user through the interactions involved 
 *   in importing metadata from a spreadsheet.
 * </p>
 * <strong>
 *   This interaction requires more fine tuning and isn't yet
 *   linked from anywhere
 * </strong>
 * <p>
 *   The steps in this process are:
 *   <ol>
 *     <li>upload a file</li>
 *     <li>
 *       Answer questions about the detected format, or receive
 *       an error message if the format is unknown
 *     </li>
 *     <li>
 *       set up the mapping and optional transformations with 
 *       options about what to do in the event of errors
 *     </li>
 *     <li>
 *       Display processing progress somewhere in the UI
 *     </li>
 *   </ul>
 * </p>
 */
public class ImportPage extends CollectionPage {

    private static final long serialVersionUID = 1L;
    
    private static final String DATA_IMPORT_PROCESS_SESSION_ATTRIBUTE_NAME = "data-import-process";  

    public DataImportProcess process;

    private FileField fileField;
    
    private Form mappingForm;
    
    public RecordImportOperation operation;
    
    public Dialog dialog;
    
    private List<Submit> interactionSubmitList;
    
    public void onInit() {
        super.onInit();
        // get the current process
        process = (DataImportProcess) getContext().getSession().getAttribute(DATA_IMPORT_PROCESS_SESSION_ATTRIBUTE_NAME);
        if (process == null) {
            process = new DataImportProcess();
            getContext().getSession().setAttribute(DATA_IMPORT_PROCESS_SESSION_ATTRIBUTE_NAME, process);
        }
        
        if (process.getFile() == null) {
            // stage one (either before or after post)
            Form uploadForm = new Form("uploadForm");
            fileField = new FileField("file", getMessage("file"));
            uploadForm.add(fileField);
            uploadForm.add(new Submit("upload", getMessage("submit-file"), this, "onSubmitFile"));
            uploadForm.add(new Submit("cancel", getMessage("submit-cancel"), this, "onCancel"));
            addControl(uploadForm);
        } else if (process.getRecords() == null) {
            // there was some error, the file should have been parsed
            getContext().getSession().removeAttribute(DATA_IMPORT_PROCESS_SESSION_ATTRIBUTE_NAME);
            addModel("errorMessage", getMessage("unrecognized-data"));
        } else if (process.getRecordImportOperation() == null) {
            List<Option> fieldPartOptions = new ArrayList<Option>();
            fieldPartOptions.add(new Option("-SKIP-"));
            fieldPartOptions.add(new Option("[ID]"));
            for (FieldConfiguration fieldConf : collection.listFieldConfigurations()) {
                FieldDefinition def = collection.getFieldDefinition(fieldConf.getFieldType());
                List<String> attributes = def.getDataSpecification().getValidAttributeNames();
                List<String> parts = def.getDataSpecification().getValidPartNames();
                if ((attributes == null || attributes.isEmpty()) && parts.size() == 1) {
                    fieldPartOptions.add(new Option(def.getType(), fieldConf.getDisplayLabel()));
                } else {
                    // TODO: add support for this complex field
                }
            }
                
            // the file has been parsed, display the metadata
            mappingForm = new Form("mappingForm");
            Metadata m = process.getRecords().getMetadata();
            for (int i = 0; i < m.getFieldCount(); i ++) {
                TextField originalName = new TextField("originalName" + i);
                originalName.setReadonly(true);
                originalName.setValue(m.getFieldName(i));
                mappingForm.add(originalName);
                
                Select mappedLocation = new Select("mappedLocation" + i);
                for (Option option : fieldPartOptions) {
                    mappedLocation.add(option);
                    if (option.getLabel().equals(m.getFieldName(i))) {
                        mappedLocation.setValue(option.getValue());
                    }
                }
                mappingForm.add(mappedLocation);
            }
            mappingForm.add(new Submit("Map", getMessage("submit-mapping"), this, "onImport"));
            mappingForm.add(new Submit("cancel", getMessage("submit-cancel"), this, "onCancel"));
            addControl(mappingForm);
        } else {
            operation = process.getRecordImportOperation();
            int i = 0;
            if (operation.requiresUserInteraction()) {
                dialog = operation.getInteractionDialog();
                Form confirmForm = new Form("confirmForm");
                interactionSubmitList = new ArrayList<Submit>();
                for (String response : dialog.getSuggestedResponses()) {
                    Submit submit = new Submit("response_" + (i ++), response, this, "onConfirm");
                    interactionSubmitList.add(submit);
                    confirmForm.add(submit);
                }
                addControl(confirmForm);
            }
        }
        
    }
    
    public boolean onSubmitFile() {
        // save the file to a temporary file
        try {
            File tempFile = File.createTempFile("photocat-", "-upload.data");
            DataImportProcess.writeStreamToFile(fileField.getFileItem().getInputStream(), tempFile);
            process.setFile(tempFile, fileField.getFileItem().getName());
            setRedirect("import.htm");
            return false;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
    
    public boolean onImport() {
        int idMappingCount = 0;
        FieldMapping mapping = new FieldMapping();
        String recordFieldName = null;
        for (Field field : mappingForm.getFieldList()) {
            if (field instanceof TextField) {
                recordFieldName = field.getValue();
            }
            if (field instanceof Select) {
                if ("[ID]".equals(field.getValue())) {
                    idMappingCount ++;
                    mapping.setIdField(recordFieldName);
                } else {
                    String fieldType = field.getValue();
                    LOGGER.info(recordFieldName + " --> " + fieldType);
                    FieldDefinition def = collection.getFieldDefinition(fieldType);
                    // Note: the following allows hacked POSTs to create a mapping to an unexpected field.
                    //       this is only a minor concern, because the user is already authorized to screw
                    //       up the records as much as he/she wishes.  Also, the addSimpleFieldMapping() 
                    //       will choke on most non-sensical values.
                    if (def != null) {
                        mapping.addSimpleFieldMapping(recordFieldName, def);
                    }
                }
            }
        }
        if (idMappingCount == 0) {
            mappingForm.setError(getMessage("error-missing-id"));
        } else if (idMappingCount > 1) {
            mappingForm.setError(getMessage("error-multiple-id", String.valueOf(idMappingCount)));
        } else {
            RecordImportOperation op = new RecordImportOperation(getMessage("import-operation-name", process.getOriginalFilename()), process.getRecords(), mapping, getItemManager(), user, "import.htm");
            process.setRecordImportOperation(op);
            UserOperationManager opMan = UserOperationManager.getOperationManager(getContext().getRequest(), user.getUsername());
            opMan.queueOperation(op);
            setRedirect("display-collection.htm");
            return false;
        }
        return true;
    }
    
    public boolean onCancel() {
        getContext().getSession().removeAttribute(DATA_IMPORT_PROCESS_SESSION_ATTRIBUTE_NAME);
        setRedirect("batches.htm");
        return false;
    }
    
    public boolean onConfirm() {
        Dialog d = operation.getInteractionDialog();
        for (int i = 0; i < interactionSubmitList.size(); i ++) {
            Submit button = interactionSubmitList.get(i);
            if (button.isClicked()) {
                operation.respondToInteractionDialog(d, button.getValue());
            }
        }
        getContext().getSession().removeAttribute(DATA_IMPORT_PROCESS_SESSION_ATTRIBUTE_NAME);
        setRedirect("batches.htm");
        return false;
    }
    
}
