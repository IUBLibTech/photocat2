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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import edu.indiana.dlib.catalog.accesscontrol.UserInfo;
import edu.indiana.dlib.catalog.config.CollectionConfiguration;
import edu.indiana.dlib.catalog.config.DataFormatException;
import edu.indiana.dlib.catalog.config.HistoryEnabledItemManager;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemManager;
import edu.indiana.dlib.catalog.config.ItemMetadata;
import edu.indiana.dlib.catalog.config.OptimisticLockingException;
import edu.indiana.dlib.catalog.config.RepositoryException;
import edu.indiana.dlib.catalog.config.impl.fedora.DatastreamProfile;
import edu.indiana.dlib.catalog.config.impl.fedora.FedoraException;
import edu.indiana.dlib.catalog.config.impl.fedora.FedoraRestApiWrapper;

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
    
    private String itemContentModel;
    
    private String metadataDatastreamId;
    
    public FedoraItemManager(String username, String password, String host, String contextName, Integer port, String cmodelPid, String dsId) {
        fedora = new FedoraRestApiWrapper(username, password, host, contextName, port, false);
        metadataDatastreamId = dsId;
        itemContentModel = cmodelPid;
    }
    
    public synchronized Item fetchItem(String id) throws RepositoryException {
        try {
            return new DLPFedoraItem(fedora, id, metadataDatastreamId);
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
    
    public synchronized void saveItemMetadata(Item updatedItem, UserInfo user) throws OptimisticLockingException, RepositoryException {
        // validate control fields
        String pid = DLPFedoraItem.getPid(updatedItem.getControlFields());
        if (pid != null) {
            // verify that the item exists and matches the pid
            DLPFedoraItem remoteItem = (DLPFedoraItem) this.fetchItem(updatedItem.getId());
            if (remoteItem == null) {
                throw new OptimisticLockingException("The item with id, \"" + updatedItem.getId() + "\" no longer exists in the repository!");
            }
            if (!pid.equals(DLPFedoraItem.getPid(remoteItem.getControlFields()))) {
                throw new RepositoryException("The repository item with id, \"" + updatedItem.getId() + "\" does not have the same PID " + pid + " as when it was originally fetched!");
            }
            
            String lastModified = DLPFedoraItem.getLastModificationDateStr(updatedItem.getControlFields());
            if (!DLPFedoraItem.getLastModificationDateStr(remoteItem.getControlFields()).equals(lastModified)) {
                throw new OptimisticLockingException("The item with id, \"" + updatedItem.getId() + "\" has been modified since you last viewed it.");
            }
            
            // store the XML
            try {
                fedora.addOrReplaceDatastreamWithDocument(pid, metadataDatastreamId, FedoraRestApiWrapper.ControlGroup.M, null, "text/xml", updatedItem.getMetadata().generateDocument(), metadataDatastreamId + ".xml");
            } catch (FedoraException ex) {
                throw new RepositoryException(ex);
            } catch (ParserConfigurationException ex) {
                throw new RepositoryException(ex);
            }
            
        } else {
            // validate that no item exists with the given id
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Currently unsupported. 
     * @throws UnsupportedOperationException whenever called
     */
    public synchronized String createNewItem(CollectionConfiguration config, UserInfo user) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Currently unsupported. 
     * @throws UnsupportedOperationException whenever called
     */
    public synchronized boolean removeItem(Item item, CollectionConfiguration config, UserInfo user) throws RepositoryException {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Unimplemented.
     */
    public synchronized String getNextId(String prefix) throws FedoraException, IOException {
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
            
            return new DefaultItemMetadata(fedora.getDatastream(pid, metadataDatastreamId, (date == null ? null : FedoraRestApiWrapper.printFedoraDateString(date))));
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
            for (DatastreamProfile profile : fedora.getDatastreamHistory(pid, this.metadataDatastreamId)) {
                Date date = FedoraRestApiWrapper.parseFedoraDate(profile.getProperty(DatastreamProfile.DatastreamProperty.DS_CREATE_DATE));
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

}
