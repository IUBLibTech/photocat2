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
package edu.indiana.dlib.catalog.accesscontrol;

import javax.servlet.http.HttpServletRequest;

/**
 * An interface for a class that manages the authentication
 * for a given HttpSession.  The expected interaction is as
 * follows:
 * 
 * <ul>
 *   <li>A resource requiring authentication invokes {@link getCurrentUser()}.</li>
 *   <li>
 *     If that method returns null, it issues a redirect to the URL returned 
 *     from {@link getAuthenticatinoRedirectUrl()}.
 *   </li>
 * </ul>
 * 
 * TODO: This should really be replaced with a more standard solution like JAAS 
 */
public interface AuthenticationManager {
    
    /**
     * Gets the user associated with the current session
     * if one has been authenticated.
     */
    public UserInfo getCurrentUser(HttpServletRequest currentRequest) throws AuthenticationException;
    
    /**
     * Gets a URL to which unauthenticated users should be
     * redirected in order to authenticate themselves.
     */
    public String getAuthenticationRedirectUrl(HttpServletRequest currentRequest) throws AuthenticationException;
    
    /**
     * Logs out the current user from this system.
     * @param session the current HttpSession
     */
    public void logOut(HttpServletRequest currentRequest);

}
