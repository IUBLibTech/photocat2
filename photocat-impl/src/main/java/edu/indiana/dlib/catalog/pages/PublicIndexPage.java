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

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.ConfigurationManagerException;
import edu.indiana.dlib.catalog.publicfacing.controls.BrowsePanel;
import edu.indiana.dlib.catalog.search.SearchManager;

/**
 * Ultimately merge this with the main IndexPage based on the presence
 * of a public interface vs a cataloging interface.
 */
public class PublicIndexPage extends PublicBorderPage {

    public List<CollectionConfiguration> featuredCollections;

    public void onInit() {
        super.onInit();
        ConfigurationManager cm = getConfigurationManager();
        SearchManager sm = getSearchManager();
        if (cm != null && sm != null) {
            addControl(new BrowsePanel(cm, sm));
            featuredCollections = new ArrayList<CollectionConfiguration>();
            try {
                for (CollectionConfiguration c : cm.getCollectionConfigurations(ConfigurationManager.COLLECTION, true)) {
                    featuredCollections.add(c);
                }
            } catch (ConfigurationManagerException ex) {
                throw new RuntimeException(ex);
            }
            Collections.shuffle(featuredCollections);
        }
    }
    
    public List<Element> getHeadElements() { 
        // Use lazy loading to ensure the JS is only added the 
        // first time this method is called. 
        if (headElements == null) { 
            // Get the head elements from the super implementation 
            headElements = super.getHeadElements(); 

            headElements.add(new CssImport("/css/index.css"));
            headElements.add(new CssImport("/css/slideshow.css"));
            headElements.add(new JsImport("/js/jquery/jquery-1.7.1.min.js"));
        } 
        return headElements; 
    } 
    
}
