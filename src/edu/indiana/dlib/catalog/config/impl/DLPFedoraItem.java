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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import edu.indiana.dlib.catalog.config.DataFormatException;
import edu.indiana.dlib.catalog.config.DataView;
import edu.indiana.dlib.catalog.config.Item;
import edu.indiana.dlib.catalog.config.ItemMetadata;
import edu.indiana.dlib.catalog.config.NameValuePair;
import edu.indiana.dlib.catalog.config.impl.fedora.FedoraException;
import edu.indiana.dlib.catalog.config.impl.fedora.FedoraRestApiWrapper;
import edu.indiana.dlib.catalog.config.impl.fedora.DatastreamProfile;

/**
 * This is a FedoraItem implementation that is written around
 * the content models supported in Indiana University's
 * Digital Library Program's fedora instance.  Many assumptions
 * about the content models are encoded into this class.  While
 * it may not be useful for other implementations, the code 
 * can illustrate the ease with which fedora objects can be 
 * parsed into Item objects.
 */
public class DLPFedoraItem implements Item {
    
    private ItemMetadata metadata;

    private List<NameValuePair> controlFields;
    
    private List<DataView> dataViews;
    
    private DataView preview;
    
    public DLPFedoraItem(FedoraRestApiWrapper fedora, String id, String metadataDatastreamId) throws IOException, DataFormatException, FedoraException, XPathExpressionException, SAXException, ParserConfigurationException {
        // determine the pid
        String pid = null;
        List<String> pids = fedora.dcIdentifierLookup(id);
        if (pids.size() != 1) {
            throw new RuntimeException(pids.size() + " items found with the \"unique\" identifier, \"" + id + "\"!");
        } else {
            pid = pids.get(0);
        }
        
        // parse the metadata
        metadata = new DefaultItemMetadata(fedora.getDatastream(pid, metadataDatastreamId));
        if (!metadata.getId().equals(id)) {
            throw new RuntimeException("The item identified by \"" + id + "\" improperly claims to be \"" + metadata.getId() + "\"! (pid=" + pid + ")");
        }
        String metadataModificationDateStr = fedora.getDatastreamProperty(pid, metadataDatastreamId, DatastreamProfile.DatastreamProperty.DS_CREATE_DATE);
        
        // determine the data views
        // TODO: a more robust implementation would not assume datastream names
        // or content types... this quick and dirty implementation only works
        // for DLP images.
        this.dataViews = new ArrayList<DataView>();
        List<String> pidsThatCouldBeViews = new ArrayList<String>();
        pidsThatCouldBeViews.add(pid);
        pidsThatCouldBeViews.addAll(fedora.getRelatedPids(pid, "info:fedora/fedora-system:def/relations-external#hasMetadata"));
        for (String viewPid : pidsThatCouldBeViews) {
            List<String> dsIds = fedora.listDatastreams(viewPid);
            boolean blocked = dsIds.contains("POLICY");
            for (String dsId : dsIds) {
                URL url = new URL((blocked ? getProxyUrl(fedora, viewPid, dsId) : fedora.getServerUrl() + "/get/" + pid + "/" + dsId));
                if (dsId.equals("THUMBNAIL")) {
                    DataView thumbView = new DefaultDataView(url, "image/jpeg", "thumbnail", false);
                    this.dataViews.add(thumbView);
                    this.preview = thumbView;
                } else if (dsId.equals("SCREEN")) {
                    this.dataViews.add(new DefaultDataView(url, "image/jpeg", "screen size image", false));
                } else if (dsId.equals("LARGE")) {
                    this.dataViews.add(new DefaultDataView(url, "image/jpeg", "large size image", false));
                } else if (dsId.equals("MASTER")) {
                    this.dataViews.add(new DefaultDataView(url, null, "master image", false));
                } else if (dsId.equals("IMAGE")) {
                    DataView preview = new DefaultDataView(url, "image/jpeg", "submitted image", false);
                    dataViews.add(preview);
                    this.preview = preview;
                }
            }
        }
        
        this.controlFields = new ArrayList<NameValuePair>(2);
        this.controlFields.add(new NameValuePair("pid", pid));
        this.controlFields.add(new NameValuePair("lastModified", metadataModificationDateStr));
    }
    
    private String getProxyUrl(FedoraRestApiWrapper fedora, String pid, String dsId) {
        return fedora.getServerUrl().replaceAll("/fedora$", "/iudl-dissem/casproxy?pid="  + pid + "&dsname=" + dsId);
    }
    
    public static String getPid(List<NameValuePair> controlFields) {
        for (NameValuePair value : controlFields) {
            if (value.getName().equals("pid")) {
                return value.getValue();
            }
        }
        return null;
    }
    
    public static String getLastModificationDateStr(List<NameValuePair> controlFields) {
        for (NameValuePair value : controlFields) {
            if (value.getName().equals("lastModified")) {
                return value.getValue();
            }
        }
        return null;
    }

    public String getCollectionId() {
        return this.metadata.getCollectionId();
    }

    public String getId() {
        return this.metadata.getId();
    }

    public String getIdWithinCollection() {
        return this.getId().substring(this.getId().lastIndexOf('/') + 1);
    }

    public ItemMetadata getMetadata() {
        return this.metadata;
    }

    public DataView getPreview() {
        return this.preview;
    }

    public Collection<DataView> listDataViews() {
        return this.dataViews;
    }
    
    public List<NameValuePair> getControlFields() {
        return this.controlFields;
    }
    
}
