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
package edu.indiana.dlib.catalog.config.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.CollectionConfigurationData;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.ConfigurationManagerException;
import edu.indiana.dlib.catalog.config.Definitions;
import edu.indiana.dlib.catalog.config.impl.fedora.FedoraException;
import edu.indiana.dlib.catalog.config.impl.fedora.FedoraRestApiWrapper;
import edu.indiana.dlib.catalog.config.impl.fedora.DatastreamProfile;

/**
 * <p>
 *   A ConfigurationManager that loads collection configurations and
 *   field definitions from a fedora repository.
 * </p>
 * <p>
 *   This class requires that the underlying fedora repository have 
 *   the resource index enabled, have content models that includes
 *   an XML datastream with the collection configuration and field
 *   definition respectively.
 * </p>
 */
public class FedoraConfigurationManager implements ConfigurationManager {

    /**
     * A fedora client that is used to access the Fedora
     * repository.  This instance should probably be read-only
     * but must be provided with credentials for API-M access.
     */
    private FedoraRestApiWrapper fedora;
    
    /**
     * The content model for Collection objects that contain
     * the collection configuration.
     */
    private String collectionContentModel;
    
    /**
     * The identifier of the datastream that contains the
     * collection configuration.
     */
    private String collectionConfigDSID;
    
    /**
     * The content model for Definition objects that contain
     * a definition XML.
     */
    private String definitionContentModel;
    
    /**
     * The identifier of the datastream that contains the 
     * definition XML.
     */
    private String definitionDSID;
    
    private Map<String, String> pidToFreshnessMap;
    
    private Map<String, CollectionConfigurationData> idToConfigDataMap;
    
    private Map<String, Definitions> idToDefinitionsMap;
    
    private Map<String, String> pidToIdMap;
    
    private List<CollectionConfiguration> cache;
    
    public static void main(String args[]) throws ConfigurationManagerException {
        FedoraConfigurationManager configManager = new FedoraConfigurationManager("fedoraAdmin", "adminD3v", "fedora-dev.dlib.indiana.edu", "fedora", 8080, "cmodel:photocat-config", "field-configuration.xml", "cmodel:photocat-definition", "field-definitions.xml");
        for (CollectionConfiguration config : configManager.getCollectionConfigurations(true)) {
            System.out.println(config.getId() + " - " + config.getCollectionMetadata().getDescription());
        }
    }
    
    /**
     * Creates a new FedoraConfigurationManager with all the configuration requirements.
     * @param username username for the fedora repository
     * @param password password for the fedora repository
     * @param host the host of the fedora repository ie. fedora.example.com
     * @param contextName the context name (or path) for the fedora web application
     *     (typically "fedora")
     * @param port the port the application is running on
     * @param confCM the pid of the formal content model that contains the
     *     collection configuration as a datastream
     * @param confDSID the datastream id for the XML datastream that contains
     *     the collection configuration on objects that conform to the confCM 
     *     content model.
     * @param defCM the pid of the formal content model that contains the 
     *     field definition as a datastream
     * @param defDSID the datastream id for the XML datastream that contains
     *     the field definition on objects that conform to the defCM content
     *     model.
     */
    public FedoraConfigurationManager(String username, String password, String host, String contextName, Integer port, String confCM, String confDSID, String defCM, String defDSID) {
        fedora = new FedoraRestApiWrapper(username, password, host, contextName, port, true);
        this.collectionConfigDSID = confDSID;
        this.collectionContentModel = confCM;
        this.definitionDSID = defDSID;
        this.definitionContentModel = defCM;
        
        this.pidToFreshnessMap = new HashMap<String, String>();
        this.idToConfigDataMap = new HashMap<String, CollectionConfigurationData>();
        this.idToDefinitionsMap = new HashMap<String, Definitions>();
        this.pidToIdMap = new HashMap<String, String>();
    }
    
