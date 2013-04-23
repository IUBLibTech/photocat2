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
import java.util.Collections;
import java.util.List;

import org.apache.click.element.CssImport;
import org.apache.click.element.Element;
import org.apache.click.element.JsImport;

import edu.indiana.dlib.catalog.accesscontrol.AuthenticationException;
import edu.indiana.dlib.catalog.accesscontrol.AuthenticationManager;
import edu.indiana.dlib.catalog.accesscontrol.AuthorizationManager;
import edu.indiana.dlib.catalog.accesscontrol.AuthorizationSystemException;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.ConfigurationManagerException;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.publicfacing.config.impl.SearchLinks;
import edu.indiana.dlib.catalog.publicfacing.controls.BrowsePanel;
import edu.indiana.dlib.catalog.search.SearchManager;
import edu.indiana.dlib.catalog.search.SearchResultItemSummary;
import edu.indiana.dlib.catalog.search.structured.DefaultPagingSpecification;
import edu.indiana.dlib.catalog.search.structured.DefaultStructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.SearchConstraint;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchQuery;
import edu.indiana.dlib.catalog.search.structured.StructuredSearchResults;
import edu.indiana.dlib.catalog.search.structured.constraints.CollectionSearchConstraint;

public class SplashPage extends PublicBorderPage {

    /**
     * Determines if the page is public
     */
    public boolean onSecurityCheck() {
        if (!super.onSecurityCheck()) {
            return false;
        }
        try {
            if ((collection != null && !collection.isPublic()) || (unit != null && !unit.isPublic())) {
                AuthenticationManager m = super.getAuthenticationManager();
                if (m == null) {
                    setRedirect("unauthorized.htm");
                } else {
                    user = m.getCurrentUser(getContext().getRequest());
                    if (user == null) {
                       setRedirect("unauthorized.htm");
                       return false;
                    } else {
                        ConfigurationManager cm = getConfigurationManager();
                        AuthorizationManager authz = getAuthorizationManager();
                        if (collection != null) {
                            if (!authz.canManageCollection(cm.getCollectionConfiguration(collection.getId()), cm.getParent(collection.getId(), false), user)) {
                                setRedirect("unauthorized.htm");
                                return false;
                            }
                        }
                        if (unit != null) {
                            if (!authz.canManageUnit(unit, user)) {
                                setRedirect("unauthorized.htm");
                                return false;
                            }
                        }
                    }
                }
            }
        } catch (AuthenticationException ex) {
            throw new RuntimeException(ex);
        } catch (AuthorizationSystemException ex) {
            throw new RuntimeException(ex);
        } catch (ConfigurationManagerException ex) {
            throw new RuntimeException(ex);
        }
        return true;
    }
    
    public void onRender() {
        super.onRender();
        ConfigurationManager cm = getConfigurationManager();
        
        if (collection != null) {
            
        } else if (unit != null) {
            try {
                addModel("collections", cm.getChildren(unit.getId(), ConfigurationManager.COLLECTION, true));
            } catch (Throwable t) {
                LOGGER.error("Error getting collections within unit " + unit.getId() + "!", t);
            }
        }
        
        SearchManager sm = getSearchManager();
        if (cm != null && sm != null) {
            addControl(new BrowsePanel(cm, sm, null, (collection != null ? collection : unit)));
            addModel("links", new SearchLinks("search.htm", null, getScopeId(), sm));
        }
        
        // set up the featured images
        ItemManager im = getItemManager();
        if (sm != null && collection != null && cm != null && cm != null && im != null) {
            try {
                StructuredSearchResults sresults = sm.search(getFeaturedItemSafeQuery(cm, collection));
                if (sresults.getTotalResultsCount() >= 5) {
                    try {
                        List<SearchResultItemSummary> results = new ArrayList<SearchResultItemSummary>();
                        for (Item item : sresults.getResults()) {
                            SearchResultItemSummary summary = new SearchResultItemSummary(item, cm.getCollectionConfiguration(item.getCollectionId())); 
                            results.add(summary);
                        }
                        Collections.shuffle(results);
                        addModel("featuredItems", results);
                    } catch (ConfigurationManagerException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    List<SearchConstraint> constraints = new ArrayList<SearchConstraint>();
                    constraints.add(new CollectionSearchConstraint(collection, true));
                    constraints.addAll(cm.getPublicRecordsSearchConstraints());
                    DefaultStructuredSearchQuery query = new DefaultStructuredSearchQuery(new DefaultPagingSpecification().pageSize(5), constraints);
                    StructuredSearchResults searchResults = sm.search(query);
                    List<SearchResultItemSummary> results = new ArrayList<SearchResultItemSummary>();
                    if (searchResults.getResults() != null) {
                        for (Item item : searchResults.getResults()) {
                            try {
                                SearchResultItemSummary summary = new SearchResultItemSummary(item, cm.getCollectionConfiguration(item.getCollectionId())); 
                                results.add(summary);
                            } catch (ConfigurationManagerException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                    Collections.shuffle(results);
                    addModel("featuredItems", results);
                }
            } catch (Throwable t) {
                // unable to generate featured items
                LOGGER.error("Unable to generate featured items!", t);
            }
        }
    }
    
    private StructuredSearchQuery getFeaturedItemSafeQuery(ConfigurationManager cm, CollectionConfiguration c) throws ConfigurationManagerException {
        // create a query that incorporates the scope as well 
        // as any required constraints
        List<SearchConstraint> constraints = new ArrayList<SearchConstraint>();
        constraints.add(c.getCollectionMetadata().getConditionForFeaturedItems());
        constraints.add(new CollectionSearchConstraint(c.getId()));
        constraints.addAll(cm.getPublicRecordsSearchConstraints());
        return new DefaultStructuredSearchQuery(constraints);

    }
    
    public List<Element> getHeadElements() { 
        // Use lazy loading to ensure the JS is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 

            headElements.add(new CssImport("/css/splash.css"));
            headElements.add(new CssImport("/css/slideshow.css"));
            headElements.add(new JsImport("/js/jquery/jquery-1.7.1.min.js"));
            headElements.add(new JsImport("/js/jquery/jquery-jcarousel.min.js"));
        } 
        return headElements; 
    } 

    public String getTitle() {
        if (collection != null) {
            return getMessage("title", collection.getCollectionMetadata().getFullName());
        } else if (unit != null) {
            return getMessage("title", unit.getCollectionMetadata().getFullName());
        } else {
            return getMessage("default-title");
        }
    }
    
}
