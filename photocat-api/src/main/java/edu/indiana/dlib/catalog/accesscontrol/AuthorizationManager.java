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

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.Item;

/**
 * An interface whose implementation is responsible for making
 * authorization decisions.  Underlying implementations may
 * consult a database, access control lists, ADS group membership
 * or anything else to make the appropriate decision.
 * 
 * Implementations of this interface should be written to be 
 * thread safe because there's intended to be one 
 * AuthorizationManager instance for the entire application.
 */
public interface AuthorizationManager {

    /**
     * Checks whether the described user can manage the application
     * as a whole.  This should be reserved for the smallest group
     * of well-trained individuals as typically experimental or 
     * extremely powerful features are only enabled for this group.
     * @param user the user whose authorization is in question
     * @return true if the user can perform application management
     * operations.
     */
    public boolean canManageApplication(UserInfo user) throws AuthorizationSystemException;
    
    /**
     * Checks whether the described user can view the contents
     * and metadata for the collection with the given configuration.
     * @param collection the collection configuration
     * @param user the user whose authorization is in question
     * @return true if the user can view the collection, false 
     * otherwise
     */
    public boolean canViewCollection(CollectionConfiguration collection, CollectionConfiguration unit, UserInfo user) throws AuthorizationSystemException;

    /**
     * Checks whether the described user can manage the
     * collection with the given configuration.  This may include
     * such things as updating the configuration and vocabulary
     * sources.
     * @param collection the collection configuration
     * @param user the user whose authorization is in question
     * @return true if the user can view the collection, false 
     * otherwise
     */
    public boolean canManageCollection(CollectionConfiguration collection, CollectionConfiguration unit, UserInfo user) throws AuthorizationSystemException;
    
    /**
     * Checks whether the described user can manage the
     * collection with the given configuration.  This may include
     * such things as updating the configuration and vocabulary
     * sources.
     * @param collection the collection configuration
     * @param user the user whose authorization is in question
     * @return true if the user can view the collection, false 
     * otherwise
     */
    public boolean canManageUnit(CollectionConfiguration unit, UserInfo user) throws AuthorizationSystemException;
    
    /**
     * Checks whether the described user can edit the item in the
     * context of the given collection.
     * @param item the item to be edited
     * @param collection the collection configuration
     * @param user the user whose authorization is in question
     * @return true if the user can view the collection, false 
     * otherwise
     */
    public boolean canEditItem(Item item, CollectionConfiguration collection, CollectionConfiguration unit, UserInfo user) throws AuthorizationSystemException;
    
    /**
     * Checks whether the described user can remove the item from
     * the given collection.
     * @param item the item to be removed
     * @param collection the collection configuration
     * @param user the user whose authorization is in question
     * @return true if the user can remove the given item
     */
    public boolean canRemoveItem(Item item, CollectionConfiguration collection, CollectionConfiguration unit, UserInfo user) throws AuthorizationSystemException;
}
