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

import java.util.ArrayList;
import java.util.List;

import org.apache.click.control.Checkbox;
import org.apache.click.control.Field;
import org.apache.click.control.FieldSet;
import org.apache.click.control.FileField;
import org.apache.click.control.Form;
import org.apache.click.control.Option;
import org.apache.click.control.Reset;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.TextArea;
import org.apache.click.control.TextField;
import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.config.CollectionConfigurationData;
import edu.indiana.dlib.catalog.config.CollectionMetadata;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.ConfigurationManagerException;
import edu.indiana.dlib.catalog.config.Definitions;
import edu.indiana.dlib.catalog.config.FieldConfiguration;
import edu.indiana.dlib.catalog.config.FieldDefinition;
import edu.indiana.dlib.catalog.config.impl.DefaultCollectionConfigurationData;

/**
 * A page that allows the very basic information about a collection
 * to be specified.  This includes field selection and basic metadata
 * entry.  
 * 
 * For field customization another page is required.
 * 
 * 
 * A page that allows administrative users to manage certain
 * aspects of a collection.  Specific functionality that
 * is needed includes:
 * 1.  Setting/updating the basic metadata
 * 2.  Adding and configuring fields that are already defined.
 * 3.  Removing fields (at least those that can be added again conveniently)
 * 
 * The phases 
 */
public class BasicCollectionSetupPage extends CollectionAdminPage {

    private Form form;
    
    public void onInit() {
        super.onInit();
        
        ConfigurationManager cm = getConfigurationManager();
        
        form = new Form("form");
        FieldSet metadataFieldSet = new FieldSet("metadata");

        TextField collectionNameField = new TextField("fullName", getMessage("fullName"));
        collectionNameField.setValue(collection.getCollectionMetadata().getFullName());
        collectionNameField.setRequired(true);
        metadataFieldSet.add(collectionNameField);
        
        TextField shortNameField = new TextField("shortName", getMessage("shortName"));
        shortNameField.setValue(collection.getCollectionMetadata().getShortName());
        shortNameField.setRequired(true);
        metadataFieldSet.add(shortNameField);
        
        TextArea descriptionField = new TextArea("description", getMessage("description"));
        descriptionField.setValue(collection.getCollectionMetadata().getDescription());
        descriptionField.setRequired(true);
        metadataFieldSet.add(descriptionField);
        
        TextField homepageUrlField = new TextField("homepageUrl", getMessage("homepageUrl"));
        homepageUrlField.setValue(collection.getCollectionMetadata().getHomepageUrl());
        metadataFieldSet.add(homepageUrlField);
        
        TextField homepageTitleField = new TextField("homepageTitle", getMessage("homepageTitle"));
        homepageTitleField.setValue(collection.getCollectionMetadata().getHomepageTitle());
        metadataFieldSet.add(homepageTitleField);
        
        TextField emailField = new TextField("email", getMessage("email"));
        emailField.setValue(collection.getCollectionMetadata().getCollectionManagerAddress());
        metadataFieldSet.add(emailField);
        
        TextArea termsOfUseField = new TextArea("termsOfUse", getMessage("termsOfUse"));
        termsOfUseField.setValue(collection.getCollectionMetadata().getTermsOfUse());
        metadataFieldSet.add(termsOfUseField);
        
        FileField iconImageFile = new FileField("iconUrl", getMessage("iconUrl"));
        metadataFieldSet.add(iconImageFile);
        
        Checkbox published = new Checkbox("published", getMessage("published"));
        published.setChecked(collection.isPublic());
        metadataFieldSet.add(published);
        
        form.add(metadataFieldSet);
        
        
        FieldSet fieldSelectFieldSet = new FieldSet("fields");
        int i = 0;
        for (Definitions d : cm.listKnownDefinitionSets()) {
            Select fieldSelect = new Select("fields-" + (i++), getMessage("select-fields-label", d.getId()));
            for (String fieldType : d.listFieldTypes()) {
                FieldDefinition def = d.getFieldDefinition(fieldType);
                fieldSelect.add(new Option(fieldType, def.getDefaultConfiguration().getDisplayLabel()));
            }
            fieldSelect.setMultiple(true);
            fieldSelect.setSize(Math.max(1, Math.min(10, fieldSelect.getOptionList().size())));
            fieldSelectFieldSet.add(fieldSelect);
            
            List<String> configuredFieldTypes = new ArrayList<String>();
            for (FieldConfiguration field : collection.listFieldConfigurations(true)) {
                if (field.getDefinitionId().equals(d.getId())) {
                    configuredFieldTypes.add(field.getFieldType());
                }
            }
            fieldSelect.setSelectedValues(configuredFieldTypes);
        }
        form.add(fieldSelectFieldSet);
        
        Reset reset = new Reset("reset");
        reset.setTitle(getMessage("undo-changes"));
        form.add(reset);
        
        Submit submit = new Submit("submit", getMessage("submit"), this, "onSubmit");
        form.add(submit);
        
        addControl(form);
    }
    
