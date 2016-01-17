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

import org.apache.click.control.FieldSet;
import org.apache.click.control.FileField;
import org.apache.click.control.Form;
import org.apache.click.control.Submit;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.TransformationConfiguration;
import edu.indiana.dlib.catalog.config.impl.DefaultCollectionConfigurationData;

/**
 * A portal for collection management.  Several aspects of the
 * collection can be managed from different pages.  This page
 * exposes a summary of the collection configuration with links
 * to manage the various aspects.
 */
public class ManageCollectionPage extends CollectionAdminPage {

    public Form importConfigurationForm;
    
    public Form importModsTransformationForm;
    
    public void onInit() {
        super.onInit();
        importConfigurationForm = new Form("importConfigurationForm");
        FieldSet fs = new FieldSet("importConfiguration");
        FileField configurationFileField = new FileField("file", getMessage("configuration"));
        fs.add(configurationFileField);
        fs.add(new Submit("submit", this, "onSubmit"));
        importConfigurationForm.add(fs);
        addControl(importConfigurationForm);
        
        importModsTransformationForm = new Form("importModsTransformationForm");
        fs = new FieldSet("importMODSTransformation");
        FileField modsFileField = new FileField("file", getMessage("xslt"));
        fs.add(modsFileField);
        fs.add(new Submit("submit", this, "onSubmitMods"));
        importModsTransformationForm.add(fs);
        addControl(importModsTransformationForm);
        
        if (collection.getTransformationConfigurations() != null) {
            for (TransformationConfiguration t : collection.getTransformationConfigurations()) {
                if (t.getId().equals("mods")) {
                    addModel("existingModsTransform", t.getXsltUrl());
                }
            }
        }
    }
    
    public boolean onSubmit() {
        if (importConfigurationForm.isValid()) {
            ConfigurationManager cm = getConfigurationManager();
            FileField fileField = (FileField) importConfigurationForm.getField("file");
            CollectionConfiguration orig = collection;
            try {
                DefaultCollectionConfigurationData c = new DefaultCollectionConfigurationData(fileField.getFileItem().getInputStream());
                if (!c.getId().equals(orig.getId())) {
                    importConfigurationForm.setError(getMessage("id-mismatch-error"));
                    return true;
                }
                cm.storeConfiguration(c);
                cm.clearCache();
                setRedirect("manage-collection.htm");
                return false;
            } catch (Throwable t) {
                //try to roll back the change (if one was made)
                try {
                    cm.storeConfiguration(orig);
                } catch (Throwable t2) {
                    LOGGER.error("Error rolling back collection configuration change!");
                }
                LOGGER.error("Error with submitted collection configuration!", t);
                if (t.getLocalizedMessage() != null) {
                    importConfigurationForm.setError(t.getLocalizedMessage());
                } else {
                    importConfigurationForm.setError(t.getClass().getSimpleName());
                }
                return true;
                
            }
        }
        return true;
    }
    
    
    public boolean onSubmitMods() {
        if (importModsTransformationForm.isValid()) {
            ConfigurationManager cm = getConfigurationManager();
            FileField fileField = (FileField) importModsTransformationForm.getField("file");
            CollectionConfiguration orig = collection;
            try {
                String url = cm.storeCollectionFile(orig, fileField.getFileItem().getInputStream(), "text/xml", "to-mods.xsl", true);
                List<TransformationConfiguration> t = new ArrayList<TransformationConfiguration>(orig.getTransformationConfigurations());
                boolean hasMods = false;
                boolean changedConfig = false;
                for (int i = 0; i < t.size(); i ++) {
                    TransformationConfiguration tc = t.get(i);
                    if (tc.getId().equals("mods")) {
                        hasMods = true;
                        if (tc.getXsltUrl().equals(url)) {
                            break;
                        } else {
                            t.set(i, new TransformationConfiguration("mods", "MODS", url, true));
                            changedConfig = true;
                        }
                    }
                }
                if (!hasMods) {
                    t.add(new TransformationConfiguration("mods", "MODS", url, true));
                    changedConfig = true;
                }
                if (changedConfig) {
                    // only store and refresh the cache if there's an actual change 
                    // to the configuration.
                    orig.setTransformationConfigurations(t);
                    cm.storeConfiguration(orig);
                    cm.clearCache();
                }
                setRedirect("manage-collection.htm");
                return false;
            } catch (Throwable t) {
                LOGGER.error("Error submitting new stylesheet!", t);
                if (t.getLocalizedMessage() != null) {
                    importModsTransformationForm.setError(t.getLocalizedMessage());
                } else {
                    importModsTransformationForm.setError(t.getClass().getSimpleName());
                }
                return true;
                
            }
        }
        return true;
    }
    
}
