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

import javax.servlet.ServletContext;

import org.apache.click.Page;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import edu.indiana.dlib.catalog.accesscontrol.AuthenticationManager;
import edu.indiana.dlib.catalog.accesscontrol.AuthorizationManager;
import edu.indiana.dlib.catalog.batch.BatchManager;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.HistoryEnabledItemManager;
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.search.SearchManager;

/**
 * A root for all pages in this application.  It has helper methods to
 * get the various beans from the ApplicationContext configured using
 * the Spring applicationContext.xml file.
 *
 */
public abstract class ApplicationPage extends Page implements ApplicationContextAware {

    private static final String APPLICATION_CONTEXT = ApplicationPage.class.getName();
    
    private ApplicationContext applicationContext;
    
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.applicationContext = context;
        getContext().getServletContext().setAttribute(APPLICATION_CONTEXT, context);
    }
    
    public static final ApplicationContext getApplicationContext(ServletContext sc) {
        return (ApplicationContext) sc.getAttribute(APPLICATION_CONTEXT);
    }
    
    /**
     * Gets the AuthenticationManager bean stored as the 'authenticationManager' 
     * bean in the ApplicationContext.
     */
    public AuthenticationManager getAuthenticationManager() {
        return (AuthenticationManager) this.applicationContext.getBean("authenticationManager");
    }
    
    /**
     * Gets the AuthorizationManager bean stored as the 'authorizationManager' 
     * bean in the ApplicationContext.
     */
    public AuthorizationManager getAuthorizationManager() {
        return (AuthorizationManager) this.applicationContext.getBean("authorizationManager");
    }
    
    /**
     * Gets the ConfigurationManager bean stored as the 'configurationManager' bean
     * stored in the ApplicationContext.
     */
    public ConfigurationManager getConfigurationManager() {
        return (ConfigurationManager) this.applicationContext.getBean("configurationManager");
    }
    
    /**
     * Gets the ItemManager bean stored as the 'itemManager' bean in the
     * ApplicationContext.
     */
    public ItemManager getItemManager() {
        return (ItemManager) this.applicationContext.getBean("itemManager");
    }
    
    /**
     * Gets the HistoryEnabledItemManager bean stored as the 'itemManager' 
     * bean in the ApplicationContext.  This method may return null if
     * the ItemManager for this application isn't a "History Enabled" one.
     */
    public HistoryEnabledItemManager getHistoryEnabledItemManager() {
        Object manager = this.applicationContext.getBean("itemManager");
        if (manager != null && manager instanceof HistoryEnabledItemManager) {
            return (HistoryEnabledItemManager) manager;
        } else {
            return null;
        }
    }
    
    /**
     * Gets the SearchManager bean stored as the 'searchnManager' bean
     * stored in the ApplicationContext.
     */
    public SearchManager getSearchManager() {
        return (SearchManager) this.applicationContext.getBean("searchManager");
    }
    
    
    /**
     * Gets the BatchManager bean stored as the 'searchnManager' bean
     * stored in the ApplicationContext if one is available.
     */
    public BatchManager getBatchManager() {
        return (BatchManager) applicationContext.getBean("batchManager");
    }
    
}
