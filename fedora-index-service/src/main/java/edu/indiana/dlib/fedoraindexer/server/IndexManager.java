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
package edu.indiana.dlib.fedoraindexer.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.URI;
import org.apache.log4j.Logger;

import edu.indiana.dlib.fedora.client.ObjectProfile;
import edu.indiana.dlib.fedora.client.iudl.DLPFedoraClient;
import edu.indiana.dlib.fedora.client.iudl.PURLLogic;
import edu.indiana.dlib.fedoraindexer.server.Index.Operation;

/**
 * Manages multiple {@code Index} implementations.  This includes
 * instantiation and propagation of requests for updating.  Requests
 * for optimization is also managed by this class.
 */
public class IndexManager {
    
    private static final String PURL_BASE = "http://purl.dlib.indiana.edu/iudl";
    
    public static Logger LOGGER = Logger.getLogger(IndexManager.class);
    
    private List<Index> indexList;

    private DLPFedoraClient fc;
    
    public IndexManager(Properties configuration, File workingDirectory) {
        // set up the Index objects
        int indexCount = 0;
        try {
            indexCount = Integer.parseInt(configuration.getProperty("indexCount"));
        } catch (NullPointerException ex) {
            LOGGER.error("Required property, \"indexCount\" was not specified!");
        } catch (NumberFormatException ex) {
            LOGGER.error("Required property, \"indexCount\" was not parsible as an integer! (" + configuration.getProperty("indexCount") + ")");
        }
        
        this.setUpFedoraAccess(configuration);
        
        this.indexList = new ArrayList<Index>(indexCount);
        for (int i = 0; i < indexCount; i ++) {
            String className = configuration.getProperty("index." + i + ".classname");
            if (className == null) {
            	LOGGER.error("Unable to find property : index." + i + ".classname!");
            } else {
	            boolean requiresFedoraAccess = Boolean.parseBoolean(configuration.getProperty("index." + i + ".requiresFedoraAccess"));
	            String configPropertiesFile = configuration.getProperty("index." + i + ".configPropertiesFile");
	            Properties config = null;
	            if (configPropertiesFile != null) {
	                config = new Properties();
	                FileInputStream fis = null;
	                try {
	                    if (new File(workingDirectory, configPropertiesFile).exists()) {
	                        fis = new FileInputStream(new File(workingDirectory, configPropertiesFile));
	                    } else {
	                        fis = new FileInputStream(configPropertiesFile);
	                    }
	                    config.load(fis);
	                } catch (FileNotFoundException ex) {
	                    config = null;
	                    LOGGER.error("Unable to find configuration file \"" + new File(configPropertiesFile).getAbsolutePath() + "\"!", ex);
	                } catch (IOException ex) {
	                    config = null;
	                    LOGGER.error("Unable to load configuration file \"" + new File(configPropertiesFile).getAbsolutePath() + "\"!", ex);
	                } finally {
	                    if (fis != null) {
	                        try {
	                            fis.close();
	                        } catch (IOException ex) {
	                            LOGGER.error("Unable to close \"" + configPropertiesFile + "\".", ex);
	                        }
	                    }
	                }
	            }
	            try {
	                Class indexClass = Class.forName(className);
	                if (config != null && requiresFedoraAccess) {
	                    this.indexList.add((Index) indexClass.getConstructor(Properties.class, DLPFedoraClient.class).newInstance(config, fc));
	                } else if (config != null) {
	                    this.indexList.add((Index) indexClass.getConstructor(Properties.class).newInstance(config));
	                } else {
	                    this.indexList.add((Index) indexClass.newInstance());
	                }
	            } catch (ClassNotFoundException ex) {
	                LOGGER.error("Unknown index class: " + className, ex);
	            } catch (SecurityException ex) {
	                LOGGER.error("SecurityException for index class: " + className, ex);
	            } catch (NoSuchMethodException ex) {
	                LOGGER.error("No valid constructor for index class: " + className, ex);
	            } catch (IllegalArgumentException ex) {
	                LOGGER.error("Illegal arguments for index class: " + className, ex);
	            } catch (InstantiationException ex) {
	                LOGGER.error("Error instantiating index class: " + className, ex);
	            } catch (IllegalAccessException ex) {
	                LOGGER.error("Illegal access on index class: " + className, ex);
	            } catch (InvocationTargetException ex) {
	                LOGGER.error("Exception while initializing index class: " + className, ex.getCause());
	            } catch (Throwable t) {
	            	LOGGER.error("Exception while initializing index class: " + className, t);
	            }
            }
        }
    }
    
