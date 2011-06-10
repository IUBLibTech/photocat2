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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.click.element.CssImport;
import org.apache.click.element.Element;

import edu.indiana.dlib.catalog.accesscontrol.AuthorizationManager;
import edu.indiana.dlib.catalog.accesscontrol.AuthorizationSystemException;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationManagerException;

/**
 * A Page that displays a list of all collections that
 * the currently logged-in user can view.  
 * 
 * This page is the only one that forces a refresh of data
 * from the source for the collections list rather than
 * returning cached values.  This is in part because this 
 * page is expected to be accessed infrequently by production
 * users.  Note that individual collections will have their
 * configurations refreshed when accessing the collection
 * home page.
 */
public class SelectCollectionPage extends AuthenticatedBorderPage {

    public List<CollectionConfiguration> configurations;

    public void onInit() {
        super.onInit();
        AuthorizationManager am = this.getAuthorizationManager();
        this.configurations = new ArrayList<CollectionConfiguration>();
        try {
            for (CollectionConfiguration config : this.getConfigurationManager().getCollectionConfigurations(false)) {
                try {
                    if (am.canViewCollection(config, this.user)) {
                        this.configurations.add(config);
                    }
                } catch (AuthorizationSystemException ex) {
                    throw new RuntimeException(ex);
                }
            }
            
            // If only one collection is available, redirect to it automatically
            if (this.configurations.size() == 1) {
                CollectionConfiguration singleCollection = this.configurations.iterator().next();
                this.setRedirect("collection/" + singleCollection.getCollectionMetadata().getId() + "/");
            } else {
                Collections.sort(this.configurations, new Comparator<CollectionConfiguration>() {

                    public int compare(CollectionConfiguration o1, CollectionConfiguration o2) {
                        return o1.getId().compareTo(o2.getId());
                    }});
            }
        } catch (ConfigurationManagerException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public List<Element> getHeadElements() { 
        // We use lazy loading to ensure the CSS import is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 

            CssImport cssImport = new CssImport("/css/select-collection.css"); 
            headElements.add(cssImport); 
        } 
        return headElements; 
    }

}
