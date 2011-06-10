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
package edu.indiana.dlib.catalog.config;

import java.util.Collection;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;

public interface ConfigurationManager {

    /**
     * Gets the currently known collection configurations.
     * @param clearCache if true, ensures that the underlying
     * storage implementation is queried for changes rather 
     * than just returning values from a cache.  Setting this
     * parameter to true may result in a substantially longer
     * period of time before this method returns and should
     * be used only when appropriate.
     */
    public Collection<CollectionConfiguration> getCollectionConfigurations(boolean clearCache) throws ConfigurationManagerException;
    
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
    public CollectionConfiguration getCollectionConfiguration(String id, boolean clearCache) throws ConfigurationManagerException;
    
}
