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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.DataFormatException;

/**
 * A ConfigurationManager implementation that exposes configurations
 * based on the presence of parsable configuration XML files in
 * a known directory on the local file system.
 */
public class DirectoryConfigurationManager implements ConfigurationManager {

    private Logger LOGGER = Logger.getLogger(DirectoryConfigurationManager.class); 
    
    /**
     * The directory containing the configuration files.  Methods
     * that read from this directory, to ensure thread-safety
     * and efficiency, should synchronize on this object.
     */
    private File configDir;
    
    private File definitionDir;
    
    private List<FileCollectionConfiguration> configList;
    
    private Map<String, FileCollectionConfiguration> idToConfigMap;
    
    private Map<String, FileFieldDefinitions> idToDefinitionsMap;
    
    /**
     * A map with the time the last failed attempt to load a file
     * was completed.  This is tracked to prevent the resource-
     * intensive process of reloading a configuration that is
     * invalid.
     */
    private Map<File, Date> loadErrorMap;
    
    /**
     * The principal consturctor that accepts file path Strings
     * for the directory containing configuration XML files and
     * definition XML files.  These paths will be treated as
     * relative to the environment variable "PHOTOCAT_HOME" if
     * that environment variable is set and they do not start with
     * a "/" character.  Otherwise they will be passed as the sole
     * parameter to the File() constructor and parsed accordingly.
     * @param configDir the directory containing configuration 
     * XML files.
     * @param definitionDir the directory containing definition
     * XML files.
     */
    public DirectoryConfigurationManager(String configDir, String definitionDir) {
        String photocatHome = System.getenv("PHOTOCAT_HOME");
        if (photocatHome != null && !configDir.startsWith("/")) {
            File homeDir = new File(photocatHome);
            this.configDir = new File(homeDir, configDir);
        } else {
            this.configDir = new File(configDir);
        }
        this.configDir.mkdirs();
        if (photocatHome != null && !definitionDir.startsWith("/")) {
            File homeDir = new File(photocatHome);
            this.definitionDir = new File(homeDir, definitionDir);
        } else {
            this.definitionDir = new File(definitionDir);
        }
        this.definitionDir.mkdir();
        this.configList = new ArrayList<FileCollectionConfiguration>();
        this.idToConfigMap = new HashMap<String, FileCollectionConfiguration>();
        this.loadErrorMap = new HashMap<File, Date>();
        this.idToDefinitionsMap = new HashMap<String, FileFieldDefinitions>();
    }
    
