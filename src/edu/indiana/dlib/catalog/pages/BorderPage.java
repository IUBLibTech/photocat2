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

import org.apache.click.extras.control.Menu;
import org.apache.click.extras.security.AccessController;

import edu.indiana.dlib.catalog.accesscontrol.AuthenticationException;
import edu.indiana.dlib.catalog.accesscontrol.UserInfo;

/**
 * A template that contains the header and footer and all
 * applicable navigation links.
 */
public abstract class BorderPage extends ApplicationPage {

    /**
     * The title displayed in the header.  This is set during
     * onRender() by this class by invoking 'getTitle(). Subclasses
     * should override that method to replace the title.
     */
    public String title;
    
    /**
     * The breadcrumbs for this page.  This set set during onRender()
     * by this class by invoking 'getBreadcrumbs()'.  Subclasses should
     * override that method to replace the breadcrumbs.
     */
    public String breadcrumbs;
    
    /**
     * The menu for this page.  This is set during onRender() by this
     * class by invoking 'getMenu()'.  Subclasses should override
     * that method to replace the menu.
     */
    public Menu rootMenu;
    
    /**
     * An error message that when present is formatted and displayed
     * to the user.
     */
    public String errorMessage;
    
    /**
     * Will be set during the onSecurityCheck() method with
     * the current user if the use can be authenticated.
     */
    public UserInfo user;

	public String getTemplate() {
		return "border-template.htm";
	}
	
	/**
	 * Sets the current user if possible and returns true.
	 */
	public boolean onSecurityCheck() {
	    try {
            this.user = getAuthenticationManager().getCurrentUser(getContext().getRequest());
        } catch (AuthenticationException ex) {
            // leave the user as null and ignore this error
        }
	    return true;
	}
	
	public void onRender() {
	    super.onRender();
	    rootMenu = getMenu();
	    breadcrumbs = getBreadcrumbs();
	    title = getTitle();
	}
	
	protected Menu getMenu() {
	    return null;
	}
	
	protected Menu createMenu(String label, String path, AccessController accessController) { 
        Menu menu = new Menu(); 
        menu.setLabel(label); 
        menu.setPath(path); 
        menu.setTitle(label);
        menu.setAccessController(accessController);
        return menu; 
    } 
	
	protected String getBreadcrumbs() {
	    return getMessage("breadcrumbs");
	}
	
	protected String getTitle() {
	    return getMessage("title");
	}

}
