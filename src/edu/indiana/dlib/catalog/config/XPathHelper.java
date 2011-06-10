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

import java.util.Collections;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
/**
 * A simple NamespaceContext implementation that includes
 * the relevant namespaces and schema locations with their
 * simple prefixes.
 *
 * The namespace "info:photocat/definition" is prefixed with "d".
 * The namespace "info:photocat/configuration" is prefixed with "c".
 * The namespace "info:photocat/metadata" is prefixed with "m".
 * The namespace "http://www.w3.org/2001/XMLSchema-instance" is prefixed with "xsi"
 */
public class XPathHelper {

    private static XPathHelper INSTANCE;
    
    /**
     * The namespace URI for the definition schema.
     */
    public static final String D_URI = "info:photocat/definition";

    /**
     * The schema location for the definition schema.
     */
    public static final String D_XSD_LOC = "http://purl.dlib.indiana.edu/iudl/xml/schema/photocat/field-definition.xsd";
    
    /**
     * The namespace URI for the configuration schema.
     */
    public static final String C_URI = "info:photocat/configuration"; 

    /**
     * The schema location for the configuration schema.
     */
    public static final String C_XSD_LOC = "http://purl.dlib.indiana.edu/iudl/xml/schema/photocat/field-configuration.xsd";
    
    /**
     * The namespace URI for the metadata schema.
     */
    public static final String M_URI = "info:photocat/metadata";
    
    /**
     * The schema location for the metadata schema.
     */
    public static final String M_XSD_LOC = "http://purl.dlib.indiana.edu/iudl/xml/schema/photocat/field-metadata.xsd";

    /**
     * The namespace URI for the schema instance schema.
     */
    public static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * Gets the instance of this XPathHelper.
     */
    public static XPathHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new XPathHelper();
        }
        return INSTANCE;
    }

    /**
     * An instantiated and configured XPath instance. 
     */
    private XPath xpath;

    /**
     * A NamespaceContext with all the relevant namespaces configured.
     */
    private NamespaceContext nsc;

    /**
     * A constructor for an XPathHelper.  This class is meant to have
     * a single instance per application, so the constructor is not
     * public.
     */
    private XPathHelper() {
        this.nsc = new EmbeddedNamespaceContext();
        this.xpath = XPathFactory.newInstance().newXPath();
        this.xpath.setNamespaceContext(this.nsc);
    }
    
    public XPath getXPath() {
        return this.xpath;
    }
    
    private static class EmbeddedNamespaceContext implements NamespaceContext {
        
        /**
         * Accepts "d", "c" and "m" and returns the appropriate
         * namespace URIs.
         */
        public String getNamespaceURI(String prefix) {
            if (prefix.equals("m")) {
                return M_URI;
            } else if (prefix.equals("c")) {
                return C_URI;
            } else if (prefix.equals("d")) {
                return D_URI;
            } else if (prefix.equals("xsi")) {
                return XSI_URI;
            } else {
                return null;
            }
        }
    
        public String getPrefix(String namespaceURI) {
            if (namespaceURI.equals(M_URI)) {
                return "m";
            } else if (namespaceURI.equals(C_URI)) {
                return "c";
            } else if (namespaceURI.equals(D_URI)) {
                return "d";
            } else if (namespaceURI.equals(XSI_URI)) {
                return "xsi";
            } else {
                return null;
            }
        }
    
        public Iterator getPrefixes(String namespaceURI) {
            return Collections.singletonList(this.getPrefix(namespaceURI)).iterator();
        }
    }

}