    private void setUpFedoraAccess(Properties configuration) {
        // set up APIA and APIM
        String fedoraHost = configuration.getProperty("fedoraHost");
        LOGGER.debug("Fedora Host: " + fedoraHost);
        String fedoraContextName = configuration.getProperty("fedoraContextName");
        LOGGER.debug("Fedora Context Name: " + fedoraContextName);
        String fedoraPortStr = configuration.getProperty("fedoraPort");
        int fedoraPort = 8080;
        try {
            fedoraPort = Integer.parseInt(fedoraPortStr);
            LOGGER.debug("Fedora Port: " + fedoraPort);
        } catch (NumberFormatException ex) {
            LOGGER.warn("Unable to parse fedoraPort, \"" + fedoraPortStr + "\" defaulting to " + fedoraPort + ".");
        }
        String fedoraUsername = configuration.getProperty("fedoraUsername");
        LOGGER.debug("Fedora Username: " + fedoraUsername);
        String fedoraPassword = configuration.getProperty("fedoraPassword");
        LOGGER.debug("Fedora Password: " + fedoraPassword.replaceAll(".", "*"));

        this.fc = null;
        try {
            this.fc = new DLPFedoraClient(fedoraUsername, fedoraPassword, fedoraHost, fedoraContextName, fedoraPort, true);
        } catch (Exception ex) {
            LOGGER.error("Unable to connect to Fedora!", ex);
        }
    }
    
    public void indexObject(Operation op, String pid, String date) {
        LOGGER.debug("indexObject [start]");
        try {
            FedoraObjectAdministrativeMetadata adminData = null;
            try {
                adminData = this.getAdminData(op, pid, date);
                if (adminData == null) {
                    LOGGER.info("Skipping " + pid + " because no PURL was specified in the DC datastream.");
                    return;
                }
            } catch (Throwable t) {
                if (op.equals(Operation.REMOVE)) {
                    LOGGER.warn("Unable to fetch administrative data for " + pid + ", but made a best guess for remove operation!");
                    adminData = new FedoraObjectAdministrativeMetadata(pid, null, null, null, null, null, null);
                } else {
                    LOGGER.error("Unable to fetch administrative data for " + pid + "!", t);
                    return;
                }
            }
            LOGGER.debug("  indexObject fetched AdminData");
            synchronized (this) {
                LOGGER.debug("  indexObject got monitor");
                for (Index index : this.indexList) {
                    LOGGER.debug("  indexObject considering index \"" + index.getIndexName() + "\" [start]");
                    if (op.equals(Operation.REMOVE) || index.shouldIndexObject(adminData)) {
                        try {
                            index.indexObject(op, adminData);
                        } catch (Throwable t) {
                            LOGGER.error("Unable to perform operation " + op + " on object, \"" + adminData.getPid() + " - " + adminData.getFullItemId() + "\", for index \"" + index.getIndexName() + "\"!", t);
                        }
                    } else {
                        try {
                            index.indexObject(Operation.REMOVE, adminData);
                            LOGGER.debug(index.getIndexName() + ": Removed " + adminData.getPid() + " - " + adminData.getFullItemId());
                        } catch (Throwable t) {
                            LOGGER.error("Unable to remove an ineligible object, \"" + adminData.getPid() + " - " + adminData.getFullItemId() + "\", for index \"" + index.getIndexName() + "\"!", t);
                        }
                    }
                    LOGGER.debug("  indexObject considering index \"" + index.getIndexName() + "\" [done]");
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Exception in indexObject", t);
        } finally {
            LOGGER.debug("indexObject [end]");
        }
    }
    
    public synchronized void optimize() {
        for (Index index : this.indexList) {
            try {
                index.optimize();
            } catch (IndexOperationException ex) {
                LOGGER.error("Unable to optimize index: " + index.getIndexName(), ex);
            } catch (Throwable t) {
                LOGGER.error("Unable to optimize index: " + index.getIndexName(), t);

            }
        }
    }
    
    /**
     * Builds an administrative metadata object by querying fedora or 
     * providing dummy (null) values in the case of a "remove" operation.
     * @throws RemoteException 
     * @throws StreamIOException 
     * @throws RepositoryConfigurationException 
     * @throws ObjectIntegrityException 
     */
    private FedoraObjectAdministrativeMetadata getAdminData(Operation op, String pid, String date) throws Exception {
        MultiThreadedHttpConnectionManager cm = fc.getConnectionManager();
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(new URI(fc.getServerUrl(), false));
        int currentConnections = cm.getConnectionsInPool(hostConfig); 
        int maxConnections = cm.getParams().getMaxConnectionsPerHost(hostConfig);
        if (currentConnections == maxConnections) {
            LOGGER.warn("All " + currentConnections + " connections to the fedora host are in use!");
        } else {
            LOGGER.debug(currentConnections + " of " + maxConnections + " connections to fedora are in use.");
        }
        ObjectProfile profile = fc.getObjectProfile(pid, date);
        String PURL = fc.getPURL(pid);
        if (PURL == null) {
            LOGGER.error("Unable to identify PURL for " + pid + "!");
            return null;
        }

        String fullItemId = PURLLogic.getFullItemIdFromDefaultPURL(PURL); 
        String title = profile.getLabel();
        String collectionId = PURLLogic.getCollectionIdFromDefaultPURL(PURL);
        List<String> contentModels = fc.getContentModelURIs(pid);
        String creationDate = profile.getCreateDate();
        String lastModificationDate = date;
        if (date != null && !date.equals(lastModificationDate)) {
            LOGGER.warn("Modification date mismatch: " + date + " != " + lastModificationDate);
        }
        return new FedoraObjectAdministrativeMetadata(pid, fullItemId, title, collectionId, contentModels, creationDate, lastModificationDate);
    }
}
