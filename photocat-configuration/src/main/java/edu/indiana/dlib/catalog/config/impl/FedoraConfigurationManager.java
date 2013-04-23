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
package edu.indiana.dlib.catalog.config.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.CollectionConfigurationData;
import edu.indiana.dlib.catalog.config.ConfigurationManager;
import edu.indiana.dlib.catalog.config.ConfigurationManagerException;
import edu.indiana.dlib.catalog.config.DataFormatException;
import edu.indiana.dlib.catalog.config.Definitions;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.impl.fedora.DCRecord;
import edu.indiana.dlib.catalog.config.impl.fedora.DatastreamProfile;
import edu.indiana.dlib.catalog.config.impl.fedora.FedoraException;
import edu.indiana.dlib.catalog.config.impl.fedora.FedoraRestApiWrapper;
import edu.indiana.dlib.catalog.config.impl.fedora.FedoraRestApiWrapper.ControlGroup;
import edu.indiana.dlib.catalog.search.BrowseSet;
import edu.indiana.dlib.catalog.search.impl.CollectionBrowseSet;
import edu.indiana.dlib.catalog.search.impl.DateBrowseSet;
import edu.indiana.dlib.catalog.search.impl.FieldPartBrowseSet;
import edu.indiana.dlib.catalog.search.structured.SearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.AndSearchConstraintGroup;
import edu.indiana.dlib.catalog.search.structured.constraints.CollectionSearchConstraint;
import edu.indiana.dlib.catalog.search.structured.constraints.OrSearchConstraintGroup;
import edu.indiana.dlib.catalog.search.structured.constraints.SerializableSearchConstraint;

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
    
    private String childRelationshipURI;
    
    private Map<String, String> pidToFreshnessMap;
    private Map<String, CollectionConfigurationData> idToConfigDataMap;
    private Map<String, String> idToParentIdMap;
    private Map<String, Definitions> idToDefinitionsMap;
    private Map<String, String> pidToIdMap;
    private List<String> cachedConfigIds;
    private Set<String> brokenConfigurationIds;
    
    private boolean allowPublicAccess;
    
    private Logger logger;
    
    private Document defaultConfigurationDoc;
    
    /** 
     * A directory path that all dropbox directories should be contained in.
     */
    private String dropboxBaseDir;
    
    /**
     * A directory path that all ingest directories should be contained in.
     */
    private String ingestBaseDir;

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
     * @param relationship the relationship (stored on a collection object) that
     *     links it to its parent.
     * @param defaultConfigUrl the URL of a serialized CollectionConfigurationData
     *     object that can serve as a default for new collections.  Note that only
     *     the id and type will be initially replaced, so all other values should
     *     be placeholders.  
     * @param allowPublicAccessStr a string, that if equal to "true" indicates
     *     that this FedoraConfigurationManager is meant to be backing a public
     *     web site (the Image Collections Online site).  This single switch
     *     enabled all of the content for the image collections online.
     */
    public FedoraConfigurationManager(String username, String password, String host, String contextName, Integer port, String confCM, String confDSID, String defCM, String defDSID, String relationship, String defaultConfigUrl, String allowPublicAccessStr) {
        this(username, password, host, contextName, port, confCM, confDSID, defCM, defDSID, relationship, defaultConfigUrl, null, null, allowPublicAccessStr);
    }
    public FedoraConfigurationManager(String username, String password, String host, String contextName, Integer port, String confCM, String confDSID, String defCM, String defDSID, String relationship, String defaultConfigUrl, String dropbox, String ingest, String allowPublicAccessStr) {
        logger = Logger.getLogger(this.getClass());
        fedora = new FedoraRestApiWrapper(username, password, host, contextName, port, false);
        collectionConfigDSID = confDSID;
        collectionContentModel = confCM;
        definitionDSID = defDSID;
        definitionContentModel = defCM;
        childRelationshipURI = relationship;
        dropboxBaseDir = dropbox;
        ingestBaseDir = ingest;
        
        pidToFreshnessMap = new HashMap<String, String>();
        idToConfigDataMap = new HashMap<String, CollectionConfigurationData>();
        idToParentIdMap = new HashMap<String, String>();
        idToDefinitionsMap = new HashMap<String, Definitions>();
        pidToIdMap = new HashMap<String, String>();
        if (allowPublicAccessStr.equals("true")) {
            allowPublicAccess = true;
        }
        brokenConfigurationIds = new HashSet<String>();
        
        //try {
            //defaultConfigurationDoc = fedora.parseUrlAsXmlDocument(defaultConfigUrl);
            //new DefaultCollectionConfigurationData(defaultConfigurationDoc); // just loading it to make sure we can, later
        //} catch (Throwable t) {
        //    logger.error("Error loading the default collection document! (" + defaultConfigUrl + ")", t);
        //    throw new RuntimeException(t);
        //} 
    }
    
    /**
     * Gets the CollectionConfiguration object that has the given id
     * value or returns null.  This method 
     */
    public CollectionConfiguration getCollectionConfiguration(String id) throws ConfigurationManagerException {
        long start = System.currentTimeMillis();
        try {
            // get current config data
            CollectionConfigurationData cachedConfigData = idToConfigDataMap.get(id);
            if (cachedConfigData == null) {
                String configPid = lookupObjectPid(id);
                if (configPid == null) {
                    return null;
                }
                String objectModDate = fedora.getObjectProfile(configPid).getLastModDate();
                if (cachedConfigData == null || (pidToFreshnessMap.containsKey(configPid) && pidToFreshnessMap.get(configPid).compareTo(objectModDate) <= 0)) {
                    // reload from source because it was not cached or the cache is
                    // out of date, making sure to query and cache relationship information 
                    // as well.
                    synchronized (this) {
                        cachedConfigData = new DefaultCollectionConfigurationData(fedora.getXMLDatastreamAsDocument(configPid, collectionConfigDSID));
                        brokenConfigurationIds.remove(id);
                        pidToFreshnessMap.put(configPid, objectModDate);
                        idToConfigDataMap.put(cachedConfigData.getId(), cachedConfigData);
 
                        idToParentIdMap.remove(cachedConfigData.getId());
                        List<String> parentPids = fedora.getPidsRelatedToThisPidWithRelationship(configPid, childRelationshipURI);
                        for (String parentPid : parentPids) {
                            if (fedora.listContentModelURIs(parentPid).contains("info:fedora/" + collectionContentModel)) {
                                idToParentIdMap.put(cachedConfigData.getId(), fedora.getIdForPid(parentPid));
                                break;
                            }
                        }
                    }
                }
            }
            
            // get the current backing definitions
            List<Definitions> defs = new ArrayList<Definitions>();
            for (String defId : cachedConfigData.listRepresentedDefinitionsIds()) {
                Definitions cachedDef = idToDefinitionsMap.get(defId);
                if (cachedDef == null) {
                    String defPid = lookupObjectPid(defId);
                    if (defPid == null) {
                        return null;
                    }
                    String defModDate = fedora.getDatastreamProperty(defPid, definitionDSID, DatastreamProfile.DatastreamProperty.DS_CREATE_DATE);
                    if (cachedDef == null || (pidToFreshnessMap.containsKey(defPid) && pidToFreshnessMap.get(defPid).compareTo(defModDate) <= 0)) {
                        synchronized (this) {
                            cachedDef = new DefaultFieldDefinitions(fedora.getXMLDatastreamAsDocument(defPid, definitionDSID));
                            pidToFreshnessMap.put(defPid, defModDate);
                            idToDefinitionsMap.put(cachedDef.getId(), cachedDef);
                        }
                    }
                }
                defs.add(cachedDef);
            }
            return new DefaultCollectionConfiguration(cachedConfigData, defs);
        } catch (Throwable t) {
            throw new ConfigurationManagerException(t);
        } finally {
            //logger.debug((System.currentTimeMillis() - start) + "ms to load \"" + id + "\".");
        }
    }
    
    private synchronized String lookupObjectPid(String id) throws FedoraException, IOException {
        for (Map.Entry<String, String> mapEntry : pidToIdMap.entrySet()) {
            if (mapEntry.getValue().equals(id)) {
                return mapEntry.getKey();
            }
        }
        List<String> pids = fedora.dcIdentifierLookup(id);
        if (pids.size() == 1) {
            pidToIdMap.put(pids.get(0), id);
            return pids.get(0);
        } else {
            return null;
        }
    }
    
    public synchronized Collection<CollectionConfiguration> getCollectionConfigurations(String type, boolean onlypublic) throws ConfigurationManagerException {
        List<CollectionConfiguration> configs = new ArrayList<CollectionConfiguration>();
        try {
            if (cachedConfigIds != null) {
                // use the cache to avoid doing a resource index lookup
                for (String configId : cachedConfigIds) {
                    try {
                        configs.add(getCollectionConfiguration(configId));
                    } catch (Throwable t) {
                        logger.error("The configuration for \"" + configId + "\" could not be loaded!", t);
                        brokenConfigurationIds.add(configId);
                    }
                }
            } else {
                cachedConfigIds = new ArrayList<String>();
                for (String configPid : fedora.getRelatedPids(collectionContentModel, "fedora-model:hasModel")) {
                    String id = fedora.getIdForPid(configPid);
                    cachedConfigIds.add(id);
                    try {
                        configs.add(getCollectionConfiguration(id));
                    } catch (Throwable t) {
                        brokenConfigurationIds.add(id);
                    }
                }
            }
        } catch (FedoraException ex) {
            throw new ConfigurationManagerException(ex);
        } catch (IOException ex) {
            throw new ConfigurationManagerException(ex);
        }
        filterConfigs(configs, type, onlypublic);
        return configs;
    }
    
    /**
     * A destructive method that removes collection configurations
     * from the list if they don't have the given type.
     */
    private void filterConfigs(List<CollectionConfiguration> configs, String type, boolean onlypublic) {
        Iterator<CollectionConfiguration> it = configs.iterator();
        while (it.hasNext()) {
            CollectionConfiguration config = it.next();
            if (!type.equals(config.getCollectionMetadata().getType()) || (onlypublic && !config.isPublic())) {
                it.remove();
            }
        }
    }

    public synchronized void storeConfiguration(CollectionConfigurationData config) throws ConfigurationManagerException {
        try {
            // validate the configuration, since this will throw an exception if 
            // there's a problem
            new DefaultCollectionConfiguration(config, listKnownDefinitionSets());
            
            // do additional validation for security purposes 
            Properties p = config.getCollectionMetadata().getImageSubmissionProperties();
            if (p != null && p.getProperty("dropboxDir") != null && dropboxBaseDir == null) {
                throw new ConfigurationManagerException("This FedoraConfigurationmanager has not been configured to support file submission!");
            }
            if (p != null && p.getProperty("dropboxDir") != null && !new File(p.getProperty("dropboxDir")).getAbsolutePath().startsWith(dropboxBaseDir)) {
                // This is a security concern, we want to ensure that the user can only specify paths
                // within the configuration directory
                throw new ConfigurationManagerException("The dropbox directory may not exist outside of the configured space!");
            }
            
            String collectionObjectPid = lookupObjectPid(config.getId());
            if (collectionObjectPid == null) {
                // create a new collection object

                // 1. create the object
                collectionObjectPid = fedora.createObject(null, null, null, null, null);
             
                // 2. set the dc identifier
                //    This is something required by the DLP's repository but likely not other repositories
                DCRecord dc = new DCRecord(fedora.getXMLDatastreamAsDocument(collectionObjectPid, "DC"));
                dc = dc.addIdentifier(config.getId());
                File tempFile = File.createTempFile("dublin-core-", ".xml");
                dc.writeOut(tempFile);
                fedora.addOrReplaceDatastreamWithFile(collectionObjectPid, "DC", ControlGroup.X, null, "text/xml", true, tempFile, null);
                tempFile.delete();
                
                // 2. add the configuration datastream
                fedora.addOrReplaceDatastreamWithDocument(collectionObjectPid, collectionConfigDSID, FedoraRestApiWrapper.ControlGroup.M, "Collection Configuration", "text/xml", config.generateDocument(), "cataloging-configuration.xml");

                // 3. add the content model(s)
                fedora.addRelationship(collectionObjectPid, fedora.toUriString(collectionObjectPid), fedora.HAS_MODEL, fedora.toUriString(collectionContentModel));
                //the following is required by the DLP's repository but likely not other repositories
                fedora.addRelationship(collectionObjectPid, fedora.toUriString(collectionObjectPid), fedora.HAS_MODEL, fedora.toUriString("cmodel:collection"));                
            } else {
                // update existing collection object
                fedora.addOrReplaceDatastreamWithDocument(collectionObjectPid, collectionConfigDSID, FedoraRestApiWrapper.ControlGroup.M, "Collection Configuration", "text/xml", config.generateDocument(), "cataloging-configuration.xml");
            }
            
            // update the cache
            idToConfigDataMap.remove(config.getId());
            getCollectionConfiguration(config.getId());
            
        } catch (Throwable t) {
            throw new ConfigurationManagerException(t);
        }
    }

    public List<Definitions> listKnownDefinitionSets() {
        return new ArrayList<Definitions>(idToDefinitionsMap.values());
    }

    public synchronized void clearCache() {
        cachedConfigIds = null;
        pidToFreshnessMap.clear();
        idToConfigDataMap.clear();
        idToParentIdMap.clear();
        idToDefinitionsMap.clear();
        pidToIdMap.clear();
        brokenConfigurationIds.clear();
    }

    public Collection<CollectionConfiguration> getChildren(String id, String type, boolean onlypublic) throws ConfigurationManagerException {
        // ensure that all the information is cached
        if (cachedConfigIds == null) {
            getCollectionConfigurations(null, false);
        }
        List<CollectionConfiguration> children = new ArrayList<CollectionConfiguration>();
        for (Map.Entry<String, String> entry : idToParentIdMap.entrySet()) {
            if (entry.getValue().equals(id)) {
                CollectionConfiguration child = getCollectionConfiguration(entry.getKey());
                if (type.equals(child.getCollectionMetadata().getType()) && (!onlypublic || child.isPublic())) {
                    children.add(child);
                }
            }
        }
        return children;
    }

    public CollectionConfiguration getParent(String id, boolean onlypublic) throws ConfigurationManagerException {
        // ensure that the appropriate information is cached
        getCollectionConfiguration(id);
        
        String parentId = idToParentIdMap.get(id);
        if (parentId == null) {
            return null;
        } else {
            CollectionConfiguration parent = getCollectionConfiguration(parentId);
            if (onlypublic && !parent.isPublic()) {
                return null;
            } else {
                return parent;
            }
        }
    }
    
    public void setParent(String itemId, String parentId) throws ConfigurationManagerException {
        throw new UnsupportedOperationException();
        // TODO:
        // 1. remove any existing parent (and clear the item from the cache)
        // 2. add the new parent (if it exists)
    }
    

    /**
     * Returns collections of the given type which have no parent or
     * whose parent is not public if onlypublic is set.
     */
    public Collection<CollectionConfiguration> getOrphans(String type, boolean onlypublic) throws ConfigurationManagerException {
        List<CollectionConfiguration> orphans = new ArrayList<CollectionConfiguration>();
        for (CollectionConfiguration c : getCollectionConfigurations(type, onlypublic)) {
            CollectionConfiguration parent = getParent(c.getId(), true);
            if (parent == null) {
                orphans.add(c);
            }
        }
        return orphans;
    }

    /**
     * The current implementation returns a complex set of search constraints
     * that essentially includes only items from public collections that match
     * the publication rules for that collection.
     */
    public List<SearchConstraint> getPublicRecordsSearchConstraints() throws ConfigurationManagerException {
        List<SerializableSearchConstraint> cc = new ArrayList<SerializableSearchConstraint>();
        for (CollectionConfiguration c : getCollectionConfigurations(COLLECTION, true)) {
            List<SerializableSearchConstraint> andC = new ArrayList<SerializableSearchConstraint>(2);
            andC.add(new CollectionSearchConstraint(c, true));
            andC.add(c.getCollectionMetadata().getConditionsForPublication());
            cc.add(new AndSearchConstraintGroup(andC));
        }
        return Collections.singletonList((SearchConstraint) new OrSearchConstraintGroup(cc));
    }

    public boolean isItemPublic(Item item) throws ConfigurationManagerException {
        return SerializableSearchConstraint.doesRecordMatch((SerializableSearchConstraint) getPublicRecordsSearchConstraints().get(0), item.getMetadata());
    }

    /**
     * This implementation is hard-coded to include a collection, date and subject
     * facet.
     */
    public List<BrowseSet> getGlobalFacets() {
        List<BrowseSet> facets = null;
        facets = new ArrayList<BrowseSet>();
        facets.add(0, new CollectionBrowseSet("Collection"));
        facets.add(1, new DateBrowseSet("Date", "DATE_TAKEN", "year","yyyy"));
        facets.add(2, new FieldPartBrowseSet("Topic", "TOPICAL_SUBJECT", "subject"));
        return facets;
    }

    public boolean allowPublicAccess() {
        return allowPublicAccess;
    }

    public Collection<String> getInvalidCollectionIds() {
        return brokenConfigurationIds;
    }

    public CollectionConfiguration createNewCollection(String id, String type, CollectionConfiguration parent) throws ConfigurationManagerException {
        try {
            synchronized (fedora) { 
                // load the template (applying new values)
                CollectionConfigurationData ccd = null;
                if (parent == null) {
                    // use the default config as a template
                    ccd = new DefaultCollectionConfigurationData(id, type, defaultConfigurationDoc);
                } else {
                    // use the unit config as a template
                    ccd = new DefaultCollectionConfigurationData(id, type, parent.generateDocument());
                }

                // if the configuration doesn't include anything for record id generation, set up
                // a collection-id-specific setup using a new id file.
                if (ccd.getCollectionMetadata().getRecordCreationProperties() == null || ccd.getCollectionMetadata().getRecordCreationProperties().isEmpty()) {
                    // there was a problem, so set up a default if possible
                    if (dropboxBaseDir != null && ingestBaseDir != null) {
                        String subDir = ccd.getId().indexOf('/') != -1 ? ccd.getId().substring(ccd.getId().lastIndexOf('/') + 1) : ccd.getId();

                        // create the dropbox dir and put id files and a readme in there
                        File dropboxDir = new File(new File(dropboxBaseDir), subDir);
                        dropboxDir.mkdirs();
                        
                        File itemIdGenFile = new File(dropboxDir, "item-id-gen.txt");
                        File imageIdGenFile = new File(dropboxDir, "image-id-gen.txt");
                        
                        PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(itemIdGenFile)));
                        w.println("1");
                        w.flush();
                        w.close();
                        if (!itemIdGenFile.exists()) {
                            throw new RuntimeException(itemIdGenFile.getAbsolutePath() + " was not created!");
                        }
                        
                        w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(imageIdGenFile)));
                        w.println("1");
                        w.flush();
                        w.close();
                        if (!imageIdGenFile.exists()) {
                            throw new RuntimeException(imageIdGenFile.getAbsolutePath() + " was not created!");
                        }

                        
                        w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(dropboxDir, "dropbox-readme.txt"))));
                        w.println("This directory is managed by the photocat application and the image proc application.");
                        w.println();
                        w.println("In photocat, this directory represents the dropbox for submitted images.  Image files");
                        w.println("will be placed in this directory with filenames that correspond to the records to which");
                        w.println("those items belong.  ImageProc3 should eventually process those files.");
                        w.println();
                        w.println("Special Files: (MUST NOT BE DELETED OR ALTERED!)");
                        w.println("  item-id-gen.txt   - contains the numeric part of the next id that will be used for newly created records.");
                        w.println("  image-id-gen.txt  - contains the numeric part of the next id that will be used for newly created images.");
                        w.flush();
                        w.close();
                        
                        Properties recordCreationProperties = new Properties();
                        recordCreationProperties.setProperty("idNumberFile", itemIdGenFile.getAbsolutePath());
                        recordCreationProperties.setProperty("idPrefix", "http://purl.dlib.indiana.edu/iudl/" + ccd.getId() + "/" + subDir + "-");
                        recordCreationProperties.setProperty("idDigits", "00000");
                        ccd.getCollectionMetadata().setRecordCreationProperties(recordCreationProperties);
                        
                        
                        Properties imageSubmissionProperties = null;
                        if (ccd.getCollectionMetadata().getImageSubmissionProperties() == null || ccd.getCollectionMetadata().getImageSubmissionProperties().isEmpty()) {
                            imageSubmissionProperties = new Properties();
                            imageSubmissionProperties.setProperty("dropboxDir", dropboxDir.getAbsolutePath());
                            imageSubmissionProperties.setProperty("ingestDir", new File(ingestBaseDir, subDir).getAbsolutePath());
                            imageSubmissionProperties.setProperty("idNumberFile", imageIdGenFile.getAbsolutePath());
                            imageSubmissionProperties.setProperty("idPrefix", "http://purl.dlib.indiana.edu/iudl/" + ccd.getId() + "/" + subDir + "-U-");
                            imageSubmissionProperties.setProperty("idDigits", "00000");
                            ccd.getCollectionMetadata().setImageSubmissionProperties(imageSubmissionProperties);
                        }
                    }
                    
                }
                // store it to fedora
                fedora.createNewCollectionFedoraObject(ccd, (parent != null ? parent.getId() : null), "info:fedora/" + collectionContentModel, collectionConfigDSID, childRelationshipURI);
                
                // fetch it from fedora
                return getCollectionConfiguration(id);
            }
        } catch (Throwable t) {
            logger.error("Error creating new collection! (" + id + ", " + type + ", " + (parent != null ? parent.getId() : "null") + ")", t);
            throw new ConfigurationManagerException(t);
        }
    }

    public String storeCollectionFile(CollectionConfiguration c, InputStream fileIs, String mimeType, String id, boolean overwriteIfPresent) throws ConfigurationManagerException {
        try {
            String pid = fedora.getPidForPURL(c.getId());
            if (!overwriteIfPresent && fedora.listDatastreams(pid).contains(id)) {
                throw new ConfigurationManagerException("Unable to overwrite existing datastream \"" + id + "\" on " + pid + "!");
            }
            File file = File.createTempFile("uploaded-file", ".extension");
            writeStreamToFile(fileIs, file);
            fedora.addOrReplaceDatastreamWithFile(pid, id, FedoraRestApiWrapper.ControlGroup.M, "uploaded file", mimeType, true, file, "Uploaded a file through photocat.");
            file.delete();
            return fedora.getDSAccessUrl(pid, id);
        } catch (FedoraException ex) {
            throw new ConfigurationManagerException(ex);
        } catch (IOException ex) {
            throw new ConfigurationManagerException(ex);
        }
        

    }
    
    /**
     * A helper method to use NIO functions to efficiently write
     * an InputStream to a file.
     */
    private void writeStreamToFile(InputStream is, File file) throws IOException {
        OutputStream output = new FileOutputStream(file);
        ReadableByteChannel inputChannel = Channels.newChannel(is);  
        WritableByteChannel outputChannel = Channels.newChannel(output);  
        ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);  
        while (inputChannel.read(buffer) != -1) {  
            buffer.flip();  
            outputChannel.write(buffer);  
            buffer.compact();  
        }  
        buffer.flip();  
        while (buffer.hasRemaining()) {  
            outputChannel.write(buffer);  
        }  
       inputChannel.close();  
       outputChannel.close();
    }
    
}
