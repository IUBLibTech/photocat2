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

import edu.indiana.dlib.catalog.accesscontrol.AuthenticationException;
import edu.indiana.dlib.catalog.accesscontrol.AuthenticationManager;

public abstract class AuthenticatedBorderPage extends BorderPage {

    private static final long serialVersionUID = 1L;
    
    /**
     * Attempts to determine the current user, and if it fails, 
     * redirects the AuthorizationRequredPage.
     */
    public boolean onSecurityCheck() {
        if (!super.onSecurityCheck()) {
            return false;
        }
        AuthenticationManager m = super.getAuthenticationManager();
        if (m == null) {
           ErrorPage errorPage = new ErrorPage();
           errorPage.errorMessage = getMessage("error-auth-manager-missing");
           errorPage.setPath("error.htm");
           setForward(errorPage);
        } else {
            try {
                this.user = m.getCurrentUser(getContext().getRequest());
                if (this.user == null) {
                    this.setRedirect(m.getAuthenticationRedirectUrl(getContext().getRequest()));
                    return false;
                } else {
                    return true;
                }
            } catch (AuthenticationException ex) {
                ErrorPage errorPage = new ErrorPage();
                errorPage.errorMessage = getMessage("error-auth-exception");
                errorPage.setPath("error.htm");
                setForward(errorPage);
                return false;
            }
        }
        return true;
    }
    
}
