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
package edu.indiana.dlib.catalog.config.impl.fedora;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Element;

public class DatastreamProfile {

    /**
     * An enumeration that represents all of the properties associated
     * with a datastream in fedora with internal information about how
     * those properties are referenced.
     */
    public static enum DatastreamProperty {
        DS_LABEL("dsLabel"),
        DS_VERSION_ID("dsVersionID"),
        DS_CREATE_DATE("dsCreateDate"),
        DS_STATE("dsState"),
        DS_MIME("dsMIME"),
        DS_FORMAT_URI("dsFormatURI"),
        DS_CONTROL_GROUP("dsControlGroup"),
        DS_SIZE("dsSize"),
        DS_VERSIONABLE("dsVersionable"),
        DS_INFO_TYPE("dsInfoType"),
        DS_LOCATION("dsLocation"),
        DS_LOCATION_TYPE("dsLocationType"),
        DS_CHECKSUM_TYPE("dsChecksumType"),
        DS_CHECKSUM("dsChecksum");
        
        private String propertyName;
        
        DatastreamProperty(String name) {
            this.propertyName = name;
        }
        
        public String getPropertyName() {
            return this.propertyName;
        }
    }
    
    private FedoraRestApiWrapper fedora;
    
    private Element profileEl;
    
    public DatastreamProfile(FedoraRestApiWrapper fedora, Element profileEl) {
        this.fedora = fedora;
        this.profileEl = profileEl;
    }
    
    public String getProperty(DatastreamProperty property) throws FedoraException {
        try {
            // compatible with fedora 3.4
            String value = (String) fedora.getXPath().evaluate("fedora-management:" + property.getPropertyName(), this.profileEl, XPathConstants.STRING);
            if (value != null) {
                return value;
            } else {
                // compatible with fedora 3.2
                return (String) fedora.getXPath().evaluate("/datastreamProfile/" + property.getPropertyName(), this.profileEl, XPathConstants.STRING);
            }
        } catch (XPathException ex) {
            throw new FedoraException(ex);
        }
    }
    
}
