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
package edu.indiana.dlib.catalog.pages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.click.control.ActionLink;
import org.apache.click.control.Column;
import org.apache.click.control.FieldSet;
import org.apache.click.control.Form;
import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.Table;
import org.apache.click.control.TextField;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;
import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.accesscontrol.AuthorizationManager;
import edu.indiana.dlib.catalog.accesscontrol.AuthorizationSystemException;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.ConfigurationManagerException;
import edu.indiana.dlib.catalog.config.impl.FedoraConfigurationManager;

/**
 * A page only available to site administrators that exposes 
 * several admin-only functions.
 * 
 * This class is final because it overrides some key functionality
 * to make it accessible whether cataloging or public viewing are
 * enabled or not. 
 */
public final class AdminPage extends AuthenticatedBorderPage {
    
    private static final String NONE = "NONE";
    
    private Form createCollectionForm;
    
    private Form createUnitForm;
    
    /**
     * Attempts to determine the current user, and if it fails, 
     * redirects the AuthorizationRequredPage.
     */
    public boolean onSecurityCheck() {
        boolean continueProcessing = super.onSecurityCheck();
        if (!continueProcessing) {
            return false;
        }
        AuthorizationManager authzMan = getAuthorizationManager();
        try {
            if (!authzMan.canManageApplication(user)) {
                setRedirect("authorization-denied.htm");
                return false;
            }
            return true;
        } catch (AuthorizationSystemException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Overrides the super class implementation to always return 
     * true.  This ensures that this page is accessible on collections
     * deployed without cataloging capabilities (when he superclass
     * would return false).
     */
    public boolean isCatalogingEnabled() {
        return true;
    }
    
    /**
     * Overrides the super class implementation to always return 
     * true.  This ensures that this page is accessible on collections
     * deployed without public access capabilities (when he superclass
     * would return false).
     */
    public boolean isPublicBrowsingEnabled() {
        return true;
    }
    
    public void onInit() {
        super.onInit();
        
        // Set up a table of the loaded modules (informative)
        final List<ModuleSummary> modules = new ArrayList<ModuleSummary>();
        modules.add(new ModuleSummary(getMessage("authentication"), getAuthenticationManager()));
        modules.add(new ModuleSummary(getMessage("authorization"), getAuthorizationManager()));
        modules.add(new ModuleSummary(getMessage("configuration"), getConfigurationManager()));
        modules.add(new ModuleSummary(getMessage("item"), getItemManager()));
        modules.add(new ModuleSummary(getMessage("search"), getSearchManager()));
        modules.add(new ModuleSummary(getMessage("vocabulary"), getConfigurationManager()));
        modules.add(new ModuleSummary(getMessage("batch"), getBatchManager()));
        Table moduleTable = new Table("moduleTable");
        moduleTable.addColumn(new Column("moduleType", getMessage("column-label-type")));
        moduleTable.addColumn(new Column("moduleClassName", getMessage("column-label-class")));
        moduleTable.setClass(Table.CLASS_BLUE1);
        moduleTable.setDataProvider(new DataProvider() {
            public Iterable getData() {
                return modules;
            }});
        addControl(moduleTable);
        
        // Set up a table of the loaded collections (informative)
        try {
            final Collection<CollectionConfiguration> collections = new ArrayList<CollectionConfiguration>();
            for (CollectionConfiguration config : getConfigurationManager().getCollectionConfigurations(ConfigurationManager.COLLECTION, false)) {
                collections.add(config);
            }
            Table collectionTable = new Table("collectionTable");
            collectionTable.setClass(Table.CLASS_BLUE1);
            collectionTable.addColumn(new Column("isPublic", getMessage("column-label-is-public")));
            collectionTable.addColumn(new Column("id", getMessage("column-label-collection-id")));
            collectionTable.addColumn(new Column("fullName", getMessage("column-label-collection-name")));
            collectionTable.setDataProvider(new DataProvider() {
                public Iterable getData() {
                    return collections;
                }});
            addControl(collectionTable);
        } catch (ConfigurationManagerException ex) {
            throw new RuntimeException(ex);
        }

        addModel("invalidIds", getConfigurationManager().getInvalidCollectionIds());
        
        ActionLink reloadLink = new ActionLink("reloadLink", getMessage("reload-link"), this, "onReloadCollections");
        addControl(reloadLink);
        
        if (super.isCatalogingEnabled()) { // we have to use the superclass method here because we overrode it to allow this page to be available in both applications.
            try {
                createUnitForm = new Form("createUnitForm");
                FieldSet fs = new FieldSet("createNewUnit");
                TextField idField = new TextField("id");
                idField.setRequired(true);
                fs.add(idField);
                fs.add(new Submit("create", this, "onCreateUnit"));
                createUnitForm.add(fs);
                addControl(createUnitForm);
                
                
                createCollectionForm = new Form("createCollectionForm");
                fs = new FieldSet("createNewCollection");
                idField = new TextField("id");
                idField.setRequired(true);
                fs.add(idField);
                Select unitSelect = new Select("unit");
                unitSelect.add(new Option(NONE, ""));
                for (CollectionConfiguration u : getConfigurationManager().getCollectionConfigurations(ConfigurationManager.UNIT, false)) {
                    unitSelect.add(new Option(u.getId(), u.getFullName()));
                }
                fs.add(unitSelect);
                fs.add(new Submit("create", this, "onCreateCollection"));
                createCollectionForm.add(fs);
                addControl(createCollectionForm);
            } catch (ConfigurationManagerException ex) {
                throw new RuntimeException(ex);
            }
            
        }
    }
    
    public boolean onReloadCollections() {
        getConfigurationManager().clearCache();
        setRedirect("admin.htm");
        return false;
    }
    
    public boolean onCreateUnit() {
        if (!createUnitForm.isValid()) {
            // fall through and present the validation error(s)
            return true;
        } else {
            // create the new collection and redirect to that page
            ConfigurationManager cm = getConfigurationManager();
            try {
                CollectionConfiguration c = cm.createNewCollection(createUnitForm.getFieldValue("id"), FedoraConfigurationManager.UNIT, null);
                cm.clearCache();
                setRedirect("collection/" + c.getId() + "/basic-collection-setup.htm");
                return false;
            } catch (ConfigurationManagerException ex) {
                Logger.getLogger(this.getClass()).error(ex);
                if (ex.getLocalizedMessage() != null) {
                    createUnitForm.setError(ex.getLocalizedMessage());
                } else {
                    createUnitForm.setError(ex.getClass().getSimpleName());
                }
                return true;
            }
        }
    }
    
    public boolean onCreateCollection() {
        if (!createCollectionForm.isValid()) {
            // fall through and present the validation error(s)
            return true;
        } else {
            // create the new collection and redirect to that page
            ConfigurationManager cm = getConfigurationManager();
            String parent = createCollectionForm.getFieldValue("unit");
            try {
                CollectionConfiguration c = cm.createNewCollection(createCollectionForm.getFieldValue("id"), FedoraConfigurationManager.COLLECTION, (parent.equals(NONE) ? null : cm.getCollectionConfiguration(parent)));
                cm.clearCache();
                setRedirect("collection/" + c.getId() + "/basic-collection-setup.htm");
                return false;
            } catch (ConfigurationManagerException ex) {
                Logger.getLogger(this.getClass()).error(ex);
                if (ex.getLocalizedMessage() != null) {
                    createCollectionForm.setError(ex.getLocalizedMessage());
                } else {
                    createCollectionForm.setError(ex.getClass().getSimpleName());
                }
                return true;
            }
        }
    }
    
    public List<Element> getHeadElements() { 
        // We use lazy loading to ensure the CSS import is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 

            CssImport cssImport = new CssImport("/css/admin.css"); 
            headElements.add(cssImport); 
        } 
        return headElements; 
    }
    
    public class ModuleSummary {
        
        private String moduleType;
        
        private String moduleClassName;
        
        public ModuleSummary(String type, Object module) {
            moduleType = type;
            moduleClassName = (module != null ? module.getClass().getName() : AdminPage.this.getMessage("missing"));
        }
        
        public void setModuleType(String moduleType) {
            this.moduleType = moduleType;
        }
        
        public String getModuleType() {
            return moduleType;
        }
        
        public void setModuleClassName(String moduleClassName) {
            this.moduleClassName = moduleClassName;
        }
        
        public String getModuleClassName() {
            return moduleClassName;
        }
    }
    
}
