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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.click.Control;
import org.apache.click.control.Container;
import org.apache.click.control.Form;
import org.apache.click.control.Panel;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;
import org.apache.click.extras.control.Menu;
import org.apache.click.extras.security.AccessController;
import org.apache.click.util.ClickUtils;
import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.accesscontrol.AuthorizationManager;
import edu.indiana.dlib.catalog.accesscontrol.AuthorizationSystemException;
import edu.indiana.dlib.catalog.asynchronous.Operation;
import edu.indiana.dlib.catalog.asynchronous.UserOperationManager;
import edu.indiana.dlib.catalog.batch.Batch;
import edu.indiana.dlib.catalog.batch.BatchManager;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.ConfigurationManagerException;
import edu.indiana.dlib.catalog.pages.AuthenticatedBorderPage;
import edu.indiana.dlib.catalog.search.impl.click.control.QuickSearchPanel;

public abstract class CollectionPage extends AuthenticatedBorderPage {
    
    private static final long serialVersionUID = 1L;
    
    protected Logger LOGGER = Logger.getLogger(CollectionPage.class);
    
    /**
     * The collection to which this page belongs.  This is set
     * during the onSecurityCheck method.
     */
    public CollectionConfiguration collection;
    
    /**
     * The unit to which the collection belongs or null if none.
     */
    public CollectionConfiguration unit;

    /**
     * A Panel representing a quick search of the web site.
     * The rendering of this panel is meant to be no larger 
     * than a single input box that is suitable for insertion
     * anywhere in the interface.
     */
    public QuickSearchPanel quickSearchPanel;
    
    public Panel collectionLinkPanel;
    
    private Panel batchPanel;
    
    private Panel operationPanel;
    
    public CollectionPage() {
        super();
        quickSearchPanel = new QuickSearchPanel();
    }
    
