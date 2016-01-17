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
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
import edu.indiana.dlib.catalog.config.Aspect;
import edu.indiana.dlib.catalog.config.BlockViewsConfig;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.DataFormatException;
import edu.indiana.dlib.catalog.config.DataView;
import edu.indiana.dlib.catalog.config.FileSubmissionStatus;
import edu.indiana.dlib.catalog.config.HistoryEnabledItemManager;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.config.ItemMetadata;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.config.OptimisticLockingException;
import edu.indiana.dlib.catalog.config.RepositoryException;
import edu.indiana.dlib.catalog.config.impl.fedora.DatastreamProfile;
import edu.indiana.dlib.catalog.config.impl.fedora.FedoraException;
import edu.indiana.dlib.catalog.config.impl.fedora.FedoraRestApiWrapper;
import edu.indiana.dlib.catalog.search.structured.constraints.SerializableSearchConstraint;

/**
 * A Fedora-based ItemManager implementation that contains
 * very specific code (including hard-coded URLs) appropriate
 * only for the DLP's fedora setup.
 */
public class FedoraItemManager implements ItemManager, HistoryEnabledItemManager {

    /**
     * A client that is used to access the Fedora
     * repository.  This instance must not be read-only.
     * Furthermore, all accesses using this client must 
     * synchronize on it as it is not thread-safe. 
     */
    private FedoraRestApiWrapper fedora;
    
    private String itemContentModelPid;
    
    private String metadataDatastreamId;
    
    private String privateMetadataDsId;
    
    private String blockAllItemsFedoraPolicyURL;
    
    private String blockHighResItemsFedoraPolicyURL;
    
    private String imageToRecordRelationship;
    
    /**
     * This is a rudimentary map of image content models to the image
     * datastreams they contain.  It is used to find views of objects
     * and to delete those views.  An obvious improvement would be to
     * make this configurable and actually harvest the information from
     * fedora's cmodel objects.  For now it's hard-coded for our
     * content models.
     */
    private Map<String, String[]> imageContentModelUriToDSIDMap;
    
    public FedoraItemManager(String username, String password, String host, String contextName, Integer port, String cmodelPid, String dsId, String privateDsId) {
        this(username, password, host, contextName, port, cmodelPid, dsId, privateDsId, false);
    }
    
    public FedoraItemManager(String username, String password, String host, String contextName, Integer port, String cmodelPid, String dsId, String privateDsId, String blockAll, String blockHigh) {
        this(username, password, host, contextName, port, cmodelPid, dsId, privateDsId, blockAll, blockHigh, false);
    }

    public FedoraItemManager(String username, String password, String host, String contextName, Integer port, String cmodelPid, String dsId, String privateDsId, Boolean readOnly) {
        this(username, password, host, contextName, port, cmodelPid, dsId, privateDsId, null, null, readOnly);
    }
    
    public FedoraItemManager(String username, String password, String host, String contextName, Integer port, String cmodelPid, String dsId, String privateDsId, String blockAll, String blockHigh, Boolean readOnly) {
        fedora = new FedoraRestApiWrapper(username, password, host, contextName, port, readOnly);
        metadataDatastreamId = dsId;
        itemContentModelPid = cmodelPid;
        privateMetadataDsId = privateDsId;
        blockAllItemsFedoraPolicyURL = blockAll;
        blockHighResItemsFedoraPolicyURL = blockHigh;
        imageToRecordRelationship = "info:fedora/fedora-system:def/relations-external#hasMetadata";
        
        imageContentModelUriToDSIDMap = new HashMap<String, String[]>();
        imageContentModelUriToDSIDMap.put("info:fedora/cmodel:image-derivatives", new String[] { "LARGE", "SCREEN", "THUMBNAIL" });
        imageContentModelUriToDSIDMap.put("info:fedora/cmodel:image-master", new String[] {"MASTER"} );
        imageContentModelUriToDSIDMap.put("info:fedora/djatoka:jp2CModel", new String[] { "SCALABLE" });
    }
    
    public Item fetchItemIncludingPrivateMetadata(String id, CollectionConfiguration c) throws RepositoryException {
        return fetchItem(id, c, true);
    }  
    
    public Item fetchItem(String id, CollectionConfiguration c) throws RepositoryException {
        return fetchItem(id, c, false);
    }
    
    /**
     * The current implementation gets a quick resoponse by making DLP-specific
     * assumptions about item identifiers.
     */
    public String getItemCollectionId(String itemId) {
       String fullItemId = itemId.replace("http://purl.dlib.indiana.edu/iudl/", "");
       return fullItemId.substring(0, fullItemId.lastIndexOf('/'));
    }
    