    /**
     * Ensures that the latest version of the configuration files
     * are represented in the map. 
     */
    private void refreshConfigurationList() {
        synchronized (this.configDir) {
            List<FileCollectionConfiguration> newConfigList = new ArrayList<FileCollectionConfiguration>();
            
            // load all changed definitions
            Map<String, FileFieldDefinitions> newIdToDefinitionsMap = new HashMap<String, FileFieldDefinitions>();
            for (File file : this.definitionDir.listFiles()) {
                if (file.getName().endsWith(".xml")) {
                    FileFieldDefinitions existingDefs = null;
                    for (FileFieldDefinitions defs : this.idToDefinitionsMap.values()) {
                        if (defs.getFile().equals(file)) {
                            existingDefs = defs;
                            break;
                        }
                    }
                    long modDate = file.lastModified();
                    if (existingDefs == null || existingDefs.getLastModificationDate() < modDate) {
                        try {
                            FileFieldDefinitions def = new FileFieldDefinitions(file);
                            newIdToDefinitionsMap.put(def.getId(), def);
                            LOGGER.debug("Parsed definition file, \"" + def.getId() + "\".");
                        } catch (IOException ex) {
                            LOGGER.error("IOException for the definition file at \"" + file.getAbsolutePath() + "\" (retained prior configuration)!", ex);
                        } catch (DataFormatException ex) {
                            LOGGER.error("Data exception while parsing the definition file at \"" + file.getAbsolutePath() + "\"!", ex);
                        }
                    }
                }
            }
            
            Map<String, FileCollectionConfiguration> newIdToConfigMap = new HashMap<String, FileCollectionConfiguration>();
            for (File file : configDir.listFiles()) {
                if (file.getName().endsWith(".xml")) {
                    FileCollectionConfiguration existingConfig = null;
                    for (FileCollectionConfiguration config : this.configList) {
                        if (config.getFile().equals(file)) {
                            existingConfig = config;
                            break;
                        }
                    }
                    long modDate = file.lastModified();
                    if (existingConfig == null || existingConfig.getLastModificationDate() < modDate) {
                        if (this.loadErrorMap.containsKey(file) && this.loadErrorMap.get(file).after(new Date(modDate))) {
                            // don't attempt to reload this configuration yet
                            LOGGER.info("Skipping the reloading of \"" + file.getName() + "\" because it hasn't been modified since the last load error.");
                            continue;
                        }
                        try {
                            FileCollectionConfigurationData configData = new FileCollectionConfigurationData(file);
                            if (!newIdToDefinitionsMap.containsKey(configData.getDefinitionId())) {
                                LOGGER.error("No definition file found with id \"" + configData.getDefinitionId() + "\", cannot load configuration \"" + configData.getId() + "\".");
                                continue;
                            }
                            FileCollectionConfiguration config = new FileCollectionConfiguration(configData, newIdToDefinitionsMap.get(configData.getDefinitionId()));
                            if (newIdToConfigMap.containsKey(config.getCollectionMetadata().getId())) {
                                LOGGER.error("Duplicate collection identifier, " + config.getCollectionMetadata().getId() + ", found in file \"" + file.getAbsolutePath() + "\", skipping configuration!");
                            } else {
                                newConfigList.add(config);
                                newIdToConfigMap.put(config.getCollectionMetadata().getId(), config);
                                this.loadErrorMap.remove(file);
                            }
                        } catch (DataFormatException ex) {
                            this.loadErrorMap.put(file, new Date(modDate));
                            if (existingConfig != null) {
                                LOGGER.error("Data exception for the configuration file at \"" + file.getAbsolutePath() + "\" (retained prior configuration)!", ex);
                                newConfigList.add(existingConfig);
                                newIdToConfigMap.put(existingConfig.getCollectionMetadata().getId(), existingConfig);
                            } else {
                                LOGGER.error("Data exception while parsing the configuration file at \"" + file.getAbsolutePath() + "\"!", ex);
                            }
                        } catch (IOException ex) {
                            if (existingConfig != null) {
                                LOGGER.error("IOException for the configuration file at \"" + file.getAbsolutePath() + "\" (retained prior configuration)!", ex);
                                newConfigList.add(existingConfig);
                                newIdToConfigMap.put(existingConfig.getCollectionMetadata().getId(), existingConfig);
                            } else {
                                LOGGER.error("IOException while parsing the configuration file at \"" + file.getAbsolutePath() + "\"!", ex);
                            }
    
                        }
                    } else {
                        newConfigList.add(existingConfig);
                        newIdToConfigMap.put(existingConfig.getCollectionMetadata().getId(), existingConfig);
                    }
                }
            }
            synchronized (this) {
                this.configList = newConfigList;
                this.idToConfigMap = newIdToConfigMap;
                this.idToDefinitionsMap = newIdToDefinitionsMap;
            }
        }
    }
    
    
    public synchronized CollectionConfiguration getCollectionConfiguration(String id, boolean clearCache) {
        this.refreshConfigurationList();
        return this.idToConfigMap.get(id);
    }

    public synchronized Collection<CollectionConfiguration> getCollectionConfigurations(boolean clearCache) {
        this.refreshConfigurationList();
        ArrayList<CollectionConfiguration> results = new ArrayList<CollectionConfiguration>();
        for (FileCollectionConfiguration config : this.configList) {        
            results.add(config);
        }
        return results;
    }

}