    public boolean onSubmit() {
        Logger.getLogger(getClass()).debug("submit");
        if (form.isValid()) {
            // do a little extra validation
            if ((form.getFieldValue("homepageUrl").length() > 0) != (form.getFieldValue("homepageTitle").length() > 0)) {
                form.setError(getMessage("homepage-spec-invalid"));
                return true;
            }

            try {
                ConfigurationManager cm = getConfigurationManager();
                
                // clone the configuration (so our potentially unworkable updates don't muck with the shared object)
                CollectionConfigurationData cdata = new DefaultCollectionConfigurationData(collection);
    
                String iconUrl = null;
                
                // handle the icon image
                FileField fileField = (FileField) form.getField("iconUrl");
                if (fileField != null) {
                    FileItem fileItem = fileField.getFileItem();
                    if (fileItem != null && fileItem.getSize() > 0) {
                        iconUrl = cm.storeCollectionFile(collection, fileItem.getInputStream(), "image/jpeg", "icon.jpg", true).toString();
                    }
                }
                
                // handle the basic metadata
                // for required fields we can just copy the form value
                // for optional fields, we need to make sure an actual value was entered (not just "")
                CollectionMetadata cmdata = cdata.getCollectionMetadata();
                cmdata.setFullName(form.getFieldValue("fullName"));
                cmdata.setShortName(form.getFieldValue("shortName"));
                cmdata.setDescription(form.getFieldValue("description"));
                
                String homepageUrl = form.getFieldValue("homepageUrl");
                String homepageTitle = form.getFieldValue("homepageTitle");
                if (homepageUrl.length() > 0) {
                    cmdata.setHomepageUrl(homepageUrl);
                    cmdata.setHomepageTitle(homepageTitle);
                }
                
                String email = form.getFieldValue("email");
                if (email.length() > 0) {
                    cmdata.setCollectionManagerAddress(email);
                }
                
                String termsOfUse = form.getFieldValue("termsOfUse");
                if (termsOfUse.length() > 0) {
                    cmdata.setTermsOfUse(termsOfUse);
                }
                
                if (((Checkbox) form.getField("published")).isChecked()) {
                    cdata.setIsPublic(true);
                } else {
                    cdata.setIsPublic(false);
                }
                
                if (iconUrl != null) {
                    cmdata.setIcondUrl(iconUrl);
                }
                
                // parse the new list of field types
                List<String> newFieldTypeList = new ArrayList<String>();
                for (int i = 0; ; i ++) {
                    Field field = form.getField("fields-" + i);
                    if (field == null) {
                        break;
                    }
                    Select s = (Select) field;
                    if (getContext().isPost() && getContext().getRequest().getParameter(s.getName()) != null) {
                        // check if any value was submitted
                        for (Object fieldType : s.getSelectedValues()) {
                            newFieldTypeList.add((String) fieldType);
                        }
                    }
                }
                // remove any removed fields
                for (FieldConfiguration fc : new ArrayList<FieldConfiguration>(cdata.getFieldConfigurations())) {
                    if (!newFieldTypeList.contains(fc.getFieldType())) {
                        cdata.getFieldConfigurations().remove(fc);
                        Logger.getLogger(getClass()).debug("Removing field \"" + fc.getFieldType() + "\".");
                    } else {
                        //Logger.getLogger(getClass()).debug("Retaining field \"" + fc.getFieldType() + "\".");
                    }
                }
                // add any new fields
                for (String type : newFieldTypeList) {
                    for (Definitions defs : cm.listKnownDefinitionSets()) {
                        FieldDefinition def = defs.getFieldDefinition(type);
                        if (def != null) {
                            // copy the default field from the definition
                            FieldConfiguration fc = new FieldConfiguration(def.getDefaultConfiguration());
                            if (!cdata.getFieldConfigurations().contains(fc)) {
                                cdata.getFieldConfigurations().add(fc);
                                Logger.getLogger(getClass()).debug("Adding field \"" + fc.getFieldType() + "\".");
                            } else {
                                //Logger.getLogger(getClass()).debug("Retaining field \"" + fc.getFieldType() + "\".");
                            }
                        }
                    }
                }
                
                cm.storeConfiguration(cdata);
                setRedirect("manage-collection.htm");
                return false;
            } catch (Throwable t) {
                Logger.getLogger(getClass()).error("Error updating collection! ("+ collection.getId() + ")", t);
                if (t.getLocalizedMessage() != null) {
                    form.setError(t.getLocalizedMessage());
                } else {
                    form.setError(t.getClass().getSimpleName());
                }
                return true;
            }
        } else {
            return true;
        }
    }
    
}
