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

import edu.indiana.dlib.catalog.accesscontrol.AuthorizationSystemException;
import edu.indiana.dlib.catalog.asynchronous.Dialog;
import edu.indiana.dlib.catalog.asynchronous.UserOperationManager;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.dataimport.DataImportProcess;
import edu.indiana.dlib.catalog.dataimport.FieldMapping;
import edu.indiana.dlib.catalog.dataimport.Metadata;
import edu.indiana.dlib.catalog.dataimport.operations.RecordImportOperation;
import edu.indiana.dlib.catalog.fields.click.control.uifield.RobustDateUIField;

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
    
    public String inprogress;
    
    private List<Submit> interactionSubmitList;
    
    /**
     * Determines whether the user is authorized to import content
     * to this collection.  To do so, the user must be registered
     * as a collection manager, not just a cataloger.
     */
    public boolean onSecurityCheck() {
        if (!super.onSecurityCheck()) {
            return false;
        } else {
            // Determine if the user can manage that collection
            try {
                boolean allow = getAuthorizationManager().canManageCollection(collection, unit, user);
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
            // stage two, get the mapping
            List<Option> fieldPartOptions = new ArrayList<Option>();
            fieldPartOptions.add(new Option(getMessage("mapping-skip")));
            for (FieldConfiguration fieldConf : collection.listFieldConfigurations(true)) {
                FieldDefinition def = collection.getFieldDefinition(fieldConf);
                if (collection.getEnabledPartNames(fieldConf).size() == 1 && !fieldConf.isReadOnly()) {
                    fieldPartOptions.add(new Option(def.getType(), fieldConf.getDisplayLabel()));
                } else if (collection.getFieldDefinition(fieldConf).getJavaImplementation().getJavaClassName().equals(RobustDateUIField.class.getName())) {
                    fieldPartOptions.add(new Option(def.getType(), fieldConf.getDisplayLabel()));
                } else {
                    // TODO: add support for these complex field
                    // for now, they won't be an option for import
                    LOGGER.debug("There are " + collection.getBrowsablePartNames(fieldConf).size() + " browsable part names for field " + fieldConf.getDisplayLabel() + ".");
                }
            }
                
            // the file has been parsed, display the metadata
            mappingForm = new Form("mappingForm");
            Metadata m = process.getRecords().getMetadata();
            
            Select idField = new Select("idField", getMessage("id-field-label"));
            idField.add(new Option("unspecified", ""));
            idField.setRequired(true);
            // the other options will be added as we cycle through the fields below...
            mappingForm.add(idField);
            
            for (int i = 0; i < m.getFieldCount(); i ++) {
                if (m.getFieldName(i) != null && m.getFieldName(i).trim().length() > 0) {
                    idField.add(new Option(m.getFieldName(i)));
                    
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
                    
                    Select conflictResolution = new Select("conflictResolution" + i);
                    conflictResolution.add(new Option("skip", getMessage("skip")));
                    conflictResolution.add(new Option("overwrite", getMessage("overwrite")));
                    conflictResolution.add(new Option("add", getMessage("add")));
                    mappingForm.add(conflictResolution);
                }
            }
            mappingForm.add(new Submit("Map", getMessage("submit-mapping"), this, "onImport"));
            mappingForm.add(new Submit("cancel", getMessage("submit-cancel"), this, "onCancel"));
            addControl(mappingForm);
        } else {
            // stage 3, either in progress, or complete
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
            } else {
                inprogress = getMessage("in-progress");
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
        if (!mappingForm.isValid()) {
            return true;
        } else {
            FieldMapping mapping = new FieldMapping(collection);
            mapping.setIdField(mappingForm.getField("idField").getValue());
            
            String recordFieldName = null;
            for (Field field : mappingForm.getFieldList()) {
                if (field instanceof TextField) {
                    recordFieldName = field.getValue();
                }
                if (field instanceof Select && field.getName().startsWith("mappedLocation")) {
                    // Above we named the fields "mappedLocation1", "mappedLocation2", ...
                    // We also had the conflict rule as "conflictResolution1", "conflictResolution2" ...
                    // We can use this to get the name of the field that has the conflict resolution
                    // information for this field.
                    int offset = Integer.parseInt(field.getName().replace("mappedLocation", ""));
                    String crStr = mappingForm.getFieldValue("conflictResolution" + offset);
                    FieldMapping.ConflictResolution cr = FieldMapping.ConflictResolution.RETAIN_ORIGINAL;
                    if (crStr.equals("add")) {
                        cr = FieldMapping.ConflictResolution.ADD_ADDITIONAL_VALUE;
                    } else if (crStr.equals("overwrite")) {
                        cr = FieldMapping.ConflictResolution.OVERWRITE;
                    }
                    
                    String fieldType = field.getValue();
                    LOGGER.info(recordFieldName + " --> " + fieldType);
                    FieldConfiguration conf = collection.getFieldConfiguration(fieldType);
                    FieldDefinition def = collection.getFieldDefinition(conf);
                    // Note: the following allows hacked POSTs to create a mapping to an unexpected field.
                    //       this is only a minor concern, because the user is already authorized to screw
                    //       up the records as much as he/she wishes.  Also, the addSimpleFieldMapping() 
                    //       will choke on most nonsensical values.
                    if (def != null) {
                        if (def.getJavaImplementation().getJavaClassName().equals(RobustDateUIField.class.getName())) {
                            mapping.addFieldPartMapping(recordFieldName, def, RobustDateUIField.ENTERED_DATE, cr);
                        } else {
                            List<String> partNames = collection.getBrowsablePartNames(conf);
                            if (partNames.size() == 1) {
                                mapping.addFieldPartMapping(recordFieldName, def, partNames.get(0), cr);
                            } else {
                                throw new RuntimeException(conf.getDisplayLabel() + " has multiple possible parts!");
                            }
                        }
                    }
                }
            }
            RecordImportOperation op = new RecordImportOperation(getMessage("import-operation-name", process.getOriginalFilename()), process.getRecords(), mapping, getItemManager(), user, collection, "import.htm");
            process.setRecordImportOperation(op);
            UserOperationManager opMan = UserOperationManager.getOperationManager(getContext().getRequest(), user.getUsername());
            opMan.queueOperation(op);
            setRedirect("display-collection.htm");
            return false;
        }
    }
    
    public boolean onCancel() {
        getContext().getSession().removeAttribute(DATA_IMPORT_PROCESS_SESSION_ATTRIBUTE_NAME);
        setRedirect("import.htm");
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
        setRedirect("import.htm");
        return false;
    }
    
}
