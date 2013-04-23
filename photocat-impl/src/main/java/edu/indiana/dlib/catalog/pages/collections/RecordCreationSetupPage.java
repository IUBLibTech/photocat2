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
import java.net.MalformedURLException;
import java.util.Properties;

import org.apache.click.control.FieldSet;
import org.apache.click.control.Form;
import org.apache.click.control.Radio;
import org.apache.click.control.RadioGroup;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;

import edu.indiana.dlib.catalog.config.CollectionConfigurationData;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.impl.DefaultCollectionConfigurationData;
import edu.indiana.dlib.catalog.config.impl.IdGenerator;

/**
 * A collection management page that presents the 
 * options for record creation.  This page requires
 * very implementation-specific details to be entered
 * and will be hard for even an administrative user.
 */
public class RecordCreationSetupPage extends CollectionAdminPage {

    public Form form;
    
    public void onInit() {
        super.onInit();

        // We use some special knowledge about the FileSubmitter here... we 
        // should generalize this at some point.. possibly by having the
        // FileSubmitter report the property names that are expected.
        Properties p = collection.getCollectionMetadata().getRecordCreationProperties();
        if (p == null) {
            p = new Properties();
        }
        
        form = new Form("form");
        form.setActionURL("record-creation-setup.htm");
        
        //FieldSet recFs = new FieldSet("rfs", getMessage("recFs"));
        
        TextField idGenUrlField = new TextField("idGenUrl", getMessage("idGenUrl"));
        idGenUrlField.setSize(32);
        TextField idGenPrefixField = new TextField("idGenPrefix", getMessage("idGenPrefix"));
        idGenPrefixField.setSize(32);
        TextField idPatternField = new TextField("idPattern", getMessage("idPattern"));
        idPatternField.setSize(45);
        TextField idNumberFileField = new TextField("idNumberFile", getMessage("idNumberFile"));
        idNumberFileField.setSize(32);
        TextField idNumberFormatField = new TextField("idNumberFormat", getMessage("idNumberFormat"));
        TextField idNumberPrefixField = new TextField("idNumberPrefix", getMessage("idNumberPrefix"));
        idNumberPrefixField.setSize(32);
        
        RadioGroup group = new RadioGroup("group");
        Radio generatorR = new Radio("generator", getMessage("generator"));
        if (p.containsKey("idGenerationUrl") && p.containsKey("idPrefix")) {
            idGenUrlField.setValue(p.getProperty("idGenerationUrl"));
            idGenPrefixField.setValue(p.getProperty("idPrefix"));
            generatorR.setChecked(true);
        }
        group.add(generatorR);
        Radio specifiedR = new Radio("specified", getMessage("specified"));
        if (p.containsKey("idPattern")) {
            idPatternField.setValue(p.getProperty("idPattern"));
            specifiedR.setChecked(true);
        }
        group.add(specifiedR);
        Radio autoR = new Radio("auto", getMessage("auto"));
        if (p.containsKey("idNumberFile") && p.containsKey("idPrefix") && p.containsKey("idDigits")) {
            idNumberFileField.setValue(p.getProperty("idNumberFile"));
            idNumberPrefixField.setValue(p.getProperty("idPrefix"));
            idNumberFormatField.setValue(p.getProperty("idDigits"));
            autoR.setChecked(true);
        }
        group.add(autoR);
        
        FieldSet idGenFs = new FieldSet("idGenFs", getMessage("idGenFs"));
        //idGenFs.add(generatorR);
        idGenFs.add(idGenUrlField);
        idGenFs.add(idGenPrefixField);
        form.add(idGenFs);
        
        FieldSet specFs = new FieldSet("specFs", getMessage("specFs"));
        //specFs.add(specifiedR);
        specFs.add(idPatternField);
        form.add(specFs);
        
        FieldSet autoFs = new FieldSet("autoFs", getMessage("autoFs"));
        //autoFs.add(autoR);
        autoFs.add(idNumberFileField);
        autoFs.add(idNumberPrefixField);
        autoFs.add(idNumberFormatField);
        form.add(autoFs);
        
        //form.add(recFs);
        
        FieldSet imageFs = new FieldSet("imageFs", getMessage("imageFs"));
        Properties ip = collection.getCollectionMetadata().getImageSubmissionProperties();
        if (ip == null) {
            ip = new Properties();
        }
        TextField dropboxDirField = new TextField("dropboxDir", getMessage("dropboxDir"));
        dropboxDirField.setValue(ip.getProperty("dropboxDir"));
        dropboxDirField.setRequired(true);
        dropboxDirField.setSize(45);
        imageFs.add(dropboxDirField);
        TextField toFedoraDirField = new TextField("toFedoraDir", getMessage("toFedoraDir"));
        toFedoraDirField.setValue(ip.getProperty("ingestDir"));
        toFedoraDirField.setRequired(true);
        toFedoraDirField.setSize(45);
        imageFs.add(toFedoraDirField);
        // TODO: add support for multiple submission (if desirable)
        form.add(imageFs);
        
        form.add(new Submit("submit", getMessage("submit"), this, "onSubmit"));
        addControl(form);
    }
    
