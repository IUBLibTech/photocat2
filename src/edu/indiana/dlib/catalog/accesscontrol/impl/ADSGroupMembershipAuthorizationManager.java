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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.naming.AuthenticationException;
import javax.naming.NamingException;

import edu.indiana.dlib.catalog.accesscontrol.AuthorizationSystemException;
import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
import edu.iu.uis.sit.util.directory.AdsHelper;
import edu.iu.uis.sit.util.directory.AdsPerson;

public class ADSGroupMembershipAuthorizationManager extends GroupMembershipAuthorizationManager {

    public static String ADS_GROUPS_PROPERTY_NAME = "ADS-GROUPS";
    
    private String adsUsername;
    
    private String adsPassword;
    
    public ADSGroupMembershipAuthorizationManager(String username, String password) {
        this.adsUsername = username;
        this.adsPassword = password;
    }
    
    public String getAdminGroupName() {
        return "BL-LDLP-PHOTOCAT-ADMIN";
    }

    public String getCollectionAdminGroupName(String collectionId) {
        return "BL-LDLP-PHOTOCAT-" + collectionId.replace("/", "-").toUpperCase() + "-ADMIN";
    }

    public String getCollectionCatalogerGroupName(String collectionId) {
        return "BL-LDLP-PHOTOCAT-" + collectionId.replace("/", "-").toUpperCase() + "-CATALOGER";
    }

    public String getUnitAdminGroupName(String unitId) {
        return "BL-LDLP-PHOTOCAT-" + unitId.replace("/", "-").toUpperCase() + "-ADMIN";
    }

    public String getUnitCatalogerGroupName(String unitId) {
        return "BL-LDLP-PHOTOCAT-" + unitId.replace("/", "-").toUpperCase() + "-CATALOGER";
    }
    
    public Collection<String> getGroupsForUser(UserInfo user) throws AuthorizationSystemException {
        // first see if information has been fetched
        Object initializedGroups = user.getProperty(ADS_GROUPS_PROPERTY_NAME);
        if (initializedGroups != null) {
            // already initialized, just return the collection
            return (Collection<String>) initializedGroups;
        } else {
            // fetch groups (and other info) from ADS
            Set<String> groupSet = new HashSet<String>();
            AdsHelper helper =  new AdsHelper(adsUsername, adsPassword);
            try {
                AdsPerson adsPerson = helper.getAdsPerson(user.getUsername());
                user.setEmailAddress(adsPerson.getMail());
                user.setFullName(adsPerson.getDisplayName());
                
                for (Object group : helper.getGroups(user.getUsername(), 10)) {
                    String groupName = (String) group;
                    if (groupName.startsWith("BL-LDLP-PHOTOCAT")) {
                        groupSet.add(groupName.toUpperCase());
                    }
                }
                
                user.setProperty(ADS_GROUPS_PROPERTY_NAME, groupSet);
                return groupSet;
            } catch (AuthenticationException ex) {
                throw new AuthorizationSystemException(ex);
            } catch (NamingException ex) {
                throw new AuthorizationSystemException(ex);
            }

        }
    }


}
