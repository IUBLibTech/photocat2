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
package edu.indiana.dlib.catalog.pages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.click.control.ActionLink;
import org.apache.click.control.Column;
import org.apache.click.control.Form;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;

import edu.indiana.dlib.catalog.accesscontrol.AuthorizationManager;
import edu.indiana.dlib.catalog.accesscontrol.AuthorizationSystemException;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.CollectionMetadata;
import edu.indiana.dlib.catalog.config.ConfigurationManagerException;

/**
 * A page only available to site administrators that exposes 
 * several admin-only functions.
 */
public class AdminPage extends AuthenticatedBorderPage {
    
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
        moduleTable.setDataProvider(new DataProvider() {
            public Iterable getData() {
                return modules;
            }});
        addControl(moduleTable);
        
        // Set up a table of the loaded collections (informative)
        try {
            final Collection<CollectionMetadata> collections = new ArrayList<CollectionMetadata>();
            for (CollectionConfiguration config : getConfigurationManager().getCollectionConfigurations(false)) {
                collections.add(config.getCollectionMetadata());
            }
            Table collectionTable = new Table("collectionTable");
            collectionTable.addColumn(new Column("id", getMessage("column-label-collection-id")));
            collectionTable.addColumn(new Column("name", getMessage("column-label-collection-name")));
            collectionTable.setDataProvider(new DataProvider() {
                public Iterable getData() {
                    return collections;
                }});
            addControl(collectionTable);
        } catch (ConfigurationManagerException ex) {
            throw new RuntimeException(ex);
        }

        ActionLink reloadLink = new ActionLink("reloadLink", getMessage("reload-link"), this, "onReloadCollections");
        addControl(reloadLink);
    }
    
    public boolean onReloadCollections() {
        try {
            getConfigurationManager().getCollectionConfigurations(true);
        } catch (ConfigurationManagerException ex) {
            throw new RuntimeException(ex);
        }
        setRedirect("admin.htm");
        return false;
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
    
    public static class ModuleSummary {
        
        private String moduleType;
        
        private String moduleClassName;
        
        public ModuleSummary(String type, Object module) {
            moduleType = type;
            moduleClassName = (module != null ? module.getClass().getName() : null);
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
