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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;


/**
 * <p>
 *   An object that encapsulates a fedora object's profile information.
 *   When only a single property is needed for an object, the method
 *   {@link FedoraClient.getObjectProperty()} should be invoked, but
 *   if multiple properties are needed, the method {@link 
 *   FedoraClient.getObjectProfile()} should be used to get an instance
 *   of this Object to reduce the number of HTTP roundtrips.
 * </p>
 * <p>
 *   This object is a very thin wrapper around the XML returned by the
 *   REST api call for an object profile.
 * </p>
 */
public class ObjectProfile {

    /**
     * The object properties available in Fedora 3.4.
     */
    private enum ObjectProperty {
        LABEL,
        OWNER_ID,
        MODELS,
        CREATE_DATE,
        LAST_MOD_DATE,
        DISS_INDEX_VIEW_URL,
        ITEM_INDEX_VIEW_URL,
        STATE;
    }
    
    /**
     * The Document parsed from the REST call.
     */
    private Document objectProfileDoc;
    
    /**
     * A reference to an XPath sufficient for use extracting field
     * values.
     */
    private XPath xpath;
    
    ObjectProfile(Document objectProfileDoc, XPath xpath) {
        this.objectProfileDoc = objectProfileDoc;
        MapNamespaceContext nsc = new MapNamespaceContext();
        nsc.setNamespace("fa", "http://www.fedora.info/definitions/1/0/access/");
        this.xpath = XPathFactory.newInstance().newXPath();
        this.xpath.setNamespaceContext(nsc);
        
        
    }
    
    public String getLabel() {
        try {
            String label = (String) this.xpath.evaluate("/fa:objectProfile/fa:objLabel" , this.objectProfileDoc, XPathConstants.STRING);
            if (label == null || label.equals("")) {
                label =  (String) this.xpath.evaluate("/objectProfile/objLabel/", this.objectProfileDoc, XPathConstants.STRING);
            }
            return label;
        } catch (XPathExpressionException ex) {
            return null;
        }
    }
    
    public String getOwnerId() {
        try {
            String ownerId = (String) this.xpath.evaluate("/fa:objectProfile/fa:objOwnerId" , this.objectProfileDoc, XPathConstants.STRING);
            if (ownerId == null || ownerId.equals("")) {
                ownerId = (String) this.xpath.evaluate("/objectProfile/objOwnerId" , this.objectProfileDoc, XPathConstants.STRING);
            }
            return ownerId;
        } catch (XPathExpressionException ex) {
            return null;
        }
    }
    
    public String getCreateDate() {
        try {
            String createDate = (String) this.xpath.evaluate("/fa:objectProfile/fa:objCreateDate" , this.objectProfileDoc, XPathConstants.STRING);
            if (createDate == null || createDate.equals("")) {
                createDate = (String) this.xpath.evaluate("/objectProfile/objCreateDate" , this.objectProfileDoc, XPathConstants.STRING);
            }
            return createDate;
        } catch (XPathExpressionException ex) {
            return null;
        }
    }
    
    public String getLastModDate() {
        try {
            String lastModDate = (String) this.xpath.evaluate("/fa:objectProfile/fa:objLastModDate" , this.objectProfileDoc, XPathConstants.STRING);
            if (lastModDate == null || lastModDate.equals("")) {
                lastModDate = (String) this.xpath.evaluate("/objectProfile/objLastModDate", this.objectProfileDoc, XPathConstants.STRING);
            }
            return lastModDate;
        } catch (XPathExpressionException ex) {
            return null;
        }
    }
    
    public String getState() {
        try {
            String state = (String) this.xpath.evaluate("/fa:objectProfile/fa:objState" , this.objectProfileDoc, XPathConstants.STRING);
            if (state == null || state.equals("")) {
                state = (String) this.xpath.evaluate("/objectProfile/objState" , this.objectProfileDoc, XPathConstants.STRING);
            }
            return state;
        } catch (XPathExpressionException ex) {
            return null;
        }
    }
}
