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
                    this.collection = this.getConfigurationManager().getCollectionConfiguration(collectionId, false);
                } catch (ConfigurationManagerException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                throw new IllegalStateException(getMessage("error-no-collection"));
            }
            if (this.collection == null) {
                setForward("unknown-collection.htm");
                return false;
            }
            
            // Determine if the user can access that collection
            try {
                return getAuthorizationManager().canViewCollection(this.collection, this.user);
            } catch (AuthorizationSystemException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    public void onInit() {
        super.onInit();
        addControl(quickSearchPanel);
        
        // add collection link panel if user has access to more than one collection
        AuthorizationManager am = getAuthorizationManager();
        List<CollectionConfiguration> collections = new ArrayList<CollectionConfiguration>();
        try {
            for (CollectionConfiguration collection : this.getConfigurationManager().getCollectionConfigurations(false)) {
                try {
                    if (!collection.getId().equals(this.collection.getId()) && am.canViewCollection(collection, this.user)) {
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
            List<Batch> openBatches = bm.listOpenBatches(user.getUsername(), collection.getId());
            if (openBatches != null && !openBatches.isEmpty()) {
                batchPanel = new Panel("batchPanel", "/collections/batch-panel.htm");
                batchPanel.addModel("openBatches", openBatches);
                addControl(batchPanel);
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
            //batchMenu.add(createMenu(getMessage("menu-batch-import"), collectionPath + "import.htm", rootMenu.getAccessController()));
        }
        
        return rootMenu;
    }
    
    protected String getBreadcrumbs() {
        return getMessage("breadcrumbs", this.collection.getCollectionMetadata().getName());
    }

}
