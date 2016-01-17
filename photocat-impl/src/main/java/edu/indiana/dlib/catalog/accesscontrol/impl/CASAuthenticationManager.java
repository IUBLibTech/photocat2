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
package edu.indiana.dlib.catalog.accesscontrol.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import edu.indiana.dlib.catalog.accesscontrol.AuthenticationException;
import edu.indiana.dlib.catalog.accesscontrol.AuthenticationManager;
import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
import edu.indiana.dlib.catalog.accesscontrol.UserNotAuthenticatedException;

/**
 * An AuthenticationManager implementation that redirects
 * the user to the CAS service, which authenticates the user
 * redirects them back to this page, at which point the 
 * remainder of the authentication routine is performed by the
 * {@link getCurrentUser()} method.
 */
public class CASAuthenticationManager implements AuthenticationManager {

    /**
     * The name of an attribe that if present on the request object
     * will be used as the original request URL.  This allows forwarding
     * before CAS redirection to properly handled.
     */
    public static final String PRE_FORWARD_REQUEST_ATTRIBUTE_NAME = "original-request";
    
    private static final String CURRENT_USER_ATTRIBUTE_NAME = "current-user";
    private static final String ORIGINAL_REQUEST_ATTRIBUTE_NAME = "original-request";
    private static final String AUTHENTICATION_STATE_ATTRIBUTE_NAME = "authentication-procedure-status";
    
    private String casValidationUrl;
    
    private String casLoginUrl;
    
    private String casLogoutUrl;
    
    private String cassvc;
    
    public CASAuthenticationManager(String casValidationUrl, String casLoginUrl, String casLogoutUrl, String cassvc) {
        this.casValidationUrl = casValidationUrl;
        this.casLoginUrl = casLoginUrl;
        this.casLogoutUrl = casLogoutUrl;
        this.cassvc = cassvc;
    }

    /**
     * Gets the current user, or null if the current user cannot yet be 
     * determined.
     * 
     * This method is responsible for several steps in the asynchronous
     * delegated CAS authentication routine.  
     * 
     * The first time it is called in a session it simply returns null, 
     * resulting in the caller invoking getAuthenticationRedirectURL()
     * which kicks off the CAS authentication process.
     * 
     * When it's called in a session that has already started the CAS 
     * authentication process, it's expected that there's a parameter 
     * "casticket" which is used to get the currently authenticated user.
     * After doing so and setting the "current-user" in the session
     * this method still returns null so that the redirection from the
     * getAuthenticationRedirectUrl() method redirects the user back
     * to their original request (with their original parameters, rather
     * than "casticket").  
     * @throws AuthenticationException if there's an IOException while 
     * validating the user
     * @throws UserNotAuthenticatedException if the user could not be
     * authenticated by CAS
     */
    public UserInfo getCurrentUser(HttpServletRequest currentRequest) throws AuthenticationException {
        try {
            UserInfo currentUser = (UserInfo) currentRequest.getSession().getAttribute(CURRENT_USER_ATTRIBUTE_NAME);
            if (currentUser == null && isAuthenticationProcedureStarted(currentRequest.getSession())) {
                // attempt to complete authentication based on cas
                String requestedUrl = currentRequest.getRequestURL().append(currentRequest.getQueryString() != null ? "?" + currentRequest.getQueryString() : "").toString();
                URL url = new URL(this.casValidationUrl + "?casticket=" + currentRequest.getParameter("casticket") + "&cassvc=" + cassvc + "&casurl=" + requestedUrl);
                //System.out.println("Validating ticket at: " + url.toString());
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                String valid = in.readLine();
                String username = in.readLine();
                if (valid.equals("no")) {
                    this.completeAuthenticationProcedure(currentRequest.getSession());
                    throw new UserNotAuthenticatedException("User could not be authenticated!");
                } else {
                    //System.out.println("User was authenticated");
                    UserInfo user = new UserInfo(username);
                    currentRequest.getSession().setAttribute(CURRENT_USER_ATTRIBUTE_NAME, user);
                    // authentication procedure will be completed by the redirect...
                    return null;
                }
            } else {
                //System.out.println("Not in authentication proceedure, returning user.");
                return currentUser;
            }
        } catch (IOException ex) {
            throw new AuthenticationException(ex);
        }
    }
    
    /**
     * Returns a URL to which the user's browser should be redirected 
     * to authenticate.  This method should only be called after
     * getCurrentUser() returns null indicating that there is no 
     * currently authenticated user associated with the session.
     * @throws IllegalStateException if there is a currently authenticated
     * user associated with the session
     */
    public String getAuthenticationRedirectUrl(HttpServletRequest currentRequest) throws AuthenticationException {
        UserInfo currentUser = (UserInfo) currentRequest.getSession().getAttribute(CURRENT_USER_ATTRIBUTE_NAME);
        if (currentUser != null && isAuthenticationProcedureStarted(currentRequest.getSession())) {
            // special case... we're done the authentication, but we want to redirect to the 
            // original requested page
            String url = (String) currentRequest.getSession().getAttribute(ORIGINAL_REQUEST_ATTRIBUTE_NAME);
            //System.out.println("User authenticated, redirecting to " + url);
            this.completeAuthenticationProcedure(currentRequest.getSession());
            return url;
        }
        if (currentUser != null || this.isAuthenticationProcedureStarted(currentRequest.getSession())) {
            throw new IllegalStateException();
        }
        
        // If we were were forwarded from somewhere else (perhaps the
        // CollectionPathRedirectionServlet we should use the original
        // requested URL, not the one it forwarded us to, because otherwise
        // when processing returns, the request attribute won't have been
        // set by the forwarding servlet.
        Object originalRequestUrl = currentRequest.getAttribute(PRE_FORWARD_REQUEST_ATTRIBUTE_NAME);
        String baseUrl = (originalRequestUrl == null ? currentRequest.getRequestURL().toString() : (String) originalRequestUrl);
        String url = baseUrl + (currentRequest.getQueryString() == null ? "" : "?" + currentRequest.getQueryString());
        currentRequest.getSession().setAttribute(ORIGINAL_REQUEST_ATTRIBUTE_NAME, url);
        this.startAuthenticationProcedure(currentRequest.getSession());
        //System.out.println("Sending redirect URL: " + this.casLoginUrl + "?cassvc=" + this.cassvc + "&casurl=" + url);
        return this.casLoginUrl + "?cassvc=" + this.cassvc + "&casurl=" + url;
    }


    public void logOut(HttpServletRequest currentRequest) {
        currentRequest.getSession().removeAttribute(CURRENT_USER_ATTRIBUTE_NAME);
        currentRequest.getSession().invalidate();
    }
    
    /**
     * Sets a flag (attribute) in the session indicating that 
     * a the authentication procedure has begun.
     */
    private void startAuthenticationProcedure(HttpSession session) {
        session.setAttribute(AUTHENTICATION_STATE_ATTRIBUTE_NAME, "started");
    }
    
    /**
     * Checks whether the authentication procedure has been started
     * by checking for a special session-scoped attribute.
     */
    private boolean isAuthenticationProcedureStarted(HttpSession session) {
        String status = (String) session.getAttribute(AUTHENTICATION_STATE_ATTRIBUTE_NAME);
        if (status != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Clears the session-scoped attribute that indicated that the authentication
     * procedure has been initiated.
     * @param session
     */
    private void completeAuthenticationProcedure(HttpSession session) {
        session.removeAttribute(AUTHENTICATION_STATE_ATTRIBUTE_NAME);
        session.removeAttribute(ORIGINAL_REQUEST_ATTRIBUTE_NAME);
    }
    

}