    /**
     * Gets the item with the given id.  This method returns null if no item
     * is found with that id, the parsed Item object if one is found and thows
     * an exception if more than one item are found with the supposedly unique
     * id.
     * 
     * This method filters the views according to the CollectionConfiguration
     * unless "includePrivateMetadata" is true.
     */
    private Item fetchItem(String id, CollectionConfiguration c, boolean includePrivateMetadata) throws RepositoryException {
        try {
            List<NameValuePair> controlFields = new ArrayList<NameValuePair>(3);
            
            // determine the pid
            String pid = null;
            List<String> pids = fedora.dcIdentifierLookup(id);
            if (pids.size() == 0) {
                return null;
            } else if (pids.size() > 1) {
                throw new RuntimeException(pids.size() + " items found with the \"unique\" identifier, \"" + id + "\"!");
            } else {
                pid = pids.get(0);
                controlFields.add(new NameValuePair("pid", pid));
            }
            
            
            ItemMetadata im = null;
            
            // parse the public metadata
            im = new DefaultItemMetadata(fedora.getXMLDatastreamAsDocument(pid, metadataDatastreamId));
            if (!im.getId().equals(id)) {
                throw new RuntimeException("The item identified by \"" + id + "\" improperly claims to be \"" + im.getId() + "\"! (pid=" + pid + ")");
            }
            controlFields.add(new NameValuePair("publicLastModified", fedora.getDatastreamProperty(pid, metadataDatastreamId, DatastreamProfile.DatastreamProperty.DS_CREATE_DATE)));

            // parse the private metadata
            if (includePrivateMetadata) {
                controlFields.add(new NameValuePair("private", "true"));
                if (fedora.hasDatastream(pid, privateMetadataDsId)) {
                    ItemMetadata privateIm = new DefaultItemMetadata(fedora.getXMLDatastreamAsDocument(pid, privateMetadataDsId));
                    if (!privateIm.getId().equals(id)) {
                        throw new RuntimeException("The item identified by \"" + id + "\" improperly claims to be \"" + privateIm.getId() + "\"! (pid=" + pid + ")");
                    }
                    
                    // merge the content into the main ItemMetadata
                    for (String type : privateIm.getRepresentedFieldTypes()) {
                        if (im.getFieldData(type) != null) { 
                            throw new RuntimeException("The item identified by \"" + id + "\" has improperly distributed public/private fields!");
                        } else {
                            im.setFieldValue(type, privateIm.getFieldData(type));
                        }
                    }
                    controlFields.add(new NameValuePair("privateLastModified", fedora.getDatastreamProperty(pid, privateMetadataDsId, DatastreamProfile.DatastreamProperty.DS_CREATE_DATE)));
                }
            }
            
            boolean blockAll = false;
            boolean blockHigh = false;
            if (!includePrivateMetadata) {
                BlockViewsConfig bvc = c.getCollectionMetadata().getBlockViewsConfig();
                if (bvc != null) {
                    if (bvc.blocksAll()) {
                        if (bvc.getConstraint() == null || SerializableSearchConstraint.doesRecordMatch(bvc.getConstraint(), im)) {
                            blockAll = true;
                        }
                    } else if (bvc.blocksHighRes()) {
                        if (bvc.getConstraint() == null || SerializableSearchConstraint.doesRecordMatch(bvc.getConstraint(), im)) {
                            blockHigh = true;
                        }
                    }
                }
            }
            
            // determine the data views
            // TODO: a more robust implementation would not assume datastream names
            // or content types... this quick and dirty implementation only works
            // for DLP images.
            List<Aspect> aspects = new ArrayList<Aspect>();
            List<String> pidsThatCouldBeViews = new ArrayList<String>();
            pidsThatCouldBeViews.add(pid);
            pidsThatCouldBeViews.addAll(fedora.getRelatedPids(pid, imageToRecordRelationship));
            for (String viewPid : pidsThatCouldBeViews) {
                List<DataView> dataViews = new ArrayList<DataView>();
                DataView preview = null;
                DataView screen = null;
                DataView large = null;
                FedoraDjatokaDataView scalable = null;
                List<String> dsIds = fedora.listDatastreams(viewPid);
                boolean policyBlocksAll = false;
                boolean policyBlocksHigh = false;
                boolean policyBlocksUnknown = false;
                if (dsIds.contains("POLICY")) {
                    String policyUrl = fedora.getDatastreamProperty(viewPid, "POLICY", DatastreamProfile.DatastreamProperty.DS_LOCATION);
                    if (policyUrl.equals(blockAllItemsFedoraPolicyURL)) {
                        policyBlocksAll = true;
                    } else if (policyUrl.equals(blockHighResItemsFedoraPolicyURL)) {
                        policyBlocksHigh = true;
                    } else {
                        Logger.getLogger(getClass()).warn("Unknown policy on object \"" + pid + "\": " + policyUrl);
                        policyBlocksUnknown = true;
                    }
                }
                for (String dsId : dsIds) {
                    if (dsId.equals("THUMBNAIL")) {
                        if (!blockAll) {
                            URL url = new URL((policyBlocksUnknown || policyBlocksAll ? getProxyUrl(fedora, viewPid, dsId) : fedora.getServerUrl() + "/get/" + viewPid + "/" + dsId));
                            preview = new DefaultDataView(url, "image/jpeg", "thumbnail", false);
                            dataViews.add(preview);
                        }
                    } else if (dsId.equals("SCREEN")) {
                        if (!blockAll && !blockHigh) {
                            URL url = new URL((policyBlocksUnknown || policyBlocksAll || policyBlocksHigh ? getProxyUrl(fedora, viewPid, dsId) : fedora.getServerUrl() + "/get/" + viewPid + "/" + dsId));
                            screen = new DefaultDataView(url, "image/jpeg", "screen size image", false);
                            dataViews.add(screen);
                        }
                    } else if (dsId.equals("LARGE")) {
                        if (!blockAll && !blockHigh) {
                            URL url = new URL((policyBlocksUnknown || policyBlocksAll || policyBlocksHigh ? getProxyUrl(fedora, viewPid, dsId) : fedora.getServerUrl() + "/get/" + viewPid + "/" + dsId));
                            large = new DefaultDataView(url, "image/jpeg", "large size image", false);
                            dataViews.add(large);
                        }
                    } else if (dsId.equals("MASTER")) {
                        if (!blockAll && !blockHigh) {
                            // authorization checks for this URL uses CAS, so we need no proxy
                            dataViews.add(new DefaultDataView(new URL(fedora.getServerUrl() + "/get/" + viewPid + "/" + dsId), null, "master image", false));
                        }
                    } else if (dsId.equals("IMAGE")) {
                        if (!blockAll && !blockHigh) {
                            URL url = new URL((policyBlocksUnknown || policyBlocksAll || policyBlocksHigh ? getProxyUrl(fedora, viewPid, dsId) : fedora.getServerUrl() + "/get/" + viewPid + "/" + dsId));
                            preview = new DefaultDataView(url, "image/jpeg", "submitted image", false);
                            dataViews.add(preview);
                        }
                    } else if (dsId.equals("SCALABLE")) {
                        if (!blockAll && !blockHigh) {
                         // authorization checks for this URL uses CAS, so we need no proxy
                            List<String> cmodels = fedora.listContentModelURIs(viewPid);
                            if (cmodels.contains("info:fedora/djatoka:jp2CModel")) {
                                String sdefEscaped = URLEncoder.encode("djatoka:jp2SDef", "UTF-8");
                                scalable = new FedoraDjatokaDataView(
                                        new URL(fedora.getServerUrl() + "/objects/" + URLEncoder.encode(viewPid, "UTF-8") + "/methods/" + sdefEscaped + "/getImageView"),
                                        new URL(fedora.getServerUrl() + "/objects/" + URLEncoder.encode(viewPid, "UTF-8") + "/methods/" + sdefEscaped + "/"),
                                        new URL(fedora.getServerUrl() + "/objects/" + URLEncoder.encode(viewPid, "UTF-8") + "/datastreams/SCALABLE/content"));
                                dataViews.add(scalable);
                            }
                        }
                    }
                }
                // we now return empty aspects, which are useful because you can see the status of 
                // the image processing.  This does mean that perhaps some special handling needs
                // to be put in place when building a user interface by iterating through aspects
                // because they may be empty.
                //if (!dataViews.isEmpty()) {
                    String aspectId = fedora.getIdForPid(viewPid);
                    aspects.add(new DefaultAspect(aspectId, dataViews, preview, screen, large, scalable));
                //}
            }

            return new DefaultItem(im, aspects, controlFields);
        } catch (FedoraException ex) {
            throw new RepositoryException(ex);
        } catch (IOException ex) {
            throw new RepositoryException(ex);
        } catch (XPathExpressionException ex) {
            throw new RepositoryException(ex);
        } catch (DataFormatException ex) {
            throw new RepositoryException(ex);
        } catch (SAXException ex) {
            throw new RepositoryException(ex);
        } catch (ParserConfigurationException ex) {
            throw new RepositoryException(ex);
        }
    }
    
