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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

/**
 * Metadata about the collection as a whole.  The current implementation
 * as well as the schema file on which it's based is tentative.
 */
public class CollectionMetadata {
    
    private String name;
    
    private String collectionId;
    
    private String description;
    
    private String iconUrl;
    
    private String bannerUrl;
    
    private boolean allowRecordCreation;
    
    private boolean allowRecordDeletion;
    
    /**
     * Constructs a CollectionMetadata object that represents
     * the metadata parsed from a collection element that
     * has been validated against the configuration schema.
     */
    public CollectionMetadata(Element el) throws DataFormatException {
        XPath xpath = XPathHelper.getInstance().getXPath();
        
        try {
            // Parse out the name
            XPathExpression nameXpath = xpath.compile("c:name");
            if ((Boolean) nameXpath.evaluate(el, XPathConstants.BOOLEAN)) {
                this.name = (String) nameXpath.evaluate(el, XPathConstants.STRING);
            } else {
                throw new DataFormatException("Required field \"name\" was not found!");
            }
            
            // Parse out the collectionId
            XPathExpression idXpath = xpath.compile("c:id");
            if ((Boolean) idXpath.evaluate(el, XPathConstants.BOOLEAN)) {
                this.collectionId = (String) idXpath.evaluate(el, XPathConstants.STRING);
            } else {
                throw new DataFormatException("Required field \"id\" was not found!");
            }
            
            // Parse out the description
            XPathExpression descriptionXpath = xpath.compile("c:description");
            if ((Boolean) descriptionXpath.evaluate(el, XPathConstants.BOOLEAN)) {
                this.description = (String) descriptionXpath.evaluate(el, XPathConstants.STRING);
            } else {
                throw new DataFormatException("Required field \"description\" was not found!");
            }
            
            // Parse out the iconURL
            XPathExpression iconUrlXpath = xpath.compile("c:iconURL");
            if ((Boolean) iconUrlXpath.evaluate(el, XPathConstants.BOOLEAN)) {
                this.iconUrl = (String) iconUrlXpath.evaluate(el, XPathConstants.STRING);
            } else {
                throw new DataFormatException("Required field \"iconURL\" was not found!");
            }
            
            // Parse out the bannerURL
            XPathExpression bannerUrlXpath = xpath.compile("c:bannerURL");
            if ((Boolean) bannerUrlXpath.evaluate(el, XPathConstants.BOOLEAN)) {
                this.bannerUrl = (String) bannerUrlXpath.evaluate(el, XPathConstants.STRING);
            } else {
                throw new DataFormatException("Required field \"bannerUrl\" was not found!");
            }
            
            this.allowRecordCreation = Boolean.parseBoolean(getRequiredElement("allowRecordCreation", el, xpath));
            this.allowRecordDeletion = Boolean.parseBoolean(getRequiredElement("allowRecordDeletion", el, xpath));
            

        } catch (XPathException ex) {
            // Shouldn't happen because the xpath isn't dynamicaly generated
            throw new RuntimeException(ex);
        }
    }
    
    private String getRequiredElement(String paramName, Element el, XPath xpath) throws DataFormatException, XPathExpressionException {
        XPathExpression elXpath = xpath.compile("c:" + paramName);
        if ((Boolean) elXpath.evaluate(el, XPathConstants.BOOLEAN)) {
            return (String) elXpath.evaluate(el, XPathConstants.STRING);
        } else {
            throw new DataFormatException("Required field \"" + paramName + "\" was not found!");
        }
    }
    
    public String getId() {
        return this.collectionId;
    }
    
    /**
     * Returns a unit identifier if one is present.  Currently this
     * isn't stored in the metadata but is gleaned from the collectionID.
     * 
     * TODO: This must be changed to support more robust unit configurations.
     */
    public String getUnitId() {
        if (this.collectionId.contains("/")) {
            return this.collectionId.substring(0, this.collectionId.lastIndexOf('/'));
        } else {
            return null;
        }
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public String getIconUrl() {
        return this.iconUrl;
    }
    
    public String getBannerUrl() {
        return this.bannerUrl;
    }
    
    public boolean allowRecordCreation() {
        return this.allowRecordCreation;
    }
    
    public boolean allowRecordDeletion() {
        return this.allowRecordDeletion;
    }
}
