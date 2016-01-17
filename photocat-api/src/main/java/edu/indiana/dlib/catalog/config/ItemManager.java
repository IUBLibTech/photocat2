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
package edu.indiana.dlib.catalog.config;

import java.util.Collection;
import java.util.Map;

import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
import edu.indiana.dlib.catalog.config.impl.FileSubmitter;

/**
 * An interface that abstracts away the storage, search and retrieval 
 * mechanism for items (both data files and metadata).  Implementations
 * of this could use any method for storing/accessing data/metadata on 
 * the back end, from using a fedora repository, to a RDBMS or even 
 * simple file storage.  The mechanism for grouping and linking data
 * can be defined by the implementation.
 */
public interface ItemManager {

    /**
     * Fetches the current version of the Item for the item
     * identified by the given id.
     * @param id the identifier of the item whose information
     * is to be fetched.
     * @return the Item object, or null if no such item exists.
     */
    public Item fetchItem(String id, CollectionConfiguration config) throws RepositoryException;
    
    public String getItemCollectionId(String itemId);
    
    /**
     * Fetches the current version of the Item for the item
     * identified by the given id including any private fields.
     * @param id the identifier of the item whose information
     * is to be fetched.
     * @return the Item object, or null if no such item exists.
     * @throws RepositoryException 
     */
    public Item fetchItemIncludingPrivateMetadata(String id, CollectionConfiguration config) throws RepositoryException;
    
    /**
     * Stores the current version of the ItemMetadata to the data store. 
     * @param itemMetadata the metadata to store
     * @throws OptimisticLockingException if the object has been modified
     * since it was retrieved for editing
     */
    public void saveItemMetadata(Item item, CollectionConfiguration config, UserInfo user) throws OptimisticLockingException, RepositoryException;

    /**
     * Removes the given item from the collection.
     * @param config the collection from which the item should be removed
     */
    public boolean removeItem(Item item, CollectionConfiguration config, UserInfo user) throws RepositoryException;
    
    /**
     * Returns true if the client only supports read operations (no cataloging).
     */
    public boolean isReadOnly();
    
    /**
     * A method to determine whether record creation is enabled
     * for the given collection.  This is often independent of 
     * whether the configuration allows record creation, but more
     * on whether the configuration is correct and complete.
     */
    public boolean isRecordCreationEnabled(CollectionConfiguration config);
    
    /**
     * Creates a new Item in the repository that is a member of the given
     * collection.  Callers should ensure that collections are configured to
     * allow record creation.  
     * @param config the collection to which the new item will belong
     * @return the identifier for the new item
     * @throws RepositoryException
     * @throws IllegalArgumentException if the supplied configuration indicates
     * that record creation is disallowed
     */
    public String createNewItem(CollectionConfiguration config, UserInfo user, Map<String, String> arguments) throws RepositoryException;
    
    /**
     * Returns a collection of the names of required arguments for the creation
     * of a new item.
     */
    public Collection<String> getRequiredArgumentNames(CollectionConfiguration config);

    /**
     * Gets an FileSubmitter for image files if per-item ImageFileSubmission is
     * available.
     * TODO: it would be possible to further generalize this by making the 
     * FileSubmitter smarter (including things like accepted mime type, etc.)
     * @param item the item to which we wish to submit files
     * @return a FileSubmitter that may or may not actually allow for file submission
     * (can be checked using helper methods on the file submitter).
     */
    public FileSubmitter getFileSubmitter(CollectionConfiguration c);
}