    public CollectionConfiguration getCollectionConfiguration(String id, boolean clearCache) throws ConfigurationManagerException {
        long start = System.currentTimeMillis();
        try {
            // get current config data
            CollectionConfigurationData cachedConfigData = this.idToConfigDataMap.get(id);
            if (cachedConfigData == null || clearCache) {
                String configPid = lookupObjectPid(id);
                if (configPid == null) {
                    return null;
                }
                String configModDate = fedora.getDatastreamProperty(configPid, this.collectionConfigDSID, DatastreamProfile.DatastreamProperty.DS_CREATE_DATE);
                if (cachedConfigData == null || (this.pidToFreshnessMap.containsKey(configPid) && this.pidToFreshnessMap.get(configPid).compareTo(configModDate) <= 0)) {
                    // reload from source because it was not cached or the cache is
                    // out of date
                    synchronized (this) {
                        cachedConfigData = new DefaultCollectionConfigurationData(fedora.getDatastream(configPid, collectionConfigDSID));
                        pidToFreshnessMap.put(configPid, configModDate);
                        idToConfigDataMap.put(cachedConfigData.getId(), cachedConfigData);
                    }
                }
            }
            
            // get the current backing definition
            Definitions cachedDef = this.idToDefinitionsMap.get(cachedConfigData.getDefinitionId());
            if (cachedDef == null || clearCache) {
                String defPid = lookupObjectPid(cachedConfigData.getDefinitionId());
                if (defPid == null) {
                    return null;
                }
                String defModDate = fedora.getDatastreamProperty(defPid, this.definitionDSID, DatastreamProfile.DatastreamProperty.DS_CREATE_DATE);
                //if (this.pidToFreshnessMap.containsKey(defPid)) {
                //    System.out.println(cachedConfigData.getDefinitionId() + ": \"" + this.pidToFreshnessMap.get(defPid) + "\".compareTo("+  defModDate + ") = " +  this.pidToFreshnessMap.get(defPid).compareTo(defModDate));
                //}
                if (cachedDef == null || (this.pidToFreshnessMap.containsKey(defPid) && this.pidToFreshnessMap.get(defPid).compareTo(defModDate) <= 0)) {
                    synchronized (this) {
                        cachedDef = new DefaultFieldDefinitions(fedora.getDatastream(defPid, this.definitionDSID));
                        pidToFreshnessMap.put(defPid, defModDate);
                        idToDefinitionsMap.put(cachedDef.getId(), cachedDef);
                    }
                }
            }
            
            return new DefaultCollectionConfiguration(cachedConfigData, cachedDef);
        } catch (Throwable t) {
            throw new ConfigurationManagerException(t);
        } finally {
            //ClickUtils.getLogService().debug("get collection configuration - " + (System.currentTimeMillis() - start) + " ms");
        }
    }
    
    private synchronized String lookupObjectPid(String id) throws FedoraException, IOException {
        for (Map.Entry<String, String> mapEntry : this.pidToIdMap.entrySet()) {
            if (mapEntry.getValue().equals(id)) {
                return mapEntry.getKey();
            }
        }
        List<String> pids = fedora.dcIdentifierLookup(id);
        if (pids.size() == 1) {
            this.pidToIdMap.put(pids.get(0), id);
            return pids.get(0);
        } else {
            return null;
        }
    }
    
    public synchronized Collection<CollectionConfiguration> getCollectionConfigurations(boolean clearCache) throws ConfigurationManagerException {
        long start = System.currentTimeMillis();
        if (clearCache) {
            this.cache = null;
        }
        List<CollectionConfiguration> configs = null;
        try {
            if (this.cache != null) {
                configs = this.cache;
            } else {
                configs = new ArrayList<CollectionConfiguration>();
                for (String configPid : fedora.getRelatedPids(collectionContentModel, "fedora-model:hasModel")) {
                    String id = this.lookupObjectPid(configPid);
                    configs.add(this.getCollectionConfiguration(id, clearCache));
                }
            }
        } catch (FedoraException ex) {
            throw new ConfigurationManagerException(ex);
        } catch (IOException ex) {
            throw new ConfigurationManagerException(ex);
        } finally {
            //ClickUtils.getLogService().debug("get collection configurations - " + (System.currentTimeMillis() - start) + " ms");
        }
        this.cache = configs;
        return configs;
    }
    
}
