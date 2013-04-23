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

import javax.servlet.ServletContext;

import org.apache.click.Page;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
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
        applicationContext = context;
        getContext().getServletContext().setAttribute(APPLICATION_CONTEXT, context);
    }
    
    public static final ApplicationContext getApplicationContext(ServletContext sc) {
        return (ApplicationContext) sc.getAttribute(APPLICATION_CONTEXT);
    }
    
    public boolean isCatalogingEnabled() {
        try {
            return !getItemManager().isReadOnly();
        } catch (Throwable t) {
            return false;
        }
    }
    
    public boolean isPublicBrowsingEnabled() {
        return getConfigurationManager().allowPublicAccess();
    }
    
    /**
     * Gets the AuthenticationManager bean stored as the 'authenticationManager' 
     * bean in the ApplicationContext.
     */
    public AuthenticationManager getAuthenticationManager() {
        try {
            return (AuthenticationManager) applicationContext.getBean("authenticationManager");
        } catch (NoSuchBeanDefinitionException ex) {
            return null;
        }
    }
    
    /**
     * Gets the AuthorizationManager bean stored as the 'authorizationManager' 
     * bean in the ApplicationContext.
     */
    public AuthorizationManager getAuthorizationManager() {
        try {
            return (AuthorizationManager) applicationContext.getBean("authorizationManager");
        } catch (NoSuchBeanDefinitionException ex) {
            return null;
        }
    }
    
    /**
     * Gets the ConfigurationManager bean stored as the 'configurationManager' bean
     * stored in the ApplicationContext.
     */
    public ConfigurationManager getConfigurationManager() {
        try {
            return (ConfigurationManager) applicationContext.getBean("configurationManager");
        } catch (NoSuchBeanDefinitionException ex) {
            return null;
        }
    }
    
    /**
     * Gets the ItemManager bean stored as the 'itemManager' bean in the
     * ApplicationContext.
     */
    public ItemManager getItemManager() {
        try {
            return (ItemManager) applicationContext.getBean("itemManager");
        } catch (NoSuchBeanDefinitionException ex) {
            return null;
        }
    }
    
    /**
     * Gets the HistoryEnabledItemManager bean stored as the 'itemManager' 
     * bean in the ApplicationContext.  This method may return null if
     * the ItemManager for this application isn't a "History Enabled" one.
     */
    public HistoryEnabledItemManager getHistoryEnabledItemManager() {
        try {
            Object manager = applicationContext.getBean("itemManager");
            if (manager != null && manager instanceof HistoryEnabledItemManager) {
                return (HistoryEnabledItemManager) manager;
            } else {
                return null;
            }
        } catch (NoSuchBeanDefinitionException ex) {
            return null;
        }
    }
    
    /**
     * Gets the SearchManager bean stored as the 'searchnManager' bean
     * stored in the ApplicationContext.
     */
    public SearchManager getSearchManager() {
        try {
            return (SearchManager) applicationContext.getBean("searchManager");
        } catch (NoSuchBeanDefinitionException ex) {
            return null;
        }
    }
    
    
    /**
     * Gets the BatchManager bean stored as the 'searchManager' bean
     * stored in the ApplicationContext if one is available.
     */
    public BatchManager getBatchManager() {
        try {
            return (BatchManager) applicationContext.getBean("batchManager");
        } catch (NoSuchBeanDefinitionException ex) {
            return null;
        }
    }
    
}
