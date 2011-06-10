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

import edu.indiana.dlib.catalog.accesscontrol.AuthorizationManager;
import edu.indiana.dlib.catalog.accesscontrol.AuthorizationSystemException;
import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.Item;

public abstract class GroupMembershipAuthorizationManager implements AuthorizationManager {

    public abstract String getAdminGroupName();
    
    public abstract String getCollectionAdminGroupName(String collectionId);
    
    public abstract String getCollectionCatalogerGroupName(String collectionId);
    
    public abstract String getUnitAdminGroupName(String unitId);
    
    public abstract String getUnitCatalogerGroupName(String unitId);

    public abstract Collection<String> getGroupsForUser(UserInfo user) throws AuthorizationSystemException;

    /**
     * Verifies that the item is in fact in the given collection but
     * then delegates to "canViewCollect()" because currently there is
     * no distinction between being able to view a collection and edit
     * its contents.
     */
    public boolean canEditItem(Item item, CollectionConfiguration collection, UserInfo user) throws AuthorizationSystemException {
        if (!item.getCollectionId().equals(collection.getId())) {
            throw new IllegalStateException(item.getId() + " is not in collection " + collection.getId() + "!");
        }
        return canViewCollection(collection, user);
    }

    /**
     * Returns true if the user is in the administrative or cataloger group for the
     * collection or unit or the global administrators list.  Otherwise returns false.
     */
    public boolean canViewCollection(CollectionConfiguration collection, UserInfo user) throws AuthorizationSystemException {
        String unitId = collection.getCollectionMetadata().getUnitId();
        String collectionId = collection.getId();
        Collection<String> groups = this.getGroupsForUser(user);
        if (groups.contains(this.getAdminGroupName())) {
            return true;
        } else if (unitId != null && groups.contains(getUnitAdminGroupName(unitId))) {
            return true;
        } else if (unitId != null && groups.contains(getUnitCatalogerGroupName(unitId))) {
            return true;
        } else if (groups.contains(getCollectionAdminGroupName(collectionId))) {
            return true;
        } else if (groups.contains(getCollectionCatalogerGroupName(collectionId))) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Returns true if the user is in the administrative group for the collection
     * or unit or the global administrators list.  Otherwise returns false.
     */
    public boolean canManageCollection(CollectionConfiguration collection, UserInfo user) throws AuthorizationSystemException {
        String unitId = collection.getCollectionMetadata().getUnitId();
        String collectionId = collection.getId();
        Collection<String> groups = this.getGroupsForUser(user);
        if (groups.contains(this.getAdminGroupName())) {
            return true;
        } else if (unitId != null && groups.contains(getUnitAdminGroupName(unitId))) {
            return true;
        } else if (groups.contains(getCollectionAdminGroupName(collectionId))) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Returns true if the user can manage the collection.
     */
    public boolean canRemoveItem(Item item, CollectionConfiguration collection, UserInfo user) throws AuthorizationSystemException {
        return canManageCollection(collection, user);
    }
    
    /**
     * Returns true if the user can manage the application.
     */
    public boolean canManageApplication(UserInfo user) throws AuthorizationSystemException {
        return this.getGroupsForUser(user).contains(this.getAdminGroupName());
    }

    
    
}
