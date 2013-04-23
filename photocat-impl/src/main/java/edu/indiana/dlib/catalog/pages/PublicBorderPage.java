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

import java.util.List;

import org.apache.click.control.Form;
import org.apache.click.element.CssImport;
import org.apache.click.element.Element;
import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationException;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.ConfigurationManagerException;
import edu.indiana.dlib.catalog.publicfacing.controls.QuickSearchForm;
import edu.indiana.dlib.catalog.search.SearchManager;

public abstract class PublicBorderPage extends ApplicationPage {

    protected Logger LOGGER = Logger.getLogger(PublicBorderPage.class);
    
    public static final String SCOPE_PARAM_NAME = "scope";
    
    /**
     * The title displayed in the header.  This is set during
     * onRender() by this class by invoking 'getTitle(). Subclasses
     * should override that method to replace the title.
     */
    public String title;
    
    /**
     * Will be set during the onSecurityCheck() method with
     * the current user if the use can be authenticated.
     */
    public UserInfo user;

    /**
     * The current collection (for a collection-scoped page)
     * or the collection to which the current item belongs for
     * an item-scoped page.
     */
    public CollectionConfiguration collection;
    
    /**
     * The current unit for a unit-scoped page.
     */
    public CollectionConfiguration unit;
    
    public String getTemplate() {
        return "publicfacing-border-template.htm";
    }
    
    /**
     * Sets the current user if possible and returns true.
     */
    public boolean onSecurityCheck() {
        if (!super.onSecurityCheck()) {
            return false;
        }
        
        if (!isPublicBrowsingEnabled()) {
            if (isCatalogingEnabled()) {
                setRedirect("/no-public.htm");
            } else {
                setRedirect("/error.htm");
            }
            return false;
        }
        
        // Determine the scope of this page
        try {
            ConfigurationManager cm = getConfigurationManager();
            String scopeId = getContext().getRequest().getParameter(SCOPE_PARAM_NAME);
            if (scopeId != null) {
                CollectionConfiguration c = cm.getCollectionConfiguration(scopeId);
                if (c== null) {
                    // do nothing
                } else if (c.getCollectionMetadata().isCollection()) {
                    collection = c;
                    unit = cm.getParent(c.getId(), true);
                } else if (c.getCollectionMetadata().isUnit()) {
                    unit = c;
                    addModel("unit", unit);
                }
            }
        } catch (ConfigurationManagerException ex) {
            LOGGER.error("Error accessing collection information!", ex);
            throw new RuntimeException();
        }
        return true;
    }
    
    public void onInit() {
        super.onInit();
        ConfigurationManager cm = getConfigurationManager();
        SearchManager sm = getSearchManager();
        if (cm != null && sm != null) {
            try {
                Form form = new QuickSearchForm("quickSearchForm", cm, sm, getScopeId());
                form.setActionURL("public-index.htm");
                addControl(form);
            } catch (Throwable t) {
                LOGGER.error("Error accessing collection configuration!", t);
            }
        }
    }
    
    public List<Element> getHeadElements() { 
        // Use lazy loading to ensure the JS is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 

            // Include the control's external Css resource 
            headElements.add(new CssImport("/css/shared.css"));
            headElements.add(new CssImport("/css/border.css"));
        } 
        return headElements; 
    } 
    
    /**
     * Gets the explicit scope of the page, or null if no explicit
     * scope has been specified.  The scope is a sort of sticky 
     * selection of a unit or collection and is passed around through
     * the scopeId parameter.
     * 
     * For pages that have no natural scope (like a search results 
     * page) this explicit scope may 
     */
    public String getScopeId() {
        return getContext().getRequestParameter(SCOPE_PARAM_NAME);
    }
    
    public void onRender() {
        super.onRender();
        title = getTitle();
        if (getScopeId() != null) {
            addModel("scopeId", getScopeId());
        }
    }
    
    protected String getTitle() {
        return getMessage("title");
    }
    
}
