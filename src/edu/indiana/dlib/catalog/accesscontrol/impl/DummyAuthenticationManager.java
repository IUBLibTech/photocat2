/**
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
package edu.indiana.dlib.catalog.accesscontrol.impl;

import javax.servlet.http.HttpServletRequest;

import edu.indiana.dlib.catalog.accesscontrol.AuthenticationException;
import edu.indiana.dlib.catalog.accesscontrol.AuthenticationManager;
import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
/**
 * This is an AuthenticationManager implementation that *DOES NOT
 * AUTHENTICATE*.  It simply treats all users as authentic, with 
 * the information provided to the constructor.
 * 
 * <strong>THIS CLASS SHOULD ONLY BE USED FOR DEMONSTRATION PURPOSES
 * AND NEVER IN A SETTING WHERE ACTUAL ACCESS CONTROL IS NEEDED</strong>
 */
public class DummyAuthenticationManager implements AuthenticationManager {

    private UserInfo user;
	
    public DummyAuthenticationManager(String username, String email, String fullName) {
        user = new UserInfo(username);
        user.setEmailAddress(email);
        user.setFullName(fullName);
    }

    /**
     * The current implementation always returns the user 
     * supplied to the constructor.
     */
    public UserInfo getCurrentUser(HttpServletRequest currentRequest) throws AuthenticationException {
        return user;
    }

    /**
     * The current implementaiton always returns null because
     * this class <strong>doesn't really authenticate</strong>
     */
    public String getAuthenticationRedirectUrl(HttpServletRequest currentRequest) throws AuthenticationException {
        return null;
    }

    /**
     * The current implementation doesn't do anything.
     */
    public void logOut(HttpServletRequest currentRequest) {
        
    }
}