    /**
     * Determines the collection from the Request Attribute, then
     * determines whether the current user can access that collection.
     */
    public boolean onSecurityCheck() {
        if (!super.onSecurityCheck()) {
            return false;
        } else {
            // determine the collection
            String collectionId = (String) getContext().getRequestAttribute("collectionId");
            
            if (collectionId != null) {
                try {
                    collection = getConfigurationManager().getCollectionConfiguration(collectionId);
                    unit = getConfigurationManager().getParent(collectionId, false);
                } catch (ConfigurationManagerException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                throw new IllegalStateException(getMessage("error-no-collection"));
            }
            if (collection == null || (collection.getCollectionMetadata().isUnit() && !supportsUnitAccess())) {
                setForward("unknown-collection.htm");
                return false;
            }
            
            // Determine if the user can access that collection
            try {
                boolean allow = getAuthorizationManager().canViewCollection(collection, unit, user);
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
     * A method that can be overriden by subclasses for pages that
     * may be accessed for a unit (as opposed to a collection).
     * The default implementation returns false.
     */
    protected boolean supportsUnitAccess() {
        return false;
    }
    
    public void onInit() {
        super.onInit();
        addControl(quickSearchPanel);
        
        // add collection link panel if user has access to more than one collection
        AuthorizationManager am = getAuthorizationManager();
        List<CollectionConfiguration> collections = new ArrayList<CollectionConfiguration>();
        try {
            for (CollectionConfiguration collection : this.getConfigurationManager().getCollectionConfigurations(ConfigurationManager.COLLECTION, false)) {
                try {
                    if (!collection.getId().equals(this.collection.getId()) && am.canViewCollection(collection, unit, user)) {
                        collections.add(collection);
                    }
                } catch (AuthorizationSystemException ex) {
                    // eat it, and have the menu options be suppressed...
                }
            }
        } catch (ConfigurationManagerException ex) {
            // eat it, and have no colletions list
        }
        if (!collections.isEmpty()) {
            Collections.sort(collections, new Comparator<CollectionConfiguration>() {

                public int compare(CollectionConfiguration o1, CollectionConfiguration o2) {
                    return o1.getId().compareTo(o2.getId());
                }});
            this.collectionLinkPanel = new Panel("collectionLinkPanel", "collection-link-panel.htm");
            this.collectionLinkPanel.addModel("collections", collections);
            this.addControl(collectionLinkPanel);
        }
    }
    
    public List<Element> getHeadElements() {
        if (collectionLinkPanel != null || batchPanel != null || operationPanel != null) {
            // We use lazy loading to ensure the CSS import is only added the 
            // first time this method is called. 
            if (headElements == null) { 
                // Get the head elements from the super implementation 
                headElements = super.getHeadElements(); 
                if (collectionLinkPanel != null) {
                    headElements.add(new CssImport("/css/collection-link-panel.css"));
                }
                if (batchPanel != null) {
                    headElements.add(new CssImport("/css/batch-panel.css"));
                }
                if (operationPanel != null) {
                    headElements.add(new CssImport("/css/operation-panel.css"));
                }
            } 
            return headElements;
        } else {
            return super.getHeadElements();
        }
        
    }
    
    public void onRender() {
        super.onRender();
        // because of the fancy redirection, we must explicitly set
        // the action URL for any form to a relative path of this page, 
        // because the path it determines doesn't include the fancy 
        // collection redirect.
        String pagePath = ClickUtils.getRequestURI(getContext().getRequest());
        if (pagePath.contains("/")) {
            pagePath = pagePath.substring(0, pagePath.lastIndexOf('/') + 1);
        }
        for (Control control : this.getControls()) {
            fixFormActionUrls(control, pagePath);
        }
        
        BatchManager bm = getBatchManager();
        if (bm != null) {
            try {
                List<Batch> openBatches = bm.listOpenBatches(user.getUsername(), collection.getId());
                if (openBatches != null && !openBatches.isEmpty()) {
                    batchPanel = new Panel("batchPanel", "/collections/batch-panel.htm");
                    batchPanel.addModel("openBatches", openBatches);
                    addControl(batchPanel);
                }
            } catch (Throwable t) {
                Logger.getLogger(getClass()).error("Error setting up current batches!", t);
            }
        }
        
        List<Operation> pendingOperations = UserOperationManager.getOperationManager(getContext().getSession(), user.getUsername()).listIncompleteOperations();
        if (pendingOperations != null && !pendingOperations.isEmpty()) {
            operationPanel = new Panel("operationPanel", "/collections/operation-panel.htm");
            operationPanel.addModel("pendingOperations", pendingOperations);
            addControl(operationPanel);
        }
        
    }
    
    /**
     * Because of the fancy URL-based forwarding (collection ids are specified
     * in the URL, not a parameter) any controls with action urls are going to
     * be incorrect.  This method corrects them.
     */
    private void fixFormActionUrls(Control control, String pagePath) {
        if (control instanceof Form) {
            Form form = (Form) control;
            String actionUrl = form.getActionURL();
            if (actionUrl.startsWith(pagePath)) {
                form.setActionURL(actionUrl.substring(pagePath.length()));
            }
        } else if (control instanceof Container) {
            for (Control childControl : ((Container) control).getControls()) {
                fixFormActionUrls(childControl, pagePath);
            }
        }
    }
    
    protected Menu getMenu() {
        String collectionPath = "collection/" + this.collection.getCollectionMetadata().getId() + "/";

        Menu rootMenu = new Menu("rootMenu");
        rootMenu.setAccessController(new AccessController() {
            public boolean hasAccess(HttpServletRequest request, String resource) {
                return true;
            }});
        
        Menu searchMenu = createMenu(getMessage("menu-search"), collectionPath + "search.htm?form_name=searchForm&query=&pageSize=20&action=Search", rootMenu.getAccessController());
        rootMenu.add(searchMenu);
        
        Menu browseMenu = createMenu(getMessage("menu-browse"), collectionPath + "browse.htm", rootMenu.getAccessController());
        rootMenu.add(browseMenu);

        Menu sourcesMenu = createMenu(getMessage("menu-sources"), collectionPath + "sources.htm", rootMenu.getAccessController());
        rootMenu.add(sourcesMenu);
        
        if (getBatchManager() != null) {
            Menu batchMenu = createMenu(getMessage("menu-batch"), collectionPath + "batches.htm", rootMenu.getAccessController());
            rootMenu.add(batchMenu);
        }
        
        try {
            if (getAuthorizationManager().canManageCollection(collection, unit, user)) {
                Menu importExportMenu = createMenu(getMessage("menu-batch-import-export"), collectionPath + "export.htm", rootMenu.getAccessController());
                importExportMenu.add(createMenu(getMessage("menu-batch-import"), collectionPath + "import.htm", rootMenu.getAccessController()));
                importExportMenu.add(createMenu(getMessage("menu-batch-export"), collectionPath + "export.htm", rootMenu.getAccessController()));
                rootMenu.add(importExportMenu);
                
                if (getAuthorizationManager().canManageApplication(user)) {
                    Menu manageCollectionMenu = createMenu(getMessage("menu-manage"), collectionPath + "manage-collection.htm", rootMenu.getAccessController());
                    rootMenu.add(manageCollectionMenu);
                }
                
            }
        } catch (AuthorizationSystemException ex) {
            LOGGER.warn("Error building menu.", ex);
        }
        
        return rootMenu;
    }
    
    protected String getBreadcrumbs() {
        return getMessage("breadcrumbs", this.collection.getCollectionMetadata().getFullName());
    }

}
