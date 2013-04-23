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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import edu.indiana.dlib.catalog.search.BrowseSet;
import edu.indiana.dlib.catalog.search.structured.SearchConstraint;

public interface ConfigurationManager {

    public static final String UNIT = "unit";
    public static final String COLLECTION = "collection";
    
    public void clearCache();
    
    /**
     * Gets the currently known collection configurations.
     * @param clearCache if true, ensures that the underlying
     * storage implementation is queried for changes rather 
     * than just returning values from a cache.  Setting this
     * parameter to true may result in a substantially longer
     * period of time before this method returns and should
     * be used only when appropriate.
     */
    public Collection<CollectionConfiguration> getCollectionConfigurations(String type, boolean onlypublic) throws ConfigurationManagerException;
    
    public Collection<String> getInvalidCollectionIds();
    
    public Collection<CollectionConfiguration> getChildren(String id, String type, boolean onlypublic) throws ConfigurationManagerException;
    
    public void setParent(String itemId, String parentId) throws ConfigurationManagerException;
    
    public CollectionConfiguration getParent(String id, boolean onlypublic) throws ConfigurationManagerException;
    
    public Collection<CollectionConfiguration> getOrphans(String type, boolean onlypublic) throws ConfigurationManagerException;
    
    public List<SearchConstraint> getPublicRecordsSearchConstraints() throws ConfigurationManagerException;
    
    public boolean isItemPublic(Item item) throws ConfigurationManagerException;
    
    /**
     * Gets the current collection configuration for the collection
     * with the given identifier (or null if no such collection exists).
     * Implementations of this method may cache configurations but 
     * must ensure that they deliver the current working version from
     * this method. This configuration is the aggregate of the specified
     * configuration and the default configuration (from the definition
     * file).
     * @param clearCache if true, ensures that the underlying
     * storage implementation is queried for changes rather 
     * than just returning values from a cache.  Setting this
     * parameter to true may result in a substantially longer
     * period of time before this method returns and should
     * be used only when appropriate.
     */
    public CollectionConfiguration getCollectionConfiguration(String id) throws ConfigurationManagerException;

    public CollectionConfiguration createNewCollection(String id, String type, CollectionConfiguration parent) throws ConfigurationManagerException;
    
    /**
     * Stores a new or updated version of the collection configuration.
     * This method fails if the new configuration can't be properly
     * loaded (because perhaps it references an unresolved definition
     * file). 
     */
    public void storeConfiguration(CollectionConfigurationData config) throws ConfigurationManagerException;
    
    /**
     * Stores an updated version of the field definitions.
     * This method fails if the new version of the field definition
     * results in any exceptions while reloading all of the
     * collection configurations.
     */
    public List<Definitions> listKnownDefinitionSets();
    
    /**
     * Stores a file with the collection.  This could be the icon or a MODS transformation
     * and the returned URL should be added to the configuration.
     */
    public String storeCollectionFile(CollectionConfiguration c, InputStream fileIs, String mimeType, String id, boolean overwriteIfPresent) throws ConfigurationManagerException;
    
    /**
     * Gets the set of facets appropriate for global searches.  Implementations
     * may choose to provide an arbitrary list, or compute one based on the 
     * facets defined for the public collections.
     */
    public List<BrowseSet> getGlobalFacets();
    
    public boolean allowPublicAccess();
    
}