    public String getFedoraServerUrl() {
        return fedora.getServerUrl();
    }
    
    private String getProxyUrl(FedoraRestApiWrapper fedora, String pid, String dsId) {
        return fedora.getServerUrl().replaceAll("/fedora$", "/iudl-dissem/casproxy?pid="  + pid + "&dsname=" + dsId + "&unauth-url=blocked.png");
    }
    
    private static boolean wasPrivateMetadataLoaded(Item item) {
        for (NameValuePair value : item.getControlFields()) {
            if (value.getName().equals("private")) {
                return true;
            }
        }
        return false;
    }
    
    private static String getPid(Item item) {
        for (NameValuePair value : item.getControlFields()) {
            if (value.getName().equals("pid")) {
                return value.getValue();
            }
        }
        return null;
    }
    
    private static String getLastPublicModificationDateStr(Item item) {
        for (NameValuePair value : item.getControlFields()) {
            if (value.getName().equals("publicLastModified")) {
                return value.getValue();
            }
        }
        return null;
    }
    
    private static String getLastPrivateModificationDateStr(Item item) {
        for (NameValuePair value : item.getControlFields()) {
            if (value.getName().equals("privateLastModified")) {
                return value.getValue();
            }
        }
        return null;
    }
    
    public synchronized void saveItemMetadata(Item updatedItem, CollectionConfiguration collection, UserInfo user) throws OptimisticLockingException, RepositoryException {
        // validate control fields
        String pid = getPid(updatedItem);
        if (pid != null) {
            // verify that the item exists and matches the pid
            Item remoteItem = fetchItem(updatedItem.getId(), collection, true);
            if (remoteItem == null) {
                throw new OptimisticLockingException("The item with id, \"" + updatedItem.getId() + "\" no longer exists in the repository!");
            }
            if (!pid.equals(getPid(remoteItem))) {
                throw new RepositoryException("The repository item with id, \"" + updatedItem.getId() + "\" does not have the same PID " + pid + " as when it was originally fetched!");
            }
            
            String publicImLastModified = getLastPublicModificationDateStr(updatedItem);
            if (!getLastPublicModificationDateStr(remoteItem).equals(publicImLastModified)) {
                throw new OptimisticLockingException("The item with id, \"" + updatedItem.getId() + "\" has been modified since you last viewed it.");
            }
            String privateImLastModified = getLastPrivateModificationDateStr(updatedItem);
            String privateImLastModifiedCurrent = getLastPrivateModificationDateStr(remoteItem);
            if (privateImLastModified != null && !privateImLastModified.equals(privateImLastModifiedCurrent)) {
                throw new OptimisticLockingException("The item with id, \"" + updatedItem.getId() + "\" has been modified since you last viewed it. (" + privateImLastModified + " != " + privateImLastModifiedCurrent + ")");
            }
            if (!wasPrivateMetadataLoaded(updatedItem)) {
                throw new OptimisticLockingException("Private metadata must have been loaded to perform a save operation!");
            }
            
            // store the XML
            try {
            	// store the public metadata
                fedora.addOrReplaceDatastreamWithDocument(pid, metadataDatastreamId, FedoraRestApiWrapper.ControlGroup.M, null, "text/xml", collection.getPublicItemMetadata(updatedItem.getMetadata()).generateDocument(), metadataDatastreamId + ".xml");
                
                // store the private metadata
                ItemMetadata privateIm = collection.getPrivateItemMetadata(updatedItem.getMetadata());
                if (!privateIm.getRepresentedFieldTypes().isEmpty() || fedora.hasDatastream(pid, privateMetadataDsId)) {
                    // either there are private fields, or there once were:
                    // to store the new values or to overwrite (yet not kill the history of) 
                    // old values we must write a datastream
                	fedora.addOrReplaceDatastreamWithDocument(pid, privateMetadataDsId, FedoraRestApiWrapper.ControlGroup.M, null, "text/xml", privateIm.generateDocument(), privateMetadataDsId + ".xml");
                } else {
                    // there are no private fields *AND* there have never been any:
                    // we don't need to write this datastream
                }
                
                /* update, remove or add the policy
                 * 
                 * First we determine whether we're blocking all views or high resolution
                 * views for this object.
                 * 
                 * If we're blocking any views and there's an appropriate policy specified
                 * then we'll do an update, otherwise we won't do anything.  
                 */
                boolean blockAll = false;
                boolean blockHigh = false;
                BlockViewsConfig bvc = collection.getCollectionMetadata().getBlockViewsConfig();
                if (bvc != null) {
                    if (bvc.blocksAll()) {
                        if (bvc.getConstraint() == null || SerializableSearchConstraint.doesRecordMatch(bvc.getConstraint(), updatedItem.getMetadata())) {
                            blockAll = true;
                        }
                    } else if (bvc.blocksHighRes()) {
                        if (bvc.getConstraint() == null || SerializableSearchConstraint.doesRecordMatch(bvc.getConstraint(), updatedItem.getMetadata())) {
                            blockHigh = true;
                        }
                    }
                }
                String currentPolicyUrl = fedora.hasDatastream(pid, "POLICY") ? fedora.getDatastreamProperty(pid, "POLICY", DatastreamProfile.DatastreamProperty.DS_LOCATION) : null;
                if (blockAll && blockAllItemsFedoraPolicyURL != null) {
                    // there's a clear policy that should be applied
                    if (currentPolicyUrl == null) {
                        // there's no policy, apply the prescribed one
                        fedora.addRedirectDatastream(pid, "POLICY", blockAllItemsFedoraPolicyURL);
                    } else if (currentPolicyUrl.equals(blockAllItemsFedoraPolicyURL)) {
                        // the policy is already applied, we do nothing
                    } else if (currentPolicyUrl.equals(blockHighResItemsFedoraPolicyURL)) {
                        // the policy is recognized by this system, so we can replace it
                        fedora.addRedirectDatastream(pid, "POLICY", blockAllItemsFedoraPolicyURL);
                    } else {
                        // there's an existing policy that's unrecognized, we need to 
                        // throw an exception or risk screwing up something
                        throw new RepositoryException("The object (" + pid + ") has an unrecognized policy \"" + currentPolicyUrl + "\" applied!");
                    }
                } else if (blockHigh && blockHighResItemsFedoraPolicyURL != null) {
                 // there's a clear policy that should be applied
                    if (currentPolicyUrl == null) {
                        // there's no policy, apply the prescribed one
                        fedora.addRedirectDatastream(pid, "POLICY", blockHighResItemsFedoraPolicyURL);
                    } else if (currentPolicyUrl.equals(blockAllItemsFedoraPolicyURL)) {
                        // the policy is already applied, we do nothing
                    } else if (currentPolicyUrl.equals(blockHighResItemsFedoraPolicyURL)) {
                        // the policy is recognized by this system, so we can replace it
                        fedora.addRedirectDatastream(pid, "POLICY", blockHighResItemsFedoraPolicyURL);
                    } else {
                        // there's an existing policy that's unrecognized, we need to 
                        // throw an exception or risk screwing up something
                        throw new RepositoryException("The object (" + pid + ") has an unrecognized policy \"" + currentPolicyUrl + "\" applied!");
                    }
                }
            } catch (FedoraException ex) {
                throw new RepositoryException(ex);
            } catch (ParserConfigurationException ex) {
                throw new RepositoryException(ex);
            } catch (IOException ex) {
                throw new RepositoryException(ex);
            } catch (XPathExpressionException ex) {
                throw new RepositoryException(ex);
            } catch (SAXException ex) {
                throw new RepositoryException(ex);
            }
            
        } else {
            // validate that no item exists with the given id
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Returns true if record creation is configured for the collection,
     * false if it's not configured and throws an exception if the
     * configuration is invalid. 
     */
    public boolean isRecordCreationEnabled(CollectionConfiguration config) {
        return config.getIdGenerator() != null;
    }
    
    /**
     * Creates a new metadata record in the repository.
     */
    public synchronized String createNewItem(CollectionConfiguration config, UserInfo user, Map<String, String> arguments) throws RepositoryException {
    	// determine if this operation is supported
        try {
	    	// mint an ID
	    	String id = getNextId(config, arguments);
	    	if (id == null) {
	    		throw new RepositoryException("Unable to generate an id for collection " + config.getCollectionMetadata().getId() + "!");
	    	}
	    	if (!fedora.dcIdentifierLookup(id).isEmpty()) {
	    		throw new RepositoryException("An item already exists with generated id \"" + id + "\"!");
	    	}
	    	
	    	// create an item
	    	ItemMetadata im = new ItemMetadata(id, config.getId());
	    	fedora.createNewItemMetadataFedoraObject(im, "info:fedora/" + itemContentModelPid);
	    	
	    	//return the id
	    	return id;
    	} catch (IOException ex) {
    		throw new RepositoryException(ex);
    	} catch (FedoraException ex) {
    		throw new RepositoryException(ex);
		}
    }
    
    public Collection<String> getRequiredArgumentNames(CollectionConfiguration config) {
        return config.getIdGenerator().getRequiredArguments();
    }
	
	/**
	 * This is a specially implemented method that uses information
	 * from the FileSubmissionWorkflowConfiguration to return the
	 * next Id given the provided arguments.  In many cases, no arguments
	 * are required and the next ID is provided by an outside system. 
	 * In other cases, arguments are required and are used to generate
	 * the ID (which is later validated against existing ids).
	 */
    private synchronized String getNextId(CollectionConfiguration c, Map<String, String> args) throws IOException {
        return c.getIdGenerator().getId(args);
    }

    /**
     * Currently unsupported. 
     * @throws UnsupportedOperationException whenever called
     */
    public synchronized boolean removeItem(Item item, CollectionConfiguration config, UserInfo user) throws RepositoryException {
        throw new UnsupportedOperationException();
    }
    
    public synchronized ItemMetadata getHistoricItemMetdata(String id, Date date) throws RepositoryException {
        try {
            // determine the pid
            String pid = null;
            List<String> pids = fedora.dcIdentifierLookup(id);
            if (pids.size() != 1) {
                throw new RuntimeException(pids.size() + " items found with the \"unique\" identifier, \"" + id + "\"!");
            } else {
                pid = pids.get(0);
            }
            
            ItemMetadata im = new DefaultItemMetadata(fedora.getXMLDatastreamAsDocument(pid, metadataDatastreamId, (date == null ? null : FedoraRestApiWrapper.printFedoraDateString(date))));
                
            if (fedora.hasDatastream(pid, privateMetadataDsId)) {
                try {
                    ItemMetadata privateIm = new DefaultItemMetadata(fedora.getXMLDatastreamAsDocument(pid, privateMetadataDsId, (date == null ? null : FedoraRestApiWrapper.printFedoraDateString(date))));
                    for (String type : privateIm.getRepresentedFieldTypes()) {
                        im.setFieldValue(type, privateIm.getFieldData(type));
                    }
                } catch (Throwable t) {
                    // we should really check to see if the datastream existed at the 
                    // time we're requesting, but it's just as quick and easy to 
                    // try to get it and fail.
                }
            }
            return im; 
        } catch (IOException ex) {
            throw new RepositoryException(ex);
        } catch (DataFormatException ex) {
            throw new RepositoryException(ex);
        } catch (FedoraException ex) {
            throw new RepositoryException(ex);
        }
    }

    public synchronized List<VersionInformation> getItemMetadataHistory(String id) throws RepositoryException {
        List<VersionInformation> versions = new ArrayList<VersionInformation>();
        
        try {
            // determine the pid
            String pid = null;
            List<String> pids = fedora.dcIdentifierLookup(id);
            if (pids.size() != 1) {
                throw new RuntimeException(pids.size() + " items found with the \"unique\" identifier, \"" + id + "\"!");
            } else {
                pid = pids.get(0);
            }
            Set<Date> dates = new HashSet<Date>();
            for (DatastreamProfile profile : fedora.getDatastreamHistory(pid, metadataDatastreamId)) {
                dates.add(FedoraRestApiWrapper.parseFedoraDate(profile.getProperty(DatastreamProfile.DatastreamProperty.DS_CREATE_DATE)));
            }
            if (fedora.hasDatastream(pid, privateMetadataDsId)) {
                for (DatastreamProfile profile : fedora.getDatastreamHistory(pid, privateMetadataDsId)) {
                    dates.add(FedoraRestApiWrapper.parseFedoraDate(profile.getProperty(DatastreamProfile.DatastreamProperty.DS_CREATE_DATE)));
                }
            }
            ArrayList<Date> sortedDates = new ArrayList<Date>(dates);
            Collections.sort(sortedDates);
            for (int i = sortedDates.size() -1; i >= 0; i --) {
                Date date = sortedDates.get(i);
                VersionInformation version = new VersionInformation(id, date);
                versions.add(version);
            }
            return versions;
        } catch (IOException ex) {
            throw new RepositoryException(ex);
        } catch (FedoraException ex) {
            throw new RepositoryException(ex);
        } catch (XPathExpressionException ex) {
            throw new RepositoryException(ex);
        } catch (SAXException ex) {
            throw new RepositoryException(ex);
        } catch (ParserConfigurationException ex) {
            throw new RepositoryException(ex);
        } catch (ParseException ex) {
            throw new RepositoryException(ex);
        }
    }
    
    public boolean isReadOnly() {
        return fedora.isReadOnly();
    }

    public FileSubmitter getFileSubmitter(CollectionConfiguration c) {
        return new FIMFileSubmitter(c.getCollectionMetadata().getImageSubmissionProperties());
    }
    
    /**
     * TODO: this only expects tif image files to be submitted, perhaps
     * it should be more general.
     */
    private class FIMFileSubmitter implements FileSubmitter {

        /*
         * File submission that is attached to the existing
         * record can be achieved if 'dropboxDir' and 'ingestDir'
         * are set to valid directories that have been set up
         * with an external image processing and ingest workflow. 
         */
        private File dropboxDir;
        private File ingestDir;
        
        /*
         * File submission that creates new objects in fedora
         * require that that an id generator be set up for 
         * image-only objects.
         */
        private IdGenerator idGenerator;
        
        public FIMFileSubmitter(Properties p) {
            if (p != null) {
                if (p.containsKey("dropboxDir")) {
                    dropboxDir = new File(p.getProperty("dropboxDir"));
                }
                if (p.containsKey("ingestDir")) {
                    ingestDir = new File(p.getProperty("ingestDir"));
                }
            }
            try {
                idGenerator = new IdGenerator(p);
                if (idGenerator.getRequiredArguments() != null && !idGenerator.getRequiredArguments().isEmpty()) {
                    // the idGenerator requires arguments, so for simplicity this is an
                    // unsupported case.
                    idGenerator = null;
                }
            } catch (Throwable t) {
                // fall through, the idGenerator will be null.
            }
        }
        
        /**
         * Returns true if the configuration is sufficient to allow
         * submission of a single file.
         * @throws IllegalStateException if it's clear that image 
         * submission will fail due to IOExceptions.
         */
        public boolean isFileSubmissionAvailable(Item item) {
            if (dropboxDir == null || ingestDir == null) {
                return false;
            } else if (FileSubmissionStatus.Status.PENDING_SUBMISSION.equals(getFileSubmissionStatus(item).getStatusCode()) && (item.getAspects() == null || item.getAspects().isEmpty())) {
                return true;
            } else if (dropboxDir != null && ingestDir != null && idGenerator != null) {
                // we can always submit a new image, no matter how many other
                // images are submitted or what state they're current in because
                // each new image will be a new object in fedora and will have
                // a new unique id.
                return true;
            } else {
                Logger.getLogger(this.getClass()).debug("File submission is disabled for item " + item.getId() + ": " + getFileSubmissionStatus(item).getStatusCode() + ", dropboxDir=" + dropboxDir + ", ingestDir=" + ingestDir);
                return false;
            }
        }
        
        /**
         * Writes the file out to the dropbox directory for this collection
         * with the collection local id of the item where it will be processed
         * asynchronously by an external application.
         */
        public void submitFile(InputStream fileIs, Item item) throws IOException {
            File newFile = null;
            if (FileSubmissionStatus.Status.PENDING_SUBMISSION.equals(getFileSubmissionStatus(item).getStatusCode())) {
                newFile = new File(dropboxDir, item.getIdWithinCollection() + ".tif");
            } else if (idGenerator != null) {
                Map<String, String> map = Collections.emptyMap();
                String id = idGenerator.getId(map);
                try {
                    fedora.createNewEmptyFedoraObject(item, id, imageToRecordRelationship);
                    newFile = new File(dropboxDir, id.substring(id.lastIndexOf('/') + 1) + ".tif");
                } catch (FedoraException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                throw new IllegalStateException();
            }
            if (newFile.exists()) {
                throw new IllegalStateException("A file for this item has already been uploaded!");
            } else {
                writeStreamToFile(fileIs, newFile);
            }
        }
        
        /**
         * Returns true if the collection is properly configured with a dropbox directory
         * and ingest directory and the status for the given aspect is INGESTED.
         */
        public boolean isFileReplacementAvailable(Item item, String aspectId) {
            if (dropboxDir != null && ingestDir != null && FileSubmissionStatus.Status.INGESTED.equals(getFileSubmissionStatus(item, aspectId).getStatusCode())) {
                return true;
            } else {
                return false;
            }
        }
        
        /**
         * Writes the file out to the dropbox directory for this collection
         * with the collection local aspectId where it will be processed
         * asynchronously by an external application.
         */
        public void replaceFile(InputStream fileIs, Item item, String aspectId) throws IOException {
            if (!isFileReplacementAvailable(item, aspectId)) {
                throw new IllegalStateException("Upload is currently unavailable!");
            }
            File newFile = new File(dropboxDir, aspectId + ".tif");
            if (newFile.exists()) {
                throw new IllegalStateException("A file for this item has already been uploaded!");
            } else {
                writeStreamToFile(fileIs, newFile);
            }
        }
        
        /**
         * Returns true if file submission is available and the aspect in question
         * is in the INGESTED state.
         */
        public boolean isFileRemovalAvailable(Item item, String aspectId) {
            if (dropboxDir != null && ingestDir != null && FileSubmissionStatus.Status.INGESTED.equals(getFileSubmissionStatus(item, aspectId).getStatusCode())) {
                return true;
            } else {
                return false;
            }
        }
        
        /**
         * Much more complicated than the other operations, this method 
         * must undo what was done by the asyncronous ingest operation.
         * It locates the fedora object for the aspect, if that object is
         * the same as the item object, it removes the datastreams and 
         * image content models.  If it's a different object, it just 
         * removes the relationship that link the objects. 
         */
        public void removeFile(Item item, String aspectId) throws IOException {
            try {
                String pid = null;
                if (item.getId().equals(aspectId)) {
                    // Actually delete the images off the of the object
                    // that contains both images and metadata.
                    pid = fedora.getPidForPURL(item.getId());
                    for (String cmodelUri : fedora.listContentModelURIs(pid)) {
                        String[] dsIds = imageContentModelUriToDSIDMap.get(cmodelUri);
                        if (dsIds != null) {
                            fedora.purgeRelationship(pid, "info:fedora/" + pid, FedoraRestApiWrapper.HAS_MODEL, cmodelUri);
                            for (String dsId : dsIds) {
                                fedora.purgeDatastream(pid, dsId);
                            }
                        }
                    }
                } else {
                    for (Aspect a : item.getAspects()) {
                        if (a.getId().equals(aspectId)) {
                            String itemPid = fedora.getPidForPURL(item.getId());
                            pid = fedora.getPidForPURL(a.getId());
                            // remove the relationship that links the two
                            fedora.purgeRelationship(pid, "info:fedora/" + pid, imageToRecordRelationship, "info:fedora/" + itemPid);
                            Logger.getLogger(this.getClass()).info("Removed relationship between " + pid + " and " + itemPid + " to unlink image.");
                            break;
                        }
                    }
                }
                if (pid == null) {
                    throw new RuntimeException("Unable to find image item with id " + aspectId + "!");
                }
                
                
            } catch (FedoraException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        public FileSubmissionStatus getFileSubmissionStatus(Item item) {
            return getFileSubmissionStatus(item, item.getId());
        }

        public FileSubmissionStatus getFileSubmissionStatus(Item item, String aspectId) {
            String aspectShortId = aspectId.substring(aspectId.lastIndexOf('/') + 1);
            if (dropboxDir == null || ingestDir == null) {
                return new FileSubmissionStatus(FileSubmissionStatus.Status.SUBMISSION_NOT_CONFIGURED, null);
            } else {
                if (!dropboxDir.exists() && !dropboxDir.mkdirs()) {
                    throw new RuntimeException("Unable to locate or create dropbox directory (" + dropboxDir + ")!");
                }
                if (!ingestDir.exists() && !ingestDir.mkdirs()) {
                    throw new RuntimeException("Unable to locate or create ingest directory (" + ingestDir + ")!");
                }

                for (File file : dropboxDir.listFiles()) {
                    if (file.getName().startsWith(aspectShortId)) {
                        return new FileSubmissionStatus(FileSubmissionStatus.Status.PENDING_PROCESSING, new Date(file.lastModified()));
                        // TODO: add a check for validation error (wherever that might show up)
                    }
                }
                
                for (File file : ingestDir.listFiles()) {
                    if (file.getName().startsWith(aspectShortId)) {
                        return new FileSubmissionStatus(FileSubmissionStatus.Status.PENDING_INGEST, new Date(file.lastModified()));
                    }
                }
                
                for (Aspect aspect : item.getAspects()) {
                    if (aspect.getId().equals(aspectId) && !aspect.listDataViews().isEmpty()) {
                        // an aspect (containing data views) already exists for this 
                        // id, so we know that an image has been processed and ingested
                        // for the provided aspectId.
                        return new FileSubmissionStatus(FileSubmissionStatus.Status.INGESTED, null);
                    }
                }
                
                return new FileSubmissionStatus(FileSubmissionStatus.Status.PENDING_SUBMISSION, null);
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


}