    public boolean onSubmit() {
        if (form.isValid()) {
            CollectionConfigurationData cdata = new DefaultCollectionConfigurationData(collection);
            try {
                Properties p = collection.getCollectionMetadata().getRecordCreationProperties() == null ? new Properties() : new Properties(collection.getCollectionMetadata().getRecordCreationProperties());
                addOrRemoveProperty(p, "idGenerationUrl", form.getFieldValue("idGenUrl"));
                addProperty(p, "idPrefix", form.getFieldValue("idGenPrefix"));
                addOrRemoveProperty(p, "idPattern", form.getFieldValue("idPattern"));
                addOrRemoveProperty(p, "idNumberFile", form.getFieldValue("idNumberFile"));
                addProperty(p, "idPrefix", form.getFieldValue("idNumberPrefix"));
                addOrRemoveProperty(p, "idDigits", form.getFieldValue("idNumberFormat"));
                
                if (p.containsKey("idNumberFile") && !(new File(p.getProperty("idNumberFile")).exists())) {
                    form.setError(getMessage("number-file-does-not-exist"));
                    return true;
                }
                
                // instantiate an IdGenerator just to see if there's an exception indicating an
                // invalid set of configuration options
                IdGenerator gen = new IdGenerator(p);
                cdata.getCollectionMetadata().setRecordCreationProperties(p);
            } catch (IllegalStateException ex) {
                form.setError(ex.getMessage() == null ? getMessage("invalid-combination") : ex.getMessage());
                return true;
            } catch (MalformedURLException ex) {
                form.setError(getMessage("malformed-url"));
                return true;
            }
            
            Properties ip = collection.getCollectionMetadata().getImageSubmissionProperties() == null ? new Properties() : new Properties(collection.getCollectionMetadata().getImageSubmissionProperties());
            addOrRemoveProperty(ip, "dropboxDir", form.getFieldValue("dropboxDir"));
            addOrRemoveProperty(ip, "ingestDir", form.getFieldValue("toFedoraDir"));
            File dropboxDir = new File(ip.getProperty("dropboxDir"));
            File ingestDir = new File(ip.getProperty("ingestDir"));
            if (!dropboxDir.exists()) {
                form.setError(getMessage("dropbox-dir-does-not-exist"));
                return true;
            }
            if (!ingestDir.exists()) {
                form.setError(getMessage("to-fedora-dir-does-not-exist"));
                return true;
            }
            cdata.getCollectionMetadata().setImageSubmissionProperties(ip);
            
            try {
                ConfigurationManager cm = getConfigurationManager();
                cm.storeConfiguration(cdata);
                setRedirect("manage-collection.htm");
                return false;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return true;
    }
    
    private void addOrRemoveProperty(Properties p, String name, String value) {
        if (value == null || value.trim().length() == 0) {
            p.remove(name);
        } else {
            p.setProperty(name, value);
        }
    }
    
    private void addProperty(Properties p, String name, String value) {
        if (value == null || value.trim().length() == 0) {
        } else {
            p.setProperty(name, value);
        }
    }
}
